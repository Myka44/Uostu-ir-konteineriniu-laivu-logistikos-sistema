import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { buildErrorMessage, getStowagePlan } from "./stowageApi";
import "./stowage.css";

function positionLabel(coordinate) {
  return `${coordinate.lengthPosition}, ${coordinate.widthPosition}, ${coordinate.heightPosition}`;
}

export default function StowageView() {
  const { id } = useParams();
  const location = useLocation();
  const [plan, setPlan] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    open();
  }, [id]);

  async function open() {
    setIsLoading(true);
    setError("");
    try {
      const data = await getStowagePlan(id);
      setPlan(data);
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to load stowage plan."));
    } finally {
      setIsLoading(false);
    }
  }

  const sortedCoordinates = useMemo(() => {
    return [...(plan?.coordinates || [])].sort((a, b) => {
      return a.heightPosition - b.heightPosition
        || a.lengthPosition - b.lengthPosition
        || a.widthPosition - b.widthPosition;
    });
  }, [plan]);

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Stowage plan details</h2>
          <p className="muted">Detailed cargo plan view.</p>
        </div>
        <div className="action-row">
          <Link className="button" to="/stowage-plans">Back to plans</Link>
          {plan?.ship?.id ? <Link className="button button-primary" to={`/load-ships/${plan.ship.id}`}>Pakrauti laivą</Link> : null}
        </div>
      </div>

      {location.state?.message ? <div className="alert alert-success">{location.state.message}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      {isLoading ? <p>Loading plan...</p> : plan ? (
        <div className="stowage-layout">
          <div className="card">
            <div className="details-grid">
              <div className="detail-item"><span className="detail-label">ID</span><strong>{plan.id}</strong></div>
              <div className="detail-item"><span className="detail-label">Ship</span><strong>{plan.ship?.name || "-"}</strong></div>
              <div className="detail-item"><span className="detail-label">Port</span><strong>{plan.port?.name || "-"}</strong></div>
              <div className="detail-item"><span className="detail-label">Type</span><strong>{plan.stowageType || "-"}</strong></div>
              <div className="detail-item"><span className="detail-label">Status</span><strong>{plan.stowageStatus || "-"}</strong></div>
              <div className="detail-item"><span className="detail-label">Containers</span><strong>{plan.coordinates?.length || 0}</strong></div>
            </div>
            <h3>Visualization</h3>
            <div className="stowage-map">
              {sortedCoordinates.map((coordinate) => (
                <div
                  key={coordinate.id || `${coordinate.container?.id}-${positionLabel(coordinate)}`}
                  className={`stowage-cell ${coordinate.container?.warningLabel ? "stowage-cell-danger" : ""}`}
                  title={`Position ${positionLabel(coordinate)}`}
                >
                  C{coordinate.container?.id}
                </div>
              ))}
            </div>
          </div>

          <div className="card table-card">
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Container</th>
                    <th>Type</th>
                    <th>Weight</th>
                    <th>Warning</th>
                    <th>Coordinates</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedCoordinates.map((coordinate) => (
                    <tr key={coordinate.id || `${coordinate.container?.id}-${positionLabel(coordinate)}`}>
                      <td>{coordinate.container?.id || "-"}</td>
                      <td>{coordinate.container?.type || "-"}</td>
                      <td>{coordinate.container?.weight ?? "-"}</td>
                      <td>{coordinate.container?.warningLabel || "-"}</td>
                      <td>{positionLabel(coordinate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
}
