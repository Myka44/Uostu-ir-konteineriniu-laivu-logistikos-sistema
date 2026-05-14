import { useEffect, useState } from "react";
import { getAllShips, reportDocking, buildErrorMessage } from "../Dispatcher/shipApi";

const STATE_LABELS = {
  ARRIVED: "Received",
  DEPARTED: "Dispatched",
  AWAITING_DOCKING: "Awaiting arrival",
};
const STATE_CLASS = {
  ARRIVED: "badge-success",
  DEPARTED: "badge-warning",
  AWAITING_DOCKING: "badge-info",
};

export default function CaptainMain() {
  const [ships, setShips]           = useState([]);
  const [isLoading, setIsLoading]   = useState(true);
  const [error, setError]           = useState("");
  const [actionMsg, setActionMsg]   = useState("");
  const [actionError, setActionError] = useState("");
  const [busyId, setBusyId]         = useState(null);

  useEffect(() => { load(); }, []);

  async function load() {
    setIsLoading(true);
    try {
      // Captain sees only ships that are dispatched (at sea) or their own ships
      const all = await getAllShips();
      setShips(all);
    } catch (e) {
      setError(buildErrorMessage(e, "Failed to load ships."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleReportDocking(shipId, shipName) {
    setBusyId(shipId);
    setActionError("");
    setActionMsg("");
    try {
      await reportDocking(shipId);
      setActionMsg(`Ship "${shipName}" successfully reported arrival at port.`);
      await load();
    } catch (e) {
      setActionError(buildErrorMessage(e, "Report failed."));
    } finally {
      setBusyId(null);
    }
  }

  const atSeaShips = ships.filter((s) => s.state === "DEPARTED");
  const awaitingShips = ships.filter((s) => s.state === "AWAITING_DOCKING");
  const dockedShips = ships.filter((s) => s.state === "ARRIVED");

  return (
      <section className="stack-lg">
        <div className="page-heading">
          <div>
            <h2>Captain control panel</h2>
            <p className="muted">Report arrival at port and view ship status.</p>
          </div>
        </div>

        {error && <div className="alert alert-error">{error}</div>}
        {actionMsg && <div className="alert alert-success">{actionMsg}</div>}
        {actionError && <div className="alert alert-error">{actionError}</div>}

        {isLoading ? (
            <p>Loading...</p>
        ) : (
            <>
              {/* At sea - can report docking */}
              <div className="card">
                <h3 style={{ marginBottom: 4 }}>At sea ({atSeaShips.length})</h3>
                <p className="muted" style={{ marginBottom: 16 }}>
                  Ships that can report arrival at port.
                </p>
                {atSeaShips.length === 0 ? (
                    <p className="muted">No ships at sea at the moment.</p>
                ) : (
                    <div className="table-wrap">
                      <table>
                        <thead>
                        <tr>
                          <th>Name</th>
                          <th>Type</th>
                          <th>Fuel (l)</th>
                          <th>Status</th>
                          <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        {atSeaShips.map((ship) => (
                            <tr key={ship.id}>
                              <td><strong>{ship.name}</strong></td>
                              <td>{ship.type}</td>
                              <td>{ship.fuelAmount?.toFixed(0) ?? "—"}</td>
                              <td>
                          <span className={`badge ${STATE_CLASS[ship.state]}`}>
                            {STATE_LABELS[ship.state]}
                          </span>
                              </td>
                              <td>
                                <button
                                    className="button button-primary"
                                    disabled={busyId === ship.id}
                                    onClick={() => handleReportDocking(ship.id, ship.name)}
                                >
                                  {busyId === ship.id ? "Sending..." : "Report arrival"}
                                </button>
                              </td>
                            </tr>
                        ))}
                        </tbody>
                      </table>
                    </div>
                )}
              </div>

              {/* Awaiting docking */}
              {awaitingShips.length > 0 && (
                  <div className="card">
                    <h3 style={{ marginBottom: 4 }}>
                      Awaiting arrival ({awaitingShips.length})
                    </h3>
                    <p className="muted" style={{ marginBottom: 16 }}>
                      Arrival reported - awaiting dispatcher confirmation.
                    </p>
                    <div className="table-wrap">
                      <table>
                        <thead>
                        <tr><th>Name</th><th>Type</th><th>Status</th></tr>
                        </thead>
                        <tbody>
                        {awaitingShips.map((ship) => (
                            <tr key={ship.id}>
                              <td><strong>{ship.name}</strong></td>
                              <td>{ship.type}</td>
                              <td>
                          <span className={`badge ${STATE_CLASS[ship.state]}`}>
                            {STATE_LABELS[ship.state]}
                          </span>
                              </td>
                            </tr>
                        ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
              )}

              {/* Docked ships */}
              {dockedShips.length > 0 && (
                  <div className="card">
                    <h3 style={{ marginBottom: 4 }}>Docked ({dockedShips.length})</h3>
                    <p className="muted" style={{ marginBottom: 16 }}>Ships currently in port.</p>
                    <div className="table-wrap">
                      <table>
                        <thead>
                        <tr><th>Name</th><th>Type</th><th>Port</th><th>Status</th></tr>
                        </thead>
                        <tbody>
                        {dockedShips.map((ship) => (
                            <tr key={ship.id}>
                              <td><strong>{ship.name}</strong></td>
                              <td>{ship.type}</td>
                              <td>{ship.port?.name ?? "—"}</td>
                              <td>
                          <span className={`badge ${STATE_CLASS[ship.state]}`}>
                            {STATE_LABELS[ship.state]}
                          </span>
                              </td>
                            </tr>
                        ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
              )}
            </>
        )}
      </section>
  );
}