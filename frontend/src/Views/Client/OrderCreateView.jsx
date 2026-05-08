import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { buildErrorMessage, submitOrderCreate_b, getAllItems } from "./orderApi";
import OrderItemSelector from './OrderItemSelector'

export default function OrderCreateView() {
  const navigate = useNavigate();
  const [isvykimoUostas, setIsvykimoUostas] = useState("");
  const [atvykimoUostas, setAtvykimoUostas] = useState("");
  const [ports, setPorts] = useState([]);
  const [availableItems, setAvailableItems] = useState([]);
  const [orderItems, setOrderItems] = useState([]);
  const [clientId, setClientId] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    open();
  }, []);

  function open() {
    setIsvykimoUostas("");
    setAtvykimoUostas("");
    setClientId("");
    setFieldErrors({});
    setServerError("");
    // fetch ports for selects
    (async () => {
      try {
        const list = await (await import("./orderApi")).getPorts();
        setPorts(list);
        try {
          const prekes = await getAllItems();
          setAvailableItems(prekes);
        } catch (e) {
          // ignore
        }
      } catch (e) {
        // ignore; ports will be empty
      }
    })();
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
      atvykimoUostas: atvykimoUostas,
      clientId: clientId ? Number(clientId) : null,
      items: orderItems.map(i => ({ itemId: i.item.id, quantity: i.quantity }))
    };

    setIsSubmitting(true);
    try {
      const created = await submitOrderCreate_b(payload);
      navigate(`/orders`, { state: { message: "Order created successfully." } });
    } catch (err) {
      setServerError(buildErrorMessage(err, "Failed to create order."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Create order</h2>
          <p className="muted">Enter order details and submit.</p>
        </div>
        <Link to="/orders" className="button">
          Back to list
        </Link>
      </div>

      {serverError ? <div className="alert alert-error">{serverError}</div> : null}

      <div className="card">
        <form onSubmit={submitOrder}>
          <div>
            <label>Departure port</label>
            <select value={isvykimoUostas} onChange={(e) => setIsvykimoUostas(e.target.value)}>
              <option value="">-- select departure --</option>
              {ports.map((p) => (
                <option key={p.id} value={p.name}>{p.name}</option>
              ))}
            </select>
            {fieldErrors.isvykimoUostas ? (
              <div className="alert alert-error">{fieldErrors.isvykimoUostas}</div>
            ) : null}
          </div>

          <div>
            <label>Arrival port</label>
            <select value={atvykimoUostas} onChange={(e) => setAtvykimoUostas(e.target.value)}>
              <option value="">-- select arrival --</option>
              {ports.map((p) => (
                <option key={p.id} value={p.name}>{p.name}</option>
              ))}
            </select>
            {fieldErrors.atvykimoUostas ? (
              <div className="alert alert-error">{fieldErrors.atvykimoUostas}</div>
            ) : null}
          </div>

          <div>
            <label>Client ID</label>
            <input
              value={clientId}
              onChange={(e) => setClientId(e.target.value)}
              type="number"
            />
          </div>

          <div>
            <label>Order items</label>
            <OrderItemSelector availableItems={availableItems} selectedItems={orderItems} onChange={setOrderItems} />
          </div>

          <div style={{ marginTop: "1rem" }}>
            <button className="button" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Creating..." : "Create order"}
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
