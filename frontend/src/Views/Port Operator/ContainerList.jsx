import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import DeleteContainerModal from "./DeleteContainerModal";
import {
  buildErrorMessage,
  getContainers,
  removeContainer
} from "./containerApi";

export default function ContainerListPage() {
  const location = useLocation();
  const [containers, setContainers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedContainer, setSelectedContainer] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    loadContainers();
  }, []);

  async function loadContainers() {
    setIsLoading(true);
    setError("");

    try {
      const data = await getContainers();
      setContainers(data);
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to load containers."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleDelete(containerId) {
    setIsDeleting(true);
    setError("");

    try {
      await removeContainer(containerId);
      setContainers((current) =>
        current.filter((container) => container.id !== containerId)
      );
      setSelectedContainer(null);
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to delete container."));
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>All containers</h2>
          <p className="muted">
            View, edit, create, and delete containers from one list.
          </p>
        </div>
      </div>

      {location.state?.message ? (
        <div className="alert alert-success">{location.state.message}</div>
      ) : null}

      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="card table-card">
        {isLoading ? (
          <p>Loading containers...</p>
        ) : containers.length === 0 ? (
          <p>No containers found.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Type</th>
                  <th>Weight</th>
                  <th>Volume</th>
                  <th>Max weight</th>
                  <th>Max volume</th>
                  <th>Warning label</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {containers.map((container) => (
                  <tr key={container.id}>
                    <td>{container.id}</td>
                    <td>{container.type || "-"}</td>
                    <td>{container.weight !== null && container.weight !== undefined ? Number(container.weight).toFixed(2) : "-"}</td>
                    <td>{container.volume !== null && container.volume !== undefined ? Number(container.volume).toFixed(2) : "-"}</td>
                    <td>{container.maxWeight !== null && container.maxWeight !== undefined ? Number(container.maxWeight).toFixed(2) : "-"}</td>
                    <td>{container.maxVolume !== null && container.maxVolume !== undefined ? Number(container.maxVolume).toFixed(2) : "-"}</td>
                    <td>{container.warningLabel || "-"}</td>
                    <td>
                      <div className="action-row">
                        <Link className="button button-small" to={`/containers/${container.id}`}>
                          View
                        </Link>
                        <Link
                          className="button button-small"
                          to={`/containers/${container.id}/edit`}
                        >
                          Edit
                        </Link>
                        <button
                          className="button button-small button-danger"
                          onClick={() => setSelectedContainer(container)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <DeleteContainerModal
        container={selectedContainer}
        isDeleting={isDeleting}
        onCancel={() => setSelectedContainer(null)}
        onConfirm={handleDelete}
      />
    </section>
  );
}
