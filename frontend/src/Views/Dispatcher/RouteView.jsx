import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getRoute, getRouteSegments, recalculateRoute, buildErrorMessage } from "./shipApi";

const SEG_STATE_LABELS = {
  UNVISITED: "Not visited",
  ONGOING: "In progress",
  VISITED: "Visited",
};
const SEG_STATE_CLASS = {
  UNVISITED: "badge-neutral",
  ONGOING: "badge-info",
  VISITED: "badge-success",
};

export default function RouteView() {
  const { id } = useParams();
  const [detail, setDetail] = useState(null);
  const [segments, setSegments] = useState([]);
  const [error, setError] = useState("");
  const [recalcResult, setRecalcResult] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => { load(); }, [id]);

  async function load() {
    setError("");
    try {
      const [d, segs] = await Promise.all([getRoute(id), getRouteSegments(id)]);
      setDetail(d);
      setSegments(segs);
    } catch (e) {
      setError(buildErrorMessage(e, "Failed to load route."));
    }
  }

  async function handleRecalculate() {
    setBusy(true); setError(""); setRecalcResult(null);
    try {
      const result = await recalculateRoute(id);
      setRecalcResult(result);
      await load();
    } catch (e) {
      setError(buildErrorMessage(e, "Recalculation failed."));
    } finally { setBusy(false); }
  }

  if (error && !detail) return <div className="alert alert-error" style={{ margin: 20 }}>{error}</div>;
  if (!detail) return <p style={{ padding: 20 }}>Loading...</p>;

  const { route } = detail;

  return (
      <section className="stack-lg">
        <div className="page-heading">
          <div>
            <h2>{route.name || `Route #{route.id}`}</h2>
            <p className="muted">
            <span className={`badge ${route.state === "ACTIVE" ? "badge-info" : "badge-neutral"}`}>
              {route.state === "ACTIVE" ? "Active" : "Completed"}
            </span>
              {route.ship && (
                  <span style={{ marginLeft: 10 }}>
                <Link to={`/ships/${route.ship.id}`} className="link">{route.ship.name}</Link>
              </span>
              )}
            </p>
          </div>
          <div className="action-row">
            {route.state === "ACTIVE" && (
                <button className="button button-primary" disabled={busy} onClick={handleRecalculate}>
                  {busy ? "Recalculating..." : "Recalculate"}
                </button>
            )}
            <Link className="button" to="/routes">← Routes</Link>
          </div>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {recalcResult && (
            <div className={`alert ${recalcResult.sufficientFuel ? "alert-success" : "alert-error"}`}>
              {recalcResult.sufficientFuel
                  ? `Fuel sufficient. Remaining: ~${recalcResult.remainingFuelEstimate?.toFixed(0)} l.`
                  : `Insufficient fuel for next segment! Remaining: ~${recalcResult.remainingFuelEstimate?.toFixed(0)} l.`}
              {recalcResult.usedWeather && (
                  <span style={{ marginLeft: 12, color: "#627b92", fontSize: 13 }}>
              Weather: wind {recalcResult.usedWeather.windSpeed?.toFixed(1)} m/s,
              waves {recalcResult.usedWeather.waveHeight?.toFixed(1)} m
            </span>
              )}
            </div>
        )}

        {/* Segments */}
        <div className="card table-card">
          <div style={{ padding: "16px 20px 0", borderBottom: "1px solid #e9eff6" }}>
            <h3>Segments</h3>
          </div>
          {segments.length === 0 ? (
              <p style={{ padding: 20 }}>No segments found.</p>
          ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                  <tr>
                    <th>#</th>
                    <th>From</th>
                    <th>To</th>
                    <th>Status</th>
                    <th>Predicted consumption</th>
                    <th>Fuel remaining</th>
                    <th>Coordinates</th>
                    <th></th>
                  </tr>
                  </thead>
                  <tbody>
                  {segments.map(({ segment, fuelAnalysis, coordinates }) => (
                      <tr key={segment.id}>
                        <td>{segment.sequenceNumber}</td>
                        <td>{segment.startPort?.name ?? "—"}</td>
                        <td>{segment.destinationPort?.name ?? "—"}</td>
                        <td>
                      <span className={`badge ${SEG_STATE_CLASS[segment.state] || "badge-neutral"}`}>
                        {SEG_STATE_LABELS[segment.state] || segment.state}
                      </span>
                        </td>
                        <td>{fuelAnalysis ? `${fuelAnalysis.predictedFuelConsumption?.toFixed(1)} l` : "—"}</td>
                        <td>{fuelAnalysis ? `${fuelAnalysis.predictedFuelRemaining?.toFixed(1)} l` : "—"}</td>
                        <td>{coordinates?.length ?? 0} points</td>
                        <td>
                          <Link className="button button-small" to={`/routes/${id}/segments/${segment.id}`}>
                            View
                          </Link>
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </div>
          )}
        </div>
      </section>
  );
}