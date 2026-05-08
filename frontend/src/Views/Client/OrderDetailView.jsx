import { useEffect, useState } from "react";
import { Link, useLocation, useParams, useNavigate } from "react-router-dom";
import { buildErrorMessage, getOrder, deleteOrder } from "./orderApi";
import DeleteOrderModal from "./DeleteOrderModal";

export default function OrderDetailView() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

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

  function clickDelete() {
    setSelectedOrder(order);
  }

  function handleCancelDelete() {
    setSelectedOrder(null);
  }

  async function confirmDelete(orderId) {
    setIsDeleting(true);
    setError("");
    try {
      await deleteOrder(orderId);
      navigate(`/orders`, { state: { message: "Order cancelled successfully." } });
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to delete order."));
    } finally {
      setIsDeleting(false);
      setSelectedOrder(null);
    }
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
                <button className="button button-danger" onClick={clickDelete}>
                  Delete
                </button>
              </div>
            ) : null}
          </div>
        )}
      </div>

      <DeleteOrderModal
        order={selectedOrder}
        isDeleting={isDeleting}
        onCancel={handleCancelDelete}
        onConfirm={confirmDelete}
      />
    </section>
  );
}
