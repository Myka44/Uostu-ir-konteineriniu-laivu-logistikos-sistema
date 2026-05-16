export default function ShipmentResultView({ result }) {
  if (!result || !Array.isArray(result.containers) || result.containers.length === 0) {
    return (
      <div className="card">
        <h3>Shipment result</h3>
        <p className="muted">No shipment containers were saved for this order.</p>
      </div>
    );
  }

  return (
    <section className="stack-lg">
      <div className="section-heading">
        <div>
          <h3>Shipment result</h3>
          <p className="muted">Order #{result.orderId}</p>
        </div>
      </div>

      <div className="shipment-result-grid">
        {result.containers.map((container) => (
          <article className="card shipment-container-card" key={container.containerId}>
            <div className="shipment-container-header">
              <div>
                <h4 style={{ marginBottom: 8 }}>Container #{container.containerId}</h4>
                <div className="shipment-badges">
                  <span className="badge badge-info">{container.containerType}</span>
                  {container.isHazardous ? <span className="badge badge-warning">Hazardous</span> : null}
                  {container.warningLabel ? <span className="badge badge-neutral">{container.warningLabel}</span> : null}
                </div>
              </div>
              <div className="shipment-progress-label">
                <strong>{Number(container.occupiedVolumePercent || 0).toFixed(1)}%</strong>
                <span className="muted">filled</span>
              </div>
            </div>

            <div className="shipment-progress" aria-label="Container fill percentage">
              <div
                className="shipment-progress-bar"
                style={{ width: `${Math.min(100, Math.max(0, container.occupiedVolumePercent || 0))}%` }}
              />
            </div>

            <div className="shipment-stats">
              <div className="detail-item">
                <span className="detail-label">Weight</span>
                <strong>{Number(container.currentWeight || 0).toFixed(2)} / {Number(container.maxWeight || 0).toFixed(2)} kg</strong>
              </div>
              <div className="detail-item">
                <span className="detail-label">Volume</span>
                <strong>{Number(container.currentVolume || 0).toFixed(2)} / {Number(container.maxVolume || 0).toFixed(2)} m3</strong>
              </div>
            </div>

            <div className="table-card" style={{ marginTop: 16 }}>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Item</th>
                      <th>Quantity</th>
                      <th>Weight</th>
                      <th>Volume</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(container.items || []).map((item, index) => (
                      <tr key={`${container.containerId}-${item.prekeId || index}-${index}`}>
                        <td>{item.prekePavadinimas || "-"}</td>
                        <td>{item.kiekis ?? 0}</td>
                        <td>{Number(item.svoris || 0).toFixed(2)}</td>
                        <td>{Number(item.turis || 0).toFixed(2)}</td>
                      </tr>
                    ))}
                    {(container.items || []).length === 0 ? (
                      <tr>
                        <td colSpan="4">No items assigned.</td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}