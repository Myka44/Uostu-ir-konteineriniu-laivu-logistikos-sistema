import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { buildErrorMessage, getStowagePlans } from "./stowageApi";
import "./stowage.css";

function planShipName(plan) {
  return plan?.ship?.name || "-";
}

function planPortName(plan) {
  return plan?.port?.name || "-";
}

export default function StowageList() {
  const location = useLocation();
  const [plans, setPlans] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    openStowagePlans();
  }, []);

  async function openStowagePlans() {
    setIsLoading(true);
    setError("");
    try {
      const data = await getStowagePlans();
      setPlans(data);
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to load stowage plans."));
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Stowage plans</h2>
          <p className="muted">View stowage plans and open detailed plan views.</p>
        </div>
        <Link className="button button-primary" to="/stowage-plans/new">
          Create new stowage plan
        </Link>
      </div>

      {location.state?.message ? <div className="alert alert-success">{location.state.message}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="card table-card">
        {isLoading ? (
          <p>Loading stowage plans...</p>
        ) : plans.length === 0 ? (
          <p>No stowage plans found.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Date</th>
                  <th>Ship</th>
                  <th>Port</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Containers</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {plans.map((plan) => (
                  <tr key={plan.id}>
                    <td>{plan.id}</td>
                    <td>{plan.data || "-"}</td>
                    <td>{planShipName(plan)}</td>
                    <td>{planPortName(plan)}</td>
                    <td>{plan.stowageType || "-"}</td>
                    <td><span className="badge">{plan.stowageStatus || "-"}</span></td>
                    <td>{plan.coordinates?.length ?? 0}</td>
                    <td>
                      <div className="action-row">
                        <Link className="button button-small" to={`/stowage-plans/${plan.id}`}>
                          Details
                        </Link>
                        {plan.ship?.id ? (
                          <Link className="button button-small" to={`/load-ships/${plan.ship.id}`}>
                            Pakrauti laivą
                          </Link>
                        ) : null}
                      </div>
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
