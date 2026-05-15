import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  assignContainerToShip,
  buildErrorMessage,
  getAssignableContainers,
  getContainersForShip,
  getLoadShips,
  getPorts,
  submitStowageCreate,
  unassignContainerFromShip
} from "./stowageApi";
import "./stowage.css";

export default function StowageCreate() {
  const navigate = useNavigate();
  const [ships, setShips] = useState([]);
  const [ports, setPorts] = useState([]);
  const [assignedContainers, setAssignedContainers] = useState([]);
  const [assignableContainers, setAssignableContainers] = useState([]);
  const [selectedContainerId, setSelectedContainerId] = useState("");
  const [form, setForm] = useState({ shipId: "", portId: "", stowageType: "PAKROVIMAS" });
  const [isLoading, setIsLoading] = useState(true);
  const [isContainerLoading, setIsContainerLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");

  const unassignedContainers = useMemo(
    () => assignableContainers.filter((container) => !container.ship),
    [assignableContainers]
  );

  useEffect(() => {
    open();
  }, []);

  useEffect(() => {
    if (form.shipId) {
      loadContainersForShip(form.shipId);
    } else {
      setAssignedContainers([]);
      setAssignableContainers([]);
      setSelectedContainerId("");
    }
  }, [form.shipId]);

  async function open() {
    setIsLoading(true);
    setError("");
    try {
      const [shipData, portData] = await Promise.all([getLoadShips(), getPorts()]);
      setShips(shipData);
      setPorts(portData);
      setForm((current) => ({
        ...current,
        shipId: current.shipId || String(shipData[0]?.id || ""),
        portId: current.portId || String(portData[0]?.id || "")
      }));
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to open stowage create view."));
    } finally {
      setIsLoading(false);
    }
  }

  async function loadContainersForShip(shipId) {
    setIsContainerLoading(true);
    setError("");
    try {
      const [assigned, assignable] = await Promise.all([
        getContainersForShip(shipId),
        getAssignableContainers(shipId)
      ]);
      setAssignedContainers(assigned);
      setAssignableContainers(assignable);
      setSelectedContainerId(String(assignable.find((container) => !container.ship)?.id || ""));
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to load ship containers."));
    } finally {
      setIsContainerLoading(false);
    }
  }

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function clickAssignContainer() {
    if (!form.shipId || !selectedContainerId) {
      return;
    }

    setIsContainerLoading(true);
    setError("");
    try {
      await assignContainerToShip(Number(form.shipId), [Number(selectedContainerId)]);
      await loadContainersForShip(form.shipId);
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to assign container to ship."));
    } finally {
      setIsContainerLoading(false);
    }
  }

  async function clickUnassignContainer(containerId) {
    if (!form.shipId || !containerId) {
      return;
    }

    setIsContainerLoading(true);
    setError("");
    try {
      await unassignContainerFromShip(Number(form.shipId), containerId);
      await loadContainersForShip(form.shipId);
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to remove container from ship."));
    } finally {
      setIsContainerLoading(false);
    }
  }

  async function submitStowageCreateForm(event) {
    event.preventDefault();
    setIsSubmitting(true);
    setError("");
    try {
      const result = await submitStowageCreate({
        shipId: Number(form.shipId),
        portId: Number(form.portId),
        stowageType: form.stowageType,
        containerIds: assignedContainers.map((container) => container.id)
      });
      navigate(`/stowage-plans/${result.stowagePlan.id}`, {
        state: { message: result.message || "Stowage plan created." }
      });
    } catch (err) {
      setError(buildErrorMessage(err, "Failed to create stowage plan."));
    } finally {
      setIsSubmitting(false);
    }
  }

  function renderContainerLabel(container) {
    return `#${container.id} ${container.type || "-"} | ${Number(container.weight || 0).toFixed(2)} kg | ${container.warningLabel || "no warning"}`;
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Create new stowage plan</h2>
          <p className="muted">Assign containers to the selected ship, then submitStowageCreate.</p>
        </div>
        <Link className="button" to="/stowage-plans">Back to plans</Link>
      </div>

      {error ? <div className="alert alert-error">{error}</div> : null}

      <div className="card">
        {isLoading ? (
          <p>Loading form data...</p>
        ) : (
          <form onSubmit={submitStowageCreateForm} className="stack-lg">
            <div className="form-grid">
              <label>
                Ship
                <select name="shipId" value={form.shipId} onChange={updateField} required>
                  <option value="">Select ship</option>
                  {ships.map((ship) => (
                    <option key={ship.id} value={ship.id}>{ship.name} #{ship.id}</option>
                  ))}
                </select>
              </label>
              <label>
                Port
                <select name="portId" value={form.portId} onChange={updateField} required>
                  <option value="">Select port</option>
                  {ports.map((port) => (
                    <option key={port.id} value={port.id}>{port.name} #{port.id}</option>
                  ))}
                </select>
              </label>
              <label>
                Loading type
                <select name="stowageType" value={form.stowageType} onChange={updateField}>
                  <option value="PAKROVIMAS">Pakrovimas</option>
                  <option value="ISKROVIMAS">Iškrovimas</option>
                </select>
              </label>
            </div>

            <div className="stowage-layout">
              <div className="card stack-lg">
                <div>
                  <h3>Assign container to ship</h3>
                  <p className="muted">Only unassigned containers are shown here.</p>
                </div>
                {isContainerLoading ? (
                  <p>Loading containers...</p>
                ) : unassignedContainers.length === 0 ? (
                  <p>No unassigned containers available.</p>
                ) : (
                  <div className="form-grid">
                    <label>
                      Container
                      <select
                        value={selectedContainerId}
                        onChange={(event) => setSelectedContainerId(event.target.value)}
                      >
                        {unassignedContainers.map((container) => (
                          <option key={container.id} value={container.id}>{renderContainerLabel(container)}</option>
                        ))}
                      </select>
                    </label>
                    <div className="form-actions align-end">
                      <button
                        className="button"
                        type="button"
                        disabled={!selectedContainerId || isContainerLoading}
                        onClick={clickAssignContainer}
                      >
                        Assign to ship
                      </button>
                    </div>
                  </div>
                )}
              </div>

              <div className="card stack-lg">
                <div>
                  <h3>Ship containers used for this plan</h3>
                  <p className="muted">
                    {assignedContainers.length} container(s) assigned to this ship. The generated plan uses these containers only.
                  </p>
                </div>
                {isContainerLoading ? (
                  <p>Loading containers...</p>
                ) : assignedContainers.length === 0 ? (
                  <p>No containers are assigned to this ship yet.</p>
                ) : (
                  <div className="table-wrapper">
                    <table>
                      <thead>
                        <tr>
                          <th>ID</th>
                          <th>Type</th>
                          <th>Weight</th>
                          <th>Warning</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {assignedContainers.map((container) => (
                          <tr key={container.id}>
                            <td>{container.id}</td>
                            <td>{container.type || "-"}</td>
                            <td>{Number(container.weight || 0).toFixed(2)}</td>
                            <td>{container.warningLabel || "-"}</td>
                            <td>
                              <button
                                className="button"
                                type="button"
                                disabled={isContainerLoading}
                                onClick={() => clickUnassignContainer(container.id)}
                              >
                                Remove
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>

            <div className="form-actions">
              <button
                className="button button-primary"
                disabled={isSubmitting || isContainerLoading || assignedContainers.length === 0}
                type="submit"
              >
                {isSubmitting ? "Creating..." : "Create stowage plan"}
              </button>
              <Link className="button" to="/stowage-plans">Cancel</Link>
            </div>
          </form>
        )}
      </div>
    </section>
  );
}
