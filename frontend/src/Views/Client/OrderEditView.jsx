import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { buildErrorMessage, getOrder, submitOrderEdit_b, getAllItems, getOrderItems } from "./orderApi";
import OrderItemSelector from './OrderItemSelector'

export default function OrderEditView() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [isvykimoUostas, setIsvykimoUostas] = useState("");
  const [atvykimoUostas, setAtvykimoUostas] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [availableItems, setAvailableItems] = useState([]);
  const [orderItems, setOrderItems] = useState([]);

  useEffect(() => {
    open();
  }, []);

  async function open() {
    setFieldErrors({});
    setServerError("");

    try {
      const order = await getOrder(id);
      setIsvykimoUostas(order.isvykimoUostas || "");
      setAtvykimoUostas(order.atvykimoUostas || "");
      try {
        const prekes = await getAllItems();
        setAvailableItems(prekes);
      } catch (e) {}

      try {
        const items = await getOrderItems(id);
        // map items to selector shape { item, quantity }
        const mapped = items.map(it => ({ item: it.item || it.preke, quantity: it.quantity || it.kiekis }));
        setOrderItems(mapped);
      } catch (e) {}
    } catch (err) {
      setServerError(buildErrorMessage(err, "Failed to load order."));
    }
  }

  function validateData() {
    const errors = {};
    if (!isvykimoUostas || isvykimoUostas.toString().trim() === "") {
      errors.isvykimoUostas = "Departure port is required.";
    }
    if (!atvykimoUostas || atvykimoUostas.toString().trim() === "") {
      errors.atvykimoUostas = "Arrival port is required.";
    }
    setFieldErrors(errors);
    if (!orderItems || orderItems.length === 0) {
      setServerError('At least one order item is required.');
      return false;
    }
    return Object.keys(errors).length === 0;
  }

  async function submitOrder(e) {
    e && e.preventDefault && e.preventDefault();
    setServerError("");

    if (!validateData()) return;

    const payload = {
      isvykimoUostas: isvykimoUostas,
      atvykimoUostas: atvykimoUostas
    };
    payload.items = orderItems.map(i => ({ itemId: i.item.id, quantity: i.quantity }));

    setIsSubmitting(true);
    try {
      const updated = await submitOrderEdit_b(id, payload);
      navigate(`/orders/${id}`, { state: { message: "Order updated successfully." } });
    } catch (err) {
      setServerError(buildErrorMessage(err, "Failed to update order."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Edit order</h2>
          <p className="muted">Modify departure and arrival ports.</p>
        </div>
        <Link to={`/orders/${id}`} className="button">
          Back to detail
        </Link>
      </div>

      {serverError ? <div className="alert alert-error">{serverError}</div> : null}

      <div className="card">
        <form onSubmit={submitOrder}>
          <div>
            <label>Departure port</label>
            <input
              value={isvykimoUostas}
              onChange={(e) => setIsvykimoUostas(e.target.value)}
              type="text"
            />
            {fieldErrors.isvykimoUostas ? (
              <div className="alert alert-error">{fieldErrors.isvykimoUostas}</div>
            ) : null}
          </div>

          <div>
            <label>Arrival port</label>
            <input
              value={atvykimoUostas}
              onChange={(e) => setAtvykimoUostas(e.target.value)}
              type="text"
            />
            {fieldErrors.atvykimoUostas ? (
              <div className="alert alert-error">{fieldErrors.atvykimoUostas}</div>
            ) : null}
          </div>

          <div>
            <label>Order items</label>
            <OrderItemSelector availableItems={availableItems} selectedItems={orderItems} onChange={setOrderItems} />
          </div>

          <div style={{ marginTop: "1rem" }}>
            <button className="button" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Saving..." : "Save changes"}
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
