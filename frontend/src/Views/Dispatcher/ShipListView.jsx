import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  getAllShips,
  receiveShip,
  departShip,
  deleteShip,
  getActiveRoute,
  generateRoute,
  getAllPorts,
  getShipStowages,
  buildErrorMessage,
} from "./shipApi";

const STATE_LABELS = {
  DISPATCHER: "At dispatcher",
  ARRIVED: "Received",
  DEPARTED: "Dispatched",
  AWAITING_DOCKING: "Awaiting arrival",
};
const STATE_CLASS = {
  DISPATCHER: "badge-neutral",
  ARRIVED: "badge-success",
  DEPARTED: "badge-warning",
  AWAITING_DOCKING: "badge-info",
};
const ROUTE_STATE_LABELS = { ACTIVE: "Active", FINISHED: "Finished" };
const ROUTE_STATE_CLASS  = { ACTIVE: "badge-info", FINISHED: "badge-success" };
const SEG_STATE_LABELS   = { UNVISITED: "Not started", ONGOING: "In progress", VISITED: "Visited" };
const STOWAGE_STATUS_LABELS = {
  LAUKIA_PAKROVIMO: "Awaiting loading",
  PAKRAUTAS: "Loaded",
  LAUKIA_ISKROVIMO: "Awaiting unloading",
  ISKRAUTAS: "Unloaded",
};
const STOWAGE_STATUS_CLASS = {
  LAUKIA_PAKROVIMO: "badge-warning",
  PAKRAUTAS: "badge-success",
  LAUKIA_ISKROVIMO: "badge-info",
  ISKRAUTAS: "badge-neutral",
};

export default function ShipListView() {
  const location = useLocation();
  const [ships, setShips] = useState([]);
  const [activeRoutes, setActiveRoutes] = useState({});
  const [shipStowages, setShipStowages] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");
  const [busyId, setBusyId] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(null);

  // Generate route modal
  const [generateModal, setGenerateModal] = useState(null);
  const [ports, setPorts] = useState([]);
  const [selectedPorts, setSelectedPorts] = useState([]);
  const [routeName, setRouteName] = useState("");
  const [generateBusy, setGenerateBusy] = useState(false);
  const [generateError, setGenerateError] = useState("");

  useEffect(() => { load(); }, []);

  async function load() {
    setIsLoading(true);
    setError("");
    try {
      const shipList = await getAllShips();
      setShips(shipList);

      const [routeResults, stowageResults] = await Promise.all([
        Promise.allSettled(shipList.map(s => getActiveRoute(s.id))),
        Promise.allSettled(shipList.map(s => getShipStowages(s.id))),
      ]);

      const routeMap = {};
      routeResults.forEach((result, idx) => {
        if (result.status === "fulfilled" && result.value)
          routeMap[shipList[idx].id] = result.value;
      });
      setActiveRoutes(routeMap);

      const stowageMap = {};
      stowageResults.forEach((result, idx) => {
        if (result.status === "fulfilled" && result.value?.length > 0)
          stowageMap[shipList[idx].id] = result.value[result.value.length - 1];
      });
      setShipStowages(stowageMap);
    } catch (e) {
      setError(buildErrorMessage(e, "Failed to load ships."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleReceive(id) {
    setBusyId(id);
    setActionError(""); setActionSuccess("");
    try {
      const resp = await receiveShip(id);
      setShips(prev => prev.map(s => s.id === id ? resp.ship : s));
      try {
        const route = await getActiveRoute(id);
        setActiveRoutes(prev => ({ ...prev, [id]: route }));
      } catch {
        setActiveRoutes(prev => { const n = { ...prev }; delete n[id]; return n; });
      }
      if (resp.hasActiveRoute) {
        if (resp.sufficientFuel) {
          setActionSuccess(
            `Ship received. Fuel updated: ${resp.currentFuelAmount?.toFixed(0)} l. ` +
            `Next segment requires ${resp.nextSegmentFuelRequired?.toFixed(0)} l — sufficient. ` +
            `Route recalculated.`
          );
        } else {
          setActionError(
            `Ship received, but insufficient fuel for the next segment! ` +
            `Available: ${resp.currentFuelAmount?.toFixed(0)} l, ` +
            `required: ${resp.nextSegmentFuelRequired?.toFixed(0)} l. ` +
            `Refuel before dispatching.`
          );
        }
      } else {
        setActionSuccess(`Ship received. Route completed. Fuel: ${resp.currentFuelAmount?.toFixed(0)} l.`);
      }
    } catch (e) {
      setActionError(buildErrorMessage(e, "Receive failed."));
    } finally { setBusyId(null); }
  }

  async function handleDepart(id) {
    setBusyId(id);
    setActionError(""); setActionSuccess("");
    try {
      const updated = await departShip(id);
      setShips(prev => prev.map(s => s.id === id ? updated : s));
      try {
        const route = await getActiveRoute(id);
        setActiveRoutes(prev => ({ ...prev, [id]: route }));
      } catch {
        setActiveRoutes(prev => { const n = { ...prev }; delete n[id]; return n; });
      }
      setActionSuccess(`Ship dispatched.`);
    } catch (e) {
      setActionError(buildErrorMessage(e, "Dispatch failed."));
    } finally { setBusyId(null); }
  }

  async function handleDelete(id) {
    setBusyId(id);
    setActionError("");
    try {
      await deleteShip(id);
      setShips(prev => prev.filter(s => s.id !== id));
      setActiveRoutes(prev => { const n = { ...prev }; delete n[id]; return n; });
      setShipStowages(prev => { const n = { ...prev }; delete n[id]; return n; });
      setConfirmDelete(null);
    } catch (e) {
      setActionError(buildErrorMessage(e, "Delete failed."));
    } finally { setBusyId(null); }
  }

  async function openGenerateModal(ship) {
    setGenerateModal(ship);
    setSelectedPorts([]);
    setRouteName("");
    setGenerateError("");
    if (ports.length === 0) {
      try {
        setPorts(await getAllPorts());
      } catch {
        setGenerateError("Failed to load ports.");
      }
    }
  }

  function togglePort(portId) {
    setSelectedPorts(prev =>
      prev.includes(portId) ? prev.filter(p => p !== portId) : [...prev, portId]
    );
  }

  async function handleGenerateRoute() {
    if (!selectedPorts.length) { setGenerateError("Select at least one destination port."); return; }
    setGenerateBusy(true); setGenerateError("");
    try {
      await generateRoute(generateModal.id, {
        destinationPortIds: selectedPorts,
        routeName: routeName.trim() || undefined,
      });
      try {
        setActiveRoutes(prev => ({
          ...prev,
          [generateModal.id]: null, // will reload below
        }));
        const detail = await getActiveRoute(generateModal.id);
        setActiveRoutes(prev => ({ ...prev, [generateModal.id]: detail }));
      } catch {}
      setGenerateModal(null);
      setActionSuccess("Route generated successfully.");
    } catch (e) {
      setGenerateError(buildErrorMessage(e, "Route generation failed."));
    } finally { setGenerateBusy(false); }
  }

  function getRouteInfo(routeDetail) {
    if (!routeDetail) return null;
    const { route, segments } = routeDetail;
    const seg = segments?.find(s => s.state === "ONGOING") ?? segments?.find(s => s.state === "UNVISITED");
    if (!seg) return { routeState: route?.state, routeId: route?.id, routeName: route?.name, segmentLabel: "All segments visited" };
    return {
      routeState: route?.state,
      routeId: route?.id,
      routeName: route?.name,
      segmentLabel: `${seg.startPort?.name ?? "?"} → ${seg.destinationPort?.name ?? "?"}`,
      segmentState: seg.state,
    };
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Ships</h2>
          <p className="muted">Manage ships, routes and dispatching.</p>
        </div>
        <Link className="button button-primary" to="/ships/new">+ Register ship</Link>
      </div>

      {location.state?.message && <div className="alert alert-success">{location.state.message}</div>}
      {error && <div className="alert alert-error">{error}</div>}
      {actionSuccess && <div className="alert alert-success">{actionSuccess}</div>}
      {actionError && <div className="alert alert-error">{actionError}</div>}

      <div className="card table-card">
        {isLoading ? (
          <p style={{ padding: 20 }}>Loading...</p>
        ) : ships.length === 0 ? (
          <p style={{ padding: 20 }}>No ships found.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Country</th>
                  <th>Capacity</th>
                  <th>Fuel (l)</th>
                  <th>Port</th>
                  <th>Status</th>
                  <th>Stowage plan</th>
                  <th>Active route</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {ships.map(ship => {
                  const routeInfo = getRouteInfo(activeRoutes[ship.id]);
                  const hasActiveRoute = !!activeRoutes[ship.id];
                  const stowage = shipStowages[ship.id];
                  return (
                    <tr key={ship.id}>
                      <td>
                        <Link to={`/ships/${ship.id}`} className="link">{ship.name}</Link>
                      </td>
                      <td>{ship.type || "—"}</td>
                      <td>{ship.country || "—"}</td>
                      <td>{ship.capacity}</td>
                      <td>{ship.fuelAmount?.toFixed(0) ?? "—"}</td>
                      <td>{ship.port?.name ?? "—"}</td>
                      <td>
                        <span className={`badge ${STATE_CLASS[ship.state] || "badge-neutral"}`}>
                          {STATE_LABELS[ship.state] || ship.state}
                        </span>
                      </td>
                      <td>
                        {stowage ? (
                          <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                            <span style={{ fontSize: 13, color: "#3b5a7a" }}>
                              {stowage.stowageType} · {stowage.data}
                            </span>
                            <span className={`badge ${STOWAGE_STATUS_CLASS[stowage.stowageStatus] || "badge-neutral"}`}
                                  style={{ width: "fit-content" }}>
                              {STOWAGE_STATUS_LABELS[stowage.stowageStatus] || stowage.stowageStatus}
                            </span>
                          </div>
                        ) : (
                          <span className="muted" style={{ fontSize: 13 }}>—</span>
                        )}
                      </td>
                      <td>
                        {routeInfo ? (
                          <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                            <span className={`badge ${ROUTE_STATE_CLASS[routeInfo.routeState] || "badge-neutral"}`}
                                  style={{ width: "fit-content" }}>
                              {ROUTE_STATE_LABELS[routeInfo.routeState] || routeInfo.routeState}
                            </span>
                            <span style={{ fontSize: 13, color: "#3b5a7a" }}>
                              {routeInfo.segmentLabel}
                              {routeInfo.segmentState && (
                                <span style={{ marginLeft: 6, color: "#627b92" }}>
                                  ({SEG_STATE_LABELS[routeInfo.segmentState] || routeInfo.segmentState})
                                </span>
                              )}
                            </span>
                            {routeInfo.routeId && (
                              <Link to={`/routes/${routeInfo.routeId}`} className="link" style={{ fontSize: 12 }}>
                                View →
                              </Link>
                            )}
                          </div>
                        ) : (
                          <span className="muted" style={{ fontSize: 13 }}>None</span>
                        )}
                      </td>
                      <td>
                        <div className="action-row">
                          <Link className="button button-small" to={`/ships/${ship.id}`}>View</Link>
                          <Link className="button button-small" to={`/ships/${ship.id}/edit`}>Edit</Link>
                          {ship.state === "ARRIVED" && !hasActiveRoute && (
                            <button className="button button-small button-primary"
                                    disabled={busyId === ship.id}
                                    onClick={() => openGenerateModal(ship)}>
                              Generate route
                            </button>
                          )}
                          {ship.state === "AWAITING_DOCKING" && (
                            <button className="button button-small button-primary"
                                    disabled={busyId === ship.id}
                                    onClick={() => handleReceive(ship.id)}>
                              {busyId === ship.id ? "..." : "Receive"}
                            </button>
                          )}
                          {ship.state === "ARRIVED" && hasActiveRoute && (
                            <button className="button button-small button-primary"
                                    disabled={busyId === ship.id}
                                    onClick={() => handleDepart(ship.id)}>
                              {busyId === ship.id ? "..." : "Dispatch"}
                            </button>
                          )}
                          {ship.state === "ARRIVED" && (
                            <button className="button button-small button-danger"
                                    disabled={busyId === ship.id}
                                    onClick={() => setConfirmDelete(ship)}>
                              Delete
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Delete confirmation modal */}
      {confirmDelete && (
        <div className="modal-backdrop">
          <div className="modal card">
            <h2 style={{ marginBottom: 12 }}>Confirm deletion</h2>
            <p>
              Are you sure you want to delete <strong>{confirmDelete.name}</strong>?
              All associated routes and coordinates will also be removed.
            </p>
            {actionError && (
              <div className="alert alert-error" style={{ marginTop: 12 }}>{actionError}</div>
            )}
            <div className="modal-actions" style={{ marginTop: 20 }}>
              <button className="button button-danger"
                      disabled={busyId === confirmDelete.id}
                      onClick={() => handleDelete(confirmDelete.id)}>
                {busyId === confirmDelete.id ? "Deleting..." : "Yes, delete"}
              </button>
              <button className="button" onClick={() => setConfirmDelete(null)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      {/* Generate route modal */}
      {generateModal && (
        <div className="modal-backdrop">
          <div className="modal card" style={{ maxWidth: 480 }}>
            <h2 style={{ marginBottom: 4 }}>Generate route</h2>
            <p className="muted" style={{ marginBottom: 16 }}>
              Ship: <strong>{generateModal.name}</strong> &mdash; port:{" "}
              <strong>{generateModal.port?.name ?? "Unknown"}</strong>
            </p>

            {generateError && (
              <div className="alert alert-error" style={{ marginBottom: 12 }}>{generateError}</div>
            )}

            <div style={{ marginBottom: 16 }}>
              <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
                Route name <span style={{ color: "#627b92", fontWeight: 400 }}>(optional)</span>
              </label>
              <input className="input" type="text" placeholder="Auto-generated if blank"
                     value={routeName} onChange={e => setRouteName(e.target.value)}
                     style={{ width: "100%", boxSizing: "border-box" }} />
            </div>

            <div style={{ marginBottom: 20 }}>
              <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
                Destination ports <span style={{ color: "#e05" }}>*</span>
              </label>
              <p style={{ fontSize: 13, color: "#627b92", marginBottom: 8 }}>
                Select ports in visit order.
              </p>
              <div style={{ display: "flex", flexDirection: "column", gap: 6, maxHeight: 220, overflowY: "auto",
                            border: "1px solid #dde3ea", borderRadius: 8, padding: 8 }}>
                {ports.filter(p => p.id !== generateModal.port?.id).map(port => (
                  <label key={port.id} style={{
                    display: "flex", alignItems: "center", gap: 10, padding: "8px 10px",
                    borderRadius: 6, cursor: "pointer", userSelect: "none", fontSize: 14,
                    border: selectedPorts.includes(port.id) ? "1.5px solid #2563eb" : "1.5px solid transparent",
                    background: selectedPorts.includes(port.id) ? "#eff6ff" : "transparent",
                  }}>
                    <input type="checkbox" checked={selectedPorts.includes(port.id)}
                           onChange={() => togglePort(port.id)}
                           style={{ accentColor: "#2563eb", width: 16, height: 16 }} />
                    <span>{port.name}</span>
                    {selectedPorts.includes(port.id) && (
                      <span style={{ marginLeft: "auto", fontSize: 12, color: "#fff", background: "#2563eb",
                                     borderRadius: "50%", width: 20, height: 20, display: "flex",
                                     alignItems: "center", justifyContent: "center", fontWeight: 700, flexShrink: 0 }}>
                        {selectedPorts.indexOf(port.id) + 1}
                      </span>
                    )}
                  </label>
                ))}
                {ports.filter(p => p.id !== generateModal.port?.id).length === 0 && (
                  <p style={{ color: "#627b92", fontSize: 13, padding: 8 }}>No other ports available.</p>
                )}
              </div>
            </div>

            <div className="modal-actions">
              <button className="button button-primary"
                      disabled={generateBusy || !selectedPorts.length}
                      onClick={handleGenerateRoute}>
                {generateBusy ? "Generating..." : "Generate"}
              </button>
              <button className="button" onClick={() => { setGenerateModal(null); setGenerateError(""); }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
