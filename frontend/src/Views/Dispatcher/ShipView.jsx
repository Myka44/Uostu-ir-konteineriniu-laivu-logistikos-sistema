import { useEffect, useState } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import {
  getShip, getShipRoutes, getActiveRoute, generateRoute,
  receiveShip, departShip, getAllPorts, buildErrorMessage,
} from "./shipApi";

const STATE_LABELS = { ARRIVED: "Received", DEPARTED: "Dispatched", AWAITING_DOCKING: "Awaiting arrival" };
const STATE_CLASS  = { ARRIVED: "badge-success", DEPARTED: "badge-warning", AWAITING_DOCKING: "badge-info" };
const SEG_STATE    = { UNVISITED: "Not visited", ONGOING: "In progress", VISITED: "Visited" };
const SEG_CLASS    = { UNVISITED: "badge-neutral", ONGOING: "badge-info", VISITED: "badge-success" };

export default function ShipView() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [ship, setShip]             = useState(null);
  const [routes, setRoutes]         = useState([]);
  const [activeRoute, setActiveRoute] = useState(null);
  const [ports, setPorts]           = useState([]);
  const [error, setError]           = useState("");
  const [actionError, setActionError] = useState("");
  const [busy, setBusy]             = useState(false);
  const [showGenForm, setShowGenForm] = useState(false);
  const [genPorts, setGenPorts]     = useState([]);
  const [routeName, setRouteName]   = useState("");

  useEffect(() => { load(); }, [id]);

  async function load() {
    setError("");
    try {
      const [s, r, ap, pts] = await Promise.allSettled([
        getShip(id), getShipRoutes(id), getActiveRoute(id), getAllPorts(),
      ]);
      if (s.status === "fulfilled") setShip(s.value);
      else { setError(buildErrorMessage(s.reason, "Ship not found.")); return; }
      if (r.status === "fulfilled") setRoutes(r.value);
      if (ap.status === "fulfilled") setActiveRoute(ap.value);
      if (pts.status === "fulfilled") setPorts(pts.value);
    } catch (e) {
      setError(buildErrorMessage(e, "Error loading data."));
    }
  }

  async function handleAction(fn, label) {
    setBusy(true); setActionError("");
    try {
      const updated = await fn(id);
      setShip(updated);
      await load();
    } catch (e) {
      setActionError(buildErrorMessage(e, `${label} failed.`));
    } finally { setBusy(false); }
  }

  async function handleGenerateRoute(e) {
    e.preventDefault();
    if (!genPorts.length) { setActionError("Select at least one destination port."); return; }
    setBusy(true); setActionError("");
    try {
      await generateRoute(id, {
        destinationPortIds: genPorts.map(Number),
        routeName: routeName.trim() || undefined,
      });
      setShowGenForm(false);
      setGenPorts([]); setRouteName("");
      await load();
    } catch (e) {
      setActionError(buildErrorMessage(e, "Route generation failed."));
    } finally { setBusy(false); }
  }

  function togglePort(portId) {
    const sid = String(portId);
    setGenPorts((p) => p.includes(sid) ? p.filter((x) => x !== sid) : [...p, sid]);
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

        {actionError && <div className="alert alert-error">{actionError}</div>}

        {/* Ship details */}
        <div className="card">
          <div className="details-grid">
            {[
              ["Type", ship.type],
              ["Country", ship.country],
              ["Weight", `${ship.weight} t`],
              ["Capacity", `${ship.capacity} containers`],
              ["Fuel consumption", `${ship.baseFuelConsumption} l/km`],
              ["Fuel amount", `${ship.fuelAmount?.toFixed(0)} l`],
              ["Dimensions", `${ship.length}×${ship.width}×${ship.height} m`],
            ].map(([label, value]) => (
                <div key={label} className="detail-item">
                  <span className="detail-label">{label}</span>
                  <strong>{value ?? "—"}</strong>
                </div>
            ))}
          </div>

          {/* Action buttons */}
          <div className="action-row" style={{ marginTop: 16 }}>
            {ship.state === "AWAITING_DOCKING" && (
                <button className="button button-primary" disabled={busy}
                        onClick={() => handleAction(receiveShip, "Receival")}>
                  {busy ? "..." : "Receive ship"}
                </button>
            )}
            {ship.state === "RECEIVED" && !activeRoute && (
                <button className="button button-primary" disabled={busy}
                        onClick={() => setShowGenForm(true)}>
                  Generate route
                </button>
            )}
            {ship.state === "RECEIVED" && activeRoute && (
                <button className="button button-primary" disabled={busy}
                        onClick={() => handleAction(departShip, "Dispatch")}>
                  {busy ? "..." : "Dispatch ship"}
                </button>
            )}
          </div>
        </div>

        {/* Generate route form */}
        {showGenForm && (
            <div className="card">
              <h3 style={{ marginBottom: 16 }}>Generate route</h3>
              <form onSubmit={handleGenerateRoute}>
                <label style={{ display: "grid", gap: 8, marginBottom: 16 }}>
                  <span>Route name (optional)</span>
                  <input value={routeName} onChange={(e) => setRouteName(e.target.value)}
                         placeholder={`${ship.name} route`} />
                </label>
                <p style={{ marginBottom: 12, fontWeight: 500 }}>Destination ports (in order):</p>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))", gap: 10, marginBottom: 20 }}>
                  {ports
                      .filter((p) => p.id !== ship.port?.id)
                      .map((p) => (
                          <label key={p.id} style={{ display: "flex", alignItems: "center", gap: 8, cursor: "pointer" }}>
                            <input type="checkbox" checked={genPorts.includes(String(p.id))}
                                   onChange={() => togglePort(p.id)} />
                            {p.name}
                            {genPorts.includes(String(p.id)) && (
                                <span className="badge badge-info" style={{ fontSize: 11 }}>
                        #{genPorts.indexOf(String(p.id)) + 1}
                      </span>
                            )}
                          </label>
                      ))}
                </div>
                <div className="form-actions">
                  <button className="button button-primary" type="submit" disabled={busy}>
                    {busy ? "Generating..." : "Generate"}
                  </button>
                  <button className="button" type="button" onClick={() => { setShowGenForm(false); setGenPorts([]); }}>
                    Cancel
                  </button>
                </div>
              </form>
            </div>
        )}

        {/* Active route */}
        {activeRoute && (
            <div className="card">
              <div className="section-heading" style={{ marginBottom: 16 }}>
                <h3>Active route: {activeRoute.route?.name}</h3>
                <Link className="button button-small" to={`/routes/${activeRoute.route?.id}`}>
                  View →
                </Link>
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
                      {activeRoute.segments.map((seg, i) => {
                        const fa = activeRoute.fuelAnalyses?.find(
                            (f) => f.routeSegment?.id === seg.id
                        );
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
                  {routes.map((r) => (
                      <tr key={r.id}>
                        <td>{r.name}</td>
                        <td>
                      <span className={`badge ${r.state === "ACTIVE" ? "badge-info" : "badge-neutral"}`}>
                        {r.state === "ACTIVE" ? "ACTIVE" : "FINISHED"}
                      </span>
                        </td>
                        <td>
                          <Link className="button button-small" to={`/routes/${r.id}`}>
                            View
                          </Link>
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