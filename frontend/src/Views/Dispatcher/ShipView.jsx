import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  getShip, getShipRoutes, getActiveRoute,
  receiveShip, departShip, buildErrorMessage,
} from "./shipApi";

const STATE_LABELS = { DISPATCHER: "At dispatcher", ARRIVED: "Received", DEPARTED: "Dispatched", AWAITING_DOCKING: "Awaiting arrival" };
const STATE_CLASS  = { DISPATCHER: "badge-neutral", ARRIVED: "badge-success", DEPARTED: "badge-warning", AWAITING_DOCKING: "badge-info" };
const SEG_STATE    = { UNVISITED: "Not started", ONGOING: "In progress", VISITED: "Visited" };
const SEG_CLASS    = { UNVISITED: "badge-neutral", ONGOING: "badge-info", VISITED: "badge-success" };

export default function ShipView() {
  const { id } = useParams();
  const [ship, setShip]               = useState(null);
  const [routes, setRoutes]           = useState([]);
  const [activeRoute, setActiveRoute] = useState(null);
  const [error, setError]             = useState("");
  const [actionError, setActionError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");
  const [busy, setBusy]               = useState(false);

  useEffect(() => { load(); }, [id]);

  async function load() {
    setError("");
    try {
      const [s, r, ap] = await Promise.allSettled([
        getShip(id), getShipRoutes(id), getActiveRoute(id),
      ]);
      if (s.status === "fulfilled") setShip(s.value);
      else { setError(buildErrorMessage(s.reason, "Ship not found.")); return; }
      if (r.status === "fulfilled") setRoutes(r.value);
      if (ap.status === "fulfilled") setActiveRoute(ap.value);
    } catch (e) {
      setError(buildErrorMessage(e, "Error loading data."));
    }
  }

  async function handleAction(fn, label) {
    setBusy(true); setActionError(""); setActionSuccess("");
    try {
      const result = await fn(id);
      const updatedShip = result?.ship ?? result;
      setShip(updatedShip);
      if (result?.hasActiveRoute !== undefined) {
        // receiveShip response
        if (result.hasActiveRoute) {
          if (result.sufficientFuel) {
            setActionSuccess(
              `Ship received. Fuel updated: ${result.currentFuelAmount?.toFixed(0)} l. ` +
              `Next segment requires ${result.nextSegmentFuelRequired?.toFixed(0)} l — sufficient. ` +
              `Route recalculated.`
            );
          } else {
            setActionError(
              `Ship received, but insufficient fuel for the next segment! ` +
              `Available: ${result.currentFuelAmount?.toFixed(0)} l, ` +
              `required: ${result.nextSegmentFuelRequired?.toFixed(0)} l. Refuel before dispatching.`
            );
          }
        } else {
          setActionSuccess(`Ship received. Route completed. Fuel: ${result.currentFuelAmount?.toFixed(0)} l.`);
        }
      }
      await load();
    } catch (e) {
      setActionError(buildErrorMessage(e, `${label} failed.`));
    } finally { setBusy(false); }
  }

  if (error) return <div className="alert alert-error" style={{ margin: 20 }}>{error}</div>;
  if (!ship) return <p style={{ padding: 20 }}>Loading...</p>;

  return (
    <section className="stack-lg">
      {/* Header */}
      <div className="page-heading">
        <div>
          <h2>{ship.name}</h2>
          <p className="muted">
            <span className={`badge ${STATE_CLASS[ship.state] || "badge-neutral"}`}>
              {STATE_LABELS[ship.state] || ship.state}
            </span>
            {ship.port && <span style={{ marginLeft: 10 }}>{ship.port.name}</span>}
          </p>
        </div>
        <div className="action-row">
          <Link className="button" to={`/ships/${id}/edit`}>Edit</Link>
          <Link className="button" to="/ships">← Ship list</Link>
        </div>
      </div>

      {actionSuccess && <div className="alert alert-success">{actionSuccess}</div>}
      {actionError   && <div className="alert alert-error">{actionError}</div>}

      {/* Ship details */}
      <div className="card">
        <div className="details-grid">
          {[
            ["Type",              ship.type],
            ["Country",           ship.country],
            ["Weight",            ship.weight ? `${ship.weight} t` : null],
            ["Capacity",          `${ship.capacity} containers`],
            ["Fuel consumption",  `${ship.baseFuelConsumption} l/km`],
            ["Fuel amount",       `${ship.fuelAmount?.toFixed(0)} l`],
            ["Dimensions",        `${ship.length}×${ship.width}×${ship.height} m`],
          ].map(([label, value]) => (
            <div key={label} className="detail-item">
              <span className="detail-label">{label}</span>
              <strong>{value ?? "—"}</strong>
            </div>
          ))}
        </div>

        <div className="action-row" style={{ marginTop: 16 }}>
          {ship.state === "AWAITING_DOCKING" && (
            <button className="button button-primary" disabled={busy}
                    onClick={() => handleAction(receiveShip, "Receive")}>
              {busy ? "..." : "Receive ship"}
            </button>
          )}
          {ship.state === "ARRIVED" && activeRoute && (
            <button className="button button-primary" disabled={busy}
                    onClick={() => handleAction(departShip, "Dispatch")}>
              {busy ? "..." : "Dispatch ship"}
            </button>
          )}
        </div>
      </div>

      {/* Active route */}
      {activeRoute && (
        <div className="card">
          <div className="section-heading" style={{ marginBottom: 16 }}>
            <h3>Active route: {activeRoute.route?.name}</h3>
            <Link className="button button-small" to={`/routes/${activeRoute.route?.id}`}>View →</Link>
          </div>
          {activeRoute.segments?.length > 0 && (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>#</th><th>From</th><th>To</th><th>Status</th>
                    <th>Fuel consumption</th><th>Fuel remaining</th>
                  </tr>
                </thead>
                <tbody>
                  {activeRoute.segments.map(seg => {
                    const fa = activeRoute.fuelAnalyses?.find(f => f.routeSegment?.id === seg.id);
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
            </div>
          )}
        </div>
      )}

      {/* Route history */}
      {routes.length > 0 && (
        <div className="card">
          <h3 style={{ marginBottom: 16 }}>Route history</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Name</th><th>Status</th><th></th></tr>
              </thead>
              <tbody>
                {routes.map(r => (
                  <tr key={r.id}>
                    <td>{r.name}</td>
                    <td>
                      <span className={`badge ${r.state === "ACTIVE" ? "badge-info" : "badge-neutral"}`}>
                        {r.state === "ACTIVE" ? "Active" : "Finished"}
                      </span>
                    </td>
                    <td>
                      <Link className="button button-small" to={`/routes/${r.id}`}>View</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  );
}
