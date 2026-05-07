import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { buildErrorMessage, getOrder } from "../Port Operator/orderApi";

export default function OrderDetailView() {
  const { id } = useParams();
  const location = useLocation();
  const [order, setOrder] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [showDelete, setShowDelete] = useState(false);

  useEffect(() => {
    open();
  }, []);

  async function open() {
    setIsLoading(true);
    setError("");

    try {
      const data = await getOrder(id);
      setOrder(data);
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to load order."));
    } finally {
      setIsLoading(false);
    }
  }

  function handleDeleteClick() {
    setShowDelete(true);
  }

  function handleCancelDelete() {
    setShowDelete(false);
  }

  function handleConfirmDelete() {
    // Stub: delete not implemented yet
    alert("Delete order - not implemented");
    setShowDelete(false);
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Order detail</h2>
          <p className="muted">Details for order #{id}</p>
        </div>
      </div>

      {location.state?.message ? (
        <div className="alert alert-success">{location.state.message}</div>
      ) : null}

      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="card">
        {isLoading ? (
          <p>Loading order...</p>
        ) : order == null ? (
          <p>Order not found.</p>
        ) : (
          <div>
            <p><strong>ID:</strong> {order.id}</p>
            <p><strong>Status:</strong> {order.busena}</p>
            <p><strong>Creation date:</strong> {order.sukurimoData}</p>
            <p><strong>Departure port:</strong> {order.isvykimoUostas}</p>
            <p><strong>Arrival port:</strong> {order.atvykimoUostas}</p>
            <p><strong>Client ID:</strong> {order.clientId}</p>

            {order.busena === "LAUKIAMA" ? (
              <div className="action-row">
                <Link className="button" to={`/orders/${order.id}/edit`}>
                  Edit
                </Link>
                <button className="button button-danger" onClick={handleDeleteClick}>
                  Delete
                </button>
              </div>
            ) : null}
          </div>
        )}
      </div>

      {showDelete ? (
        <div className="card">
          <p>Delete order modal (stub)</p>
          <div className="action-row">
            <button className="button" onClick={handleCancelDelete}>Cancel</button>
            <button className="button button-danger" onClick={handleConfirmDelete}>Confirm delete</button>
          </div>
        </div>
      ) : null}
    </section>
  );
}
