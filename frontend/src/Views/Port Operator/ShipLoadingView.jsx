import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { buildErrorMessage, getLoadShips, getStowagePlans, selectPlan } from "./stowageApi";

export default function ShipLoadingView() {
  const { shipId } = useParams();
  const [ship, setShip] = useState(null);
  const [plans, setPlans] = useState([]);
  const [selectedPlanId, setSelectedPlanId] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    open();
  }, [shipId]);

  async function open() {
    setIsLoading(true);
    setError("");
    try {
      const [shipData, planData] = await Promise.all([getLoadShips(), getStowagePlans()]);
      const foundShip = shipData.find((item) => String(item.id) === String(shipId));
      setShip(foundShip || null);
      setPlans(planData);
      const matchingPlan = planData.find((plan) => String(plan.ship?.id) === String(shipId) && plan.stowageType === "PAKROVIMAS");
      setSelectedPlanId(matchingPlan ? String(matchingPlan.id) : "");
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to open ship loading view."));
    } finally {
      setIsLoading(false);
    }
  }

  const matchingPlans = useMemo(() => {
    return plans.filter((plan) => String(plan.ship?.id) === String(shipId) && plan.stowageType === "PAKROVIMAS");
  }, [plans, shipId]);

  async function selectPlanForShip(event) {
    event.preventDefault();
    setIsSubmitting(true);
    setMessage("");
    setError("");
    try {
      const result = await selectPlan(Number(selectedPlanId), Number(shipId));
      if (result.success) {
        setMessage(result.message || "Plan commencement message");
      } else {
        setError(result.message || "Plan does not match the ship.");
      }
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to checkPlan."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Load ship</h2>
          <p className="muted">Select a matching stowage plan and start ship loading.</p>
        </div>
        <Link className="button" to="/load-ships">Back to ships</Link>
      </div>

      {message ? <div className="alert alert-success">{message}</div> : null}
      {error ? <div className="alert alert-error">{error}</div> : null}

      {isLoading ? <p>Loading...</p> : (
        <div className="card">
          <div className="details-grid">
            <div className="detail-item"><span className="detail-label">Ship</span><strong>{ship?.name || "-"}</strong></div>
            <div className="detail-item"><span className="detail-label">Status</span><strong>{ship?.state || "-"}</strong></div>
            <div className="detail-item"><span className="detail-label">Port</span><strong>{ship?.port?.name || "-"}</strong></div>
          </div>

          <form onSubmit={selectPlanForShip} className="stack-lg">
            <label>
              Stowage plan
              <select value={selectedPlanId} onChange={(event) => setSelectedPlanId(event.target.value)} required>
                <option value="">Select plan</option>
                {matchingPlans.map((plan) => (
                  <option key={plan.id} value={plan.id}>Plan #{plan.id} — {plan.stowageStatus}</option>
                ))}
              </select>
            </label>
            <div className="form-actions">
              <button className="button button-primary" disabled={isSubmitting || !selectedPlanId} type="submit">
                {isSubmitting ? "Checking..." : "selectPlan"}
              </button>
              <Link className="button" to="/stowage-plans/new">Create matching plan</Link>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
