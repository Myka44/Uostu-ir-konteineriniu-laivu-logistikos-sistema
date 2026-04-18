import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  buildErrorMessage,
  getContainer,
  removeContainer
} from "../api/containerApi";

export default function ContainerViewPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [container, setContainer] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    loadContainer();
  }, [id]);

  async function loadContainer() {
    setIsLoading(true);
    setError("");

    try {
      const data = await getContainer(id);
      setContainer(data);
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to load the container."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleDelete() {
    if (!window.confirm(`Delete container #${id}?`)) return;

    setIsDeleting(true);
    setError("");

    try {
      await removeContainer(id);
      navigate("/containers", {
        replace: true,
        state: { message: `Container #${id} deleted successfully.` }
      });
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to delete the container."));
    } finally {
      setIsDeleting(false);
    }
  }

  if (isLoading) {
    return <p>Loading container...</p>;
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Container details</h2>
        </div>
        <Link to="/containers" className="button">
          Back to list
        </Link>
      </div>

      {error ? <div className="alert alert-error">{error}</div> : null}

      {!container ? (
        <div className="card">
          <p>Container not found.</p>
        </div>
      ) : (
        <div className="card details-card">
          <div className="details-grid">
            <Detail label="ID" value={container.id} />
            <Detail label="Type" value={container.type} />
            <Detail label="Weight" value={container.weight} />
            <Detail label="Volume" value={container.volume} />
            <Detail label="Max weight" value={container.maxWeight} />
            <Detail label="Max volume" value={container.maxVolume} />
            <Detail label="Warning label" value={container.warningLabel || "-"} />
          </div>

          <div className="action-row">
            <Link className="button button-primary" to={`/containers/${container.id}/edit`}>
              Edit
            </Link>
            <button
              className="button button-danger"
              onClick={handleDelete}
              disabled={isDeleting}
            >
              {isDeleting ? "Deleting..." : "Delete"}
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

function Detail({ label, value }) {
  return (
    <div className="detail-item">
      <span className="detail-label">{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
