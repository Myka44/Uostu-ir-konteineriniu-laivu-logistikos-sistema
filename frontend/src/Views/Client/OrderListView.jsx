import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { buildErrorMessage, getAll } from "../Port Operator/orderApi";

export default function OrderListView() {
  const location = useLocation();
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    openOrders();
  }, []);

  async function openOrders() {
    setIsLoading(true);
    setError("");

    try {
      const data = await getAll();
      setOrders(data);
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to load orders."));
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Orders</h2>
          <p className="muted">View orders placed by clients.</p>
        </div>
      </div>

      {location.state?.message ? (
        <div className="alert alert-success">{location.state.message}</div>
      ) : null}

      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="card table-card">
        {isLoading ? (
          <p>Loading orders...</p>
        ) : orders.length === 0 ? (
          <p>No orders found.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Status</th>
                  <th>Creation date</th>
                  <th>Departure port</th>
                  <th>Arrival port</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id}>
                    <td>{order.id}</td>
                    <td>{order.busena || "-"}</td>
                    <td>{order.sukurimoData || "-"}</td>
                    <td>{order.isvykimoUostas || "-"}</td>
                    <td>{order.atvykimoUostas || "-"}</td>
                    <td>
                      <div className="action-row">
                        <Link className="button button-small" to={`/orders/${order.id}`}>
                          View
                        </Link>
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
