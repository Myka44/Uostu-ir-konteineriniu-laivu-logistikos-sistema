import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getAllRoutes, buildErrorMessage } from "./shipApi";

export default function RouteListView() {
  const [routes, setRoutes] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setIsLoading(true);
    getAllRoutes()
      .then(setRoutes)
      .catch((e) => setError(buildErrorMessage(e, "Failed to load routes.")))
      .finally(() => setIsLoading(false));
  }, []);

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Routes</h2>
          <p className="muted">All generated ship routes.</p>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card table-card">
        {isLoading ? (
          <p style={{ padding: 20 }}>Loading...</p>
        ) : routes.length === 0 ? (
          <p style={{ padding: 20 }}>No routes found.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Ship</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {routes.map((r) => (
                  <tr key={r.id}>
                    <td>{r.name || `Route #{r.id}`}</td>
                    <td>{r.ship?.name ?? "—"}</td>
                    <td>
                      <span className={`badge ${r.state === "ACTIVE" ? "badge-info" : "badge-neutral"}`}>
                        {r.state === "ACTIVE" ? "Active" : "Completed"}
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
        )}
      </div>
    </section>
  );
}