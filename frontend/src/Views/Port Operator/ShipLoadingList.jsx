import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { buildErrorMessage, getLoadShips } from "./stowageApi";

export default function ShipLoadingList() {
  const [ships, setShips] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    openLoadList();
  }, []);

  async function openLoadList() {
    setIsLoading(true);
    setError("");
    try {
      const data = await getLoadShips();
      setShips(data);
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to load ships."));
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Pakrauti laivą</h2>
          <p className="muted">Select a ship and then selectPlan for loading.</p>
        </div>
      </div>

      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="card table-card">
        {isLoading ? (
          <p>Loading ships...</p>
        ) : ships.length === 0 ? (
          <p>No ships found.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Port</th>
                  <th>Capacity</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {ships.map((ship) => (
                  <tr key={ship.id}>
                    <td>{ship.id}</td>
                    <td>{ship.name}</td>
                    <td>{ship.type || "-"}</td>
                    <td>{ship.state || "-"}</td>
                    <td>{ship.port?.name || "-"}</td>
                    <td>{ship.capacity || "-"}</td>
                    <td>
                      <Link className="button button-small" to={`/load-ships/${ship.id}`}>
                        Select plan
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
