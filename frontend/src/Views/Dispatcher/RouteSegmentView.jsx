import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getRouteSegments, buildErrorMessage } from "./shipApi";

export default function RouteSegmentView() {
  const { id: routeId, segmentId } = useParams();
  const [segData, setSegData] = useState(null);
  const [error, setError]     = useState("");

  useEffect(() => {
    getRouteSegments(routeId)
      .then((segs) => {
        const found = segs.find((s) => String(s.segment.id) === String(segmentId));
        if (found) setSegData(found);
        else setError("Segment not found.");
      })
      .catch((e) => setError(buildErrorMessage(e, "Error loading segment.")));
  }, [routeId, segmentId]);

  if (error) return <div className="alert alert-error" style={{ margin: 20 }}>{error}</div>;
  if (!segData) return <p style={{ padding: 20 }}>Loading...</p>;

  const { segment, coordinates, fuelAnalysis } = segData;

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>
            Segment #{segment.sequenceNumber}:&nbsp;
            {segment.startPort?.name ?? "?"} → {segment.destinationPort?.name ?? "?"}
          </h2>
          <p className="muted">Route segment coordinates and fuel analysis.</p>
        </div>
        <Link className="button" to={`/routes/${routeId}`}>← Route</Link>
      </div>

      {/* Fuel analysis */}
      {fuelAnalysis && (
        <div className="card">
          <h3 style={{ marginBottom: 14 }}>Fuel analysis</h3>
          <div className="details-grid">
            <div className="detail-item">
              <span className="detail-label">Predicted consumption</span>
              <strong>{fuelAnalysis.predictedFuelConsumption?.toFixed(2)} l</strong>
            </div>
            <div className="detail-item">
              <span className="detail-label">Predicted remaining after segment</span>
              <strong>{fuelAnalysis.predictedFuelRemaining?.toFixed(2)} l</strong>
            </div>
          </div>
        </div>
      )}

      {/* Coordinates */}
      <div className="card table-card">
        <div style={{ padding: "16px 20px 0", borderBottom: "1px solid #e9eff6" }}>
          <h3>Coordinates ({coordinates.length} points)</h3>
        </div>
        {coordinates.length === 0 ? (
          <p style={{ padding: 20 }}>No coordinates found.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Latitude (lat)</th>
                  <th>Longitude (lon)</th>
                  <th>Azimuth (°)</th>
                </tr>
              </thead>
              <tbody>
                {coordinates.map((c) => (
                  <tr key={c.id}>
                    <td>{c.sequenceNumber}</td>
                    <td>{c.latitude?.toFixed(6)}</td>
                    <td>{c.longitude?.toFixed(6)}</td>
                    <td>{c.azimuth?.toFixed(1) ?? "—"}</td>
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