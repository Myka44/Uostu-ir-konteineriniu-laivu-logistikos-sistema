import { useEffect, useState } from "react";
import { Link, useLocation, useParams, useNavigate } from "react-router-dom";
import { buildErrorMessage, deleteOrder, getOrder, getOrderItems, getShipmentResult, runCargoAssignment } from "./orderApi";
import DeleteOrderModal from "./DeleteOrderModal";
import ShipmentResultView from "./ShipmentResultView";

export default function OrderDetailView() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [orderItems, setOrderItems] = useState([]);
  const [shipmentResult, setShipmentResult] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isAssigning, setIsAssigning] = useState(false);

  useEffect(() => {
    open();
  }, []);

  async function open() {
    setIsLoading(true);
    setError("");
    setShipmentResult(null);
    setOrderItems([]);

    try {
      const data = await getOrder(id);
      setOrder(data);
      try {
        const items = await getOrderItems(id);
        setOrderItems(Array.isArray(items) ? items : []);
      } catch (err) {
        setError(buildErrorMessage(err, "Failed to load order items."));
      }

      try {
        const shipment = await getShipmentResult(id);
        setShipmentResult(shipment);
      } catch (err) {
        if (err?.response?.status !== 404) {
          setError(buildErrorMessage(err, "Failed to load shipment result."));
        }
      }
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to load order."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleRunCargoAssignment() {
    setIsAssigning(true);
    setError("");
    try {
      const result = await runCargoAssignment(id);
      setShipmentResult(result);
      setOrder((current) => (current ? { ...current, busena: "VYKDOMA" } : current));
    } catch (err) {
      setError(buildErrorMessage(err, "Cargo assignment failed."));
    } finally {
      setIsAssigning(false);
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

            <div className="action-row" style={{ marginTop: 16 }}>
              {order.busena === "LAUKIAMA" ? (
                <>
                <Link className="button" to={`/orders/${order.id}/edit`}>
                  Edit
                </Link>
                <button className="button button-danger" onClick={clickDelete}>
                  Delete
                </button>
                </>
              ) : null}
              {/* TODO: remove debug button before final push */}
              <button className="button button-warning" onClick={handleRunCargoAssignment} disabled={isAssigning || isLoading}>
                {isAssigning ? "Running cargo assignment..." : "Run Cargo Assignment (Debug)"}
              </button>
            </div>
          </div>
        )}
      </div>

      {!isLoading && order ? (
        <div className="card table-card">
          <div style={{ padding: "16px 20px 0", borderBottom: "1px solid #e9eff6" }}>
            <h3>Order items</h3>
          </div>
          {orderItems.length === 0 ? (
            <p style={{ padding: 20 }}>No order items found.</p>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Item</th>
                    <th>Quantity</th>
                    <th>Total weight</th>
                    <th>Total volume</th>
                  </tr>
                </thead>
                <tbody>
                  {orderItems.map((item, index) => (
                    <tr key={item.id ?? `${item.item?.id ?? item.prekeId ?? "item"}-${index}`}>
                      <td>{item.item?.name || item.prekePavadinimas || "-"}</td>
                      <td>{item.quantity ?? item.kiekis ?? 0}</td>
                      <td>{Number(item.totalWeight ?? item.svoris ?? 0).toFixed(2)}</td>
                      <td>{Number(item.totalVolume ?? item.turis ?? 0).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : null}

      {shipmentResult ? <ShipmentResultView result={shipmentResult} /> : null}

      <DeleteOrderModal
        order={selectedOrder}
        isDeleting={isDeleting}
        onCancel={handleCancelDelete}
        onConfirm={confirmDelete}
      />
    </section>
  );
}
