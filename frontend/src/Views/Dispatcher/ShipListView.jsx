import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  getAllShips,
  receiveShip,
  departShip,
  deleteShip,
  getActiveRoute,
  generateRoute,
  getAllPorts,
  buildErrorMessage,
} from "./shipApi";

const STATE_LABELS = {
  ARRIVED: "Received",
  DEPARTED: "Dispatched",
  AWAITING_DOCKING: "Awaiting arrival",
};

const STATE_CLASS = {
  ARRIVED: "badge-success",
  DEPARTED: "badge-warning",
  AWAITING_DOCKING: "badge-info",
};

const ROUTE_STATE_LABELS = {
  ACTIVE: "Active",
  FINISHED: "Finished",
};

const ROUTE_STATE_CLASS = {
  ACTIVE: "badge-info",
  FINISHED: "badge-success",
};

const SEG_STATE_LABELS = {
  UNVISITED: "Not started",
  ONGOING: "In progress",
  VISITED: "Visited",
};

export default function ShipListView() {
  const location = useLocation();
  const [ships, setShips] = useState([]);
  const [activeRoutes, setActiveRoutes] = useState({}); // shipId → routeDetail
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
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
      const routeResults = await Promise.allSettled(
        shipList.filter((s) => s.id != null).map((s) => getActiveRoute(s.id))
      );
      const routeMap = {};
      routeResults.forEach((result, idx) => {
        if (result.status === "fulfilled" && result.value) {
          routeMap[shipList[idx].id] = result.value;
        }
      });
      setActiveRoutes(routeMap);
    } catch (e) {
      setError(buildErrorMessage(e, "Failed to load ships."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleAction(id, fn, label) {
    setBusyId(id);
    setActionError("");
    try {
      const updated = await fn(id);
      setShips((prev) => prev.map((s) => (s.id === id ? updated : s)));
      try {
        const route = await getActiveRoute(id);
        setActiveRoutes((prev) => ({ ...prev, [id]: route }));
      } catch {
        setActiveRoutes((prev) => { const n = { ...prev }; delete n[id]; return n; });
      }
    } catch (e) {
      setActionError(buildErrorMessage(e, `${label} failed.`));
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(id) {
    setBusyId(id);
    setActionError("");
    try {
      await deleteShip(id);
      setShips((prev) => prev.filter((s) => s.id !== id));
      setActiveRoutes((prev) => { const n = { ...prev }; delete n[id]; return n; });
      setConfirmDelete(null);
    } catch (e) {
      setActionError(buildErrorMessage(e, "Deletion failed."));
    } finally {
      setBusyId(null);
    }
  }

  async function openGenerateModal(ship) {
    setGenerateModal(ship);
    setSelectedPorts([]);
    setRouteName("");
    setGenerateError("");
    if (ports.length === 0) {
      try {
        const all = await getAllPorts();
        setPorts(all);
      } catch (e) {
        setGenerateError("Failed to load ports.");
      }
    }
  }

  function togglePort(portId) {
    setSelectedPorts((prev) =>
      prev.includes(portId) ? prev.filter((p) => p !== portId) : [...prev, portId]
    );
  }

  async function handleGenerateRoute() {
    if (selectedPorts.length === 0) {
      setGenerateError("Select at least one destination port.");
      return;
    }
    setGenerateBusy(true);
    setGenerateError("");
    try {
      await generateRoute(generateModal.id, {
        destinationPortIds: selectedPorts,
        routeName: routeName.trim() || undefined,
      });
      try {
        const detail = await getActiveRoute(generateModal.id);
        setActiveRoutes((prev) => ({ ...prev, [generateModal.id]: detail }));
      } catch {}
      setGenerateModal(null);
    } catch (e) {
      setGenerateError(buildErrorMessage(e, "Route generation failed."));
    } finally {
      setGenerateBusy(false);
    }
  }

  function getRouteInfo(routeDetail) {
    if (!routeDetail) return null;
    const { route, segments } = routeDetail;
    const ongoing = segments?.find((s) => s.segment?.state === "ONGOING");
    const nextUnvisited = segments?.find((s) => s.segment?.state === "UNVISITED");
    const current = ongoing || nextUnvisited;
    if (!current) return { routeState: route?.state, routeId: route?.id, routeName: route?.name, segmentLabel: "All segments visited" };
    const seg = current.segment;
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
          <p className="muted">Manage ships, their routes and dispatch.</p>
        </div>
        <Link className="button button-primary" to="/ships/new">
          + Register ship
        </Link>
      </div>

      {location.state?.message && (
        <div className="alert alert-success">{location.state.message}</div>
      )}
      {error && <div className="alert alert-error">{error}</div>}
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
                  <th>Active Route</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {ships.map((ship) => {
                  const routeInfo = getRouteInfo(activeRoutes[ship.id]);
                  const hasActiveRoute = !!activeRoutes[ship.id];
                  return (
                    <tr key={ship.id}>
                      <td>
                        <Link to={`/ships/${ship.id}`} className="link">
                          {ship.name}
                        </Link>
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
                        {routeInfo ? (
                          <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                            <span
                              className={`badge ${ROUTE_STATE_CLASS[routeInfo.routeState] || "badge-neutral"}`}
                              style={{ width: "fit-content" }}
                            >
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
                              <Link
                                to={`/routes/${routeInfo.routeId}`}
                                className="link"
                                style={{ fontSize: 12 }}
                              >
                                View route →
                              </Link>
                            )}
                          </div>
                        ) : (
                          <span className="muted" style={{ fontSize: 13 }}>No active route</span>
                        )}
                      </td>
                      <td>
                        <div className="action-row">
                          <Link className="button button-small" to={`/ships/${ship.id}`}>
                            View
                          </Link>
                          <Link className="button button-small" to={`/ships/${ship.id}/edit`}>
                            Edit
                          </Link>
                          {ship.state === "ARRIVED" && !hasActiveRoute && (
                            <button
                              className="button button-small button-primary"
                              disabled={busyId === ship.id}
                              onClick={() => openGenerateModal(ship)}
                            >
                              Generate Route
                            </button>
                          )}
                          {ship.state === "AWAITING_DOCKING" && (
                            <button
                              className="button button-small button-primary"
                              disabled={busyId === ship.id}
                              onClick={() => handleAction(ship.id, receiveShip, "Receival")}
                            >
                              Receive
                            </button>
                          )}
                          {ship.state === "ARRIVED" && hasActiveRoute && (
                            <button
                              className="button button-small button-primary"
                              disabled={busyId === ship.id}
                              onClick={() => handleAction(ship.id, departShip, "Dispatch")}
                            >
                              Dispatch
                            </button>
                          )}
                          <button
                            className="button button-small button-danger"
                            disabled={busyId === ship.id}
                            onClick={() => setConfirmDelete(ship)}
                          >
                            Delete
                          </button>
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

      {/* Delete confirm modal */}
      {confirmDelete && (
        <div className="modal-backdrop">
          <div className="modal card">
            <h2 style={{ marginBottom: 12 }}>Confirm deletion</h2>
            <p>
              Are you sure you want to delete ship <strong>{confirmDelete.name}</strong>?
              All its routes and coordinates will be deleted.
            </p>
            {actionError && (
              <div className="alert alert-error" style={{ marginTop: 12 }}>{actionError}</div>
            )}
            <div className="modal-actions" style={{ marginTop: 20 }}>
              <button
                className="button button-danger"
                disabled={busyId === confirmDelete.id}
                onClick={() => handleDelete(confirmDelete.id)}
              >
                {busyId === confirmDelete.id ? "Deleting..." : "Yes, delete"}
              </button>
              <button className="button" onClick={() => setConfirmDelete(null)}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Generate Route modal */}
      {generateModal && (
        <div className="modal-backdrop">
          <div className="modal card" style={{ maxWidth: 480 }}>
            <h2 style={{ marginBottom: 4 }}>Generate Route</h2>
            <p className="muted" style={{ marginBottom: 16 }}>
              Ship: <strong>{generateModal.name}</strong> &mdash; port:{" "}
              <strong>{generateModal.port?.name ?? "Unknown"}</strong>
            </p>

            {generateError && (
              <div className="alert alert-error" style={{ marginBottom: 12 }}>
                {generateError}
              </div>
            )}

            <div style={{ marginBottom: 16 }}>
              <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
                Route name <span style={{ color: "#627b92", fontWeight: 400 }}>(optional)</span>
              </label>
              <input
                className="input"
                type="text"
                placeholder="Auto-generated if empty"
                value={routeName}
                onChange={(e) => setRouteName(e.target.value)}
                style={{ width: "100%", boxSizing: "border-box" }}
              />
            </div>

            <div style={{ marginBottom: 20 }}>
              <label style={{ display: "block", marginBottom: 6, fontWeight: 500 }}>
                Destination ports <span style={{ color: "#e05" }}>*</span>
              </label>
              <p style={{ fontSize: 13, color: "#627b92", marginBottom: 8 }}>
                Select ports in visit order. Numbers show visit sequence.
              </p>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: 6,
                  maxHeight: 220,
                  overflowY: "auto",
                  border: "1px solid #dde3ea",
                  borderRadius: 8,
                  padding: 8,
                }}
              >
                {ports
                  .filter((p) => p.id !== generateModal.port?.id)
                  .map((port) => (
                    <label
                      key={port.id}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: 10,
                        padding: "8px 10px",
                        borderRadius: 6,
                        border: selectedPorts.includes(port.id)
                          ? "1.5px solid #2563eb"
                          : "1.5px solid transparent",
                        cursor: "pointer",
                        background: selectedPorts.includes(port.id) ? "#eff6ff" : "transparent",
                        fontSize: 14,
                        userSelect: "none",
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={selectedPorts.includes(port.id)}
                        onChange={() => togglePort(port.id)}
                        style={{ accentColor: "#2563eb", width: 16, height: 16 }}
                      />
                      <span>{port.name}</span>
                      {selectedPorts.includes(port.id) && (
                        <span
                          style={{
                            marginLeft: "auto",
                            fontSize: 12,
                            color: "#fff",
                            background: "#2563eb",
                            borderRadius: "50%",
                            width: 20,
                            height: 20,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontWeight: 700,
                            flexShrink: 0,
                          }}
                        >
                          {selectedPorts.indexOf(port.id) + 1}
                        </span>
                      )}
                    </label>
                  ))}
                {ports.filter((p) => p.id !== generateModal.port?.id).length === 0 && (
                  <p style={{ color: "#627b92", fontSize: 13, padding: 8 }}>
                    No other ports available.
                  </p>
                )}
              </div>
            </div>

            <div className="modal-actions">
              <button
                className="button button-primary"
                disabled={generateBusy || selectedPorts.length === 0}
                onClick={handleGenerateRoute}
              >
                {generateBusy ? "Generating..." : "Generate"}
              </button>
              <button
                className="button"
                onClick={() => { setGenerateModal(null); setGenerateError(""); }}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
