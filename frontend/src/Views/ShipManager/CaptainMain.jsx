import { Fragment, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  getAllShips,
  reportDocking,
  getActiveRoute,
  buildErrorMessage,
} from "../Dispatcher/shipApi";

const STATE_LABELS = {
  DEPARTED: "At sea",
  AWAITING_DOCKING: "Awaiting docking",
};
const STATE_CLASS = {
  DEPARTED: "badge-warning",
  AWAITING_DOCKING: "badge-info",
};
const SEG_STATE = { UNVISITED: "Not started", ONGOING: "In progress", VISITED: "Visited" };
const SEG_CLASS = { UNVISITED: "badge-neutral", ONGOING: "badge-info", VISITED: "badge-success" };

export default function CaptainMain() {
  const [ships, setShips]             = useState([]);
  const [activeRoutes, setActiveRoutes] = useState({});
  const [expandedId, setExpandedId]   = useState(null);
  const [isLoading, setIsLoading]     = useState(true);
  const [error, setError]             = useState("");
  const [actionMsg, setActionMsg]     = useState("");
  const [actionError, setActionError] = useState("");
  const [busyId, setBusyId]           = useState(null);

  useEffect(() => { load(); }, []);

  async function load() {
    setIsLoading(true); setError("");
    try {
      const all = await getAllShips();
      const atSea = all.filter(s => s.state === "DEPARTED" || s.state === "AWAITING_DOCKING");
      setShips(atSea);

      const routeResults = await Promise.allSettled(atSea.map(s => getActiveRoute(s.id)));
      const routeMap = {};
      routeResults.forEach((result, idx) => {
        if (result.status === "fulfilled" && result.value)
          routeMap[atSea[idx].id] = result.value;
      });
      setActiveRoutes(routeMap);
    } catch (e) {
      setError(buildErrorMessage(e, "Failed to load ships."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleReportDocking(shipId, shipName) {
    setBusyId(shipId); setActionError(""); setActionMsg("");
    try {
      await reportDocking(shipId);
      setActionMsg(`Ship "${shipName}" successfully reported arrival at port.`);
      await load();
    } catch (e) {
      setActionError(buildErrorMessage(e, "Report failed."));
    } finally {
      setBusyId(null);
    }
  }

  const atSeaShips    = ships.filter(s => s.state === "DEPARTED");
  const awaitingShips = ships.filter(s => s.state === "AWAITING_DOCKING");

  function RouteRows({ ship, colSpan }) {
    const routeDetail = activeRoutes[ship.id];
    if (!routeDetail) return null;
    return (
      <tr>
        <td colSpan={colSpan} style={{ padding: 0, background: "#f8fafc" }}>
          <div style={{ padding: "12px 20px" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 10 }}>
              <strong style={{ fontSize: 14 }}>Route: {routeDetail.route?.name}</strong>
              <Link className="button button-small" to={`/routes/${routeDetail.route?.id}`}>
                View full route →
              </Link>
            </div>
            {routeDetail.segments?.length > 0 ? (
              <table style={{ width: "100%", fontSize: 13 }}>
                <thead>
                  <tr>
                    <th>#</th><th>From</th><th>To</th><th>Status</th>
                    <th>Fuel consumption</th><th>Fuel remaining</th>
                  </tr>
                </thead>
                <tbody>
                  {routeDetail.segments.map(seg => {
                    const fa = routeDetail.fuelAnalyses?.find(f => f.routeSegment?.id === seg.id);
                    return (
                      <tr key={seg.id}>
                        <td>{seg.sequenceNumber}</td>
                        <td>{seg.startPort?.name ?? "—"}</td>
                        <td>{seg.destinationPort?.name ?? "—"}</td>
                        <td>
                          <span className={`badge ${SEG_CLASS[seg.state] || "badge-neutral"}`}>
                            {SEG_STATE[seg.state] || seg.state}
                          </span>
                        </td>
                        <td>{fa ? `${fa.predictedFuelConsumption?.toFixed(1)} l` : "—"}</td>
                        <td>{fa ? `${fa.predictedFuelRemaining?.toFixed(1)} l` : "—"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            ) : (
              <p className="muted" style={{ fontSize: 13 }}>No segments found.</p>
            )}
          </div>
        </td>
      </tr>
    );
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Captain dashboard</h2>
          <p className="muted">Ships currently at sea — view active route and report arrival.</p>
        </div>
      </div>

      {error        && <div className="alert alert-error">{error}</div>}
      {actionMsg    && <div className="alert alert-success">{actionMsg}</div>}
      {actionError  && <div className="alert alert-error">{actionError}</div>}

      {isLoading ? (
        <p>Loading...</p>
      ) : ships.length === 0 ? (
        <div className="card">
          <p className="muted" style={{ padding: 8 }}>No ships are currently at sea.</p>
        </div>
      ) : (
        <>
          {/* At sea — can report arrival */}
          {atSeaShips.length > 0 && (
            <div className="card">
              <h3 style={{ marginBottom: 4 }}>At sea ({atSeaShips.length})</h3>
              <p className="muted" style={{ marginBottom: 16 }}>
                Ships that can report arrival at port.
              </p>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Type</th>
                      <th>Fuel (l)</th>
                      <th>Active route</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {atSeaShips.map(ship => (
                      <Fragment key={ship.id}>
                        <tr>
                          <td><strong>{ship.name}</strong></td>
                          <td>{ship.type}</td>
                          <td>{ship.fuelAmount?.toFixed(0) ?? "—"}</td>
                          <td>
                            {activeRoutes[ship.id] ? (
                              <button className="button button-small"
                                      onClick={() => setExpandedId(prev => prev === ship.id ? null : ship.id)}>
                                {expandedId === ship.id
                                  ? "Hide route ▲"
                                  : `${activeRoutes[ship.id].route?.name ?? "Route"} ▼`}
                              </button>
                            ) : (
                              <span className="muted" style={{ fontSize: 13 }}>No active route</span>
                            )}
                          </td>
                          <td>
                            <button className="button button-small button-primary"
                                    disabled={busyId === ship.id}
                                    onClick={() => handleReportDocking(ship.id, ship.name)}>
                              {busyId === ship.id ? "Sending..." : "Report arrival"}
                            </button>
                          </td>
                        </tr>
                        {expandedId === ship.id && (
                          <RouteRows ship={ship} colSpan={5} />
                        )}
                      </Fragment>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Awaiting docking — arrival reported, waiting for dispatcher */}
          {awaitingShips.length > 0 && (
            <div className="card">
              <h3 style={{ marginBottom: 4 }}>Awaiting docking ({awaitingShips.length})</h3>
              <p className="muted" style={{ marginBottom: 16 }}>
                Arrival reported — awaiting dispatcher confirmation.
              </p>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Type</th>
                      <th>Active route</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {awaitingShips.map(ship => (
                      <Fragment key={ship.id}>
                        <tr>
                          <td><strong>{ship.name}</strong></td>
                          <td>{ship.type}</td>
                          <td>
                            {activeRoutes[ship.id] ? (
                              <button className="button button-small"
                                      onClick={() => setExpandedId(prev => prev === ship.id ? null : ship.id)}>
                                {expandedId === ship.id
                                  ? "Hide route ▲"
                                  : `${activeRoutes[ship.id].route?.name ?? "Route"} ▼`}
                              </button>
                            ) : (
                              <span className="muted" style={{ fontSize: 13 }}>—</span>
                            )}
                          </td>
                          <td>
                            <span className={`badge ${STATE_CLASS[ship.state]}`}>
                              {STATE_LABELS[ship.state]}
                            </span>
                          </td>
                        </tr>
                        {expandedId === ship.id && (
                          <RouteRows ship={ship} colSpan={4} />
                        )}
                      </Fragment>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </section>
  );
}
