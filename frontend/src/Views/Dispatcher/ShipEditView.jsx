import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getShip, updateShip, getAllPorts, buildErrorMessage } from "./shipApi";

const SHIP_TYPES = ["KLAIPEDA", "SMETONA", "LDK", "KAUNAS", "VALDAS", "KARALIUS_MINDAUGAS", "VYTAUTAS"];
const COUNTRIES = ["LIETUVA", "MAZOJI_LIETUVA", "NAUJOJI_LIETUVA", "VALDIJA"];

export default function ShipEditView() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState(null);
  const [ports, setPorts] = useState([]);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    Promise.all([getShip(id), getAllPorts()])
      .then(([ship, portList]) => {
        setForm({
          name: ship.name || "",
          type: ship.type || "LDK",
              country: ship.country || "LIETUVA",
          weight: ship.weight ?? "",
          capacity: ship.capacity ?? "",
          baseFuelConsumption: ship.baseFuelConsumption ?? "",
          fuelAmount: ship.fuelAmount ?? "",
          length: ship.length ?? "",
          width: ship.width ?? "",
          height: ship.height ?? "",
          portId: ship.port?.id ?? "",
          state: ship.state,
        });
        setPorts(portList);
      })
      .catch((e) => setApiError(buildErrorMessage(e, "Failed to load ship data.")));
  }, [id]);

  function set(field, value) {
    setForm((p) => ({ ...p, [field]: value }));
    setErrors((p) => ({ ...p, [field]: "" }));
  }

  function validate() {
    const e = {};
    if (!form.name.trim()) e.name = "Name is required";
    if (!form.weight || isNaN(form.weight) || +form.weight <= 0) e.weight = "Must be positive";
    if (!form.capacity || isNaN(form.capacity) || +form.capacity < 1) e.capacity = "Minimum 1";
    if (!form.baseFuelConsumption || isNaN(form.baseFuelConsumption) || +form.baseFuelConsumption <= 0) e.baseFuelConsumption = "Must be positive";
    if (form.fuelAmount === "" || isNaN(form.fuelAmount) || +form.fuelAmount < 0) e.fuelAmount = "Cannot be negative";
    if (!form.length || +form.length <= 0) e.length = "Must be positive";
    if (!form.width || +form.width <= 0) e.width = "Must be positive";
    if (!form.height || +form.height <= 0) e.height = "Must be positive";
    return e;
  }

  async function submitData(e) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }
    setSaving(true);
    setApiError("");
    try {
      await updateShip(id, {
        name: form.name.trim(), type: form.type, country: form.country,
        weight: +form.weight, capacity: +form.capacity,
        baseFuelConsumption: +form.baseFuelConsumption, fuelAmount: +form.fuelAmount,
        length: +form.length, width: +form.width, height: +form.height, state: form.state,
        ...(form.portId ? { port: { id: +form.portId } } : {}),
      });
      navigate("/ships", { state: { message: `Ship "${form.name}" updated.` } });
    } catch (err) {
      setApiError(buildErrorMessage(err, "Update failed."));
    } finally {
      setSaving(false);
    }
  }

  if (!form) return <p style={{ padding: 20 }}>{apiError || "Loading..."}</p>;

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Edit ship data</h2>
          <p className="muted">Modify fields and save changes.</p>
        </div>
        <Link className="button" to={`/ships/${id}`}>← Back</Link>
      </div>

      {apiError && <div className="alert alert-error">{apiError}</div>}

      <form className="card" onSubmit={submitData} noValidate>
        <div className="form-grid">
          {[
            { f: "name", l: "Name *" },
          ].map(({ f, l }) => (
            <label key={f}>
              <span>{l}</span>
              <input value={form[f]} onChange={(e) => set(f, e.target.value)} />
              {errors[f] && <span className="field-error">{errors[f]}</span>}
            </label>
          ))}
          <label>
            <span>Type</span>
            <select value={form.type} onChange={(e) => set("type", e.target.value)}>
              {SHIP_TYPES.map((t) => <option key={t}>{t}</option>)}
            </select>
          </label>
          <label>
            <span>Country</span>
            <select value={form.country} onChange={(e) => set("country", e.target.value)}>
              {COUNTRIES.map((c) => <option key={c}>{c}</option>)}
            </select>
          </label>
          {[
            { f: "weight", l: "Weight (t) *", step: "0.1" },
            { f: "capacity", l: "Capacity *" },
            { f: "baseFuelConsumption", l: "Fuel consumption (l/km) *", step: "0.01" },
            { f: "fuelAmount", l: "Fuel amount (l) *" },
            { f: "length", l: "Length (m) *" },
            { f: "width", l: "Width (m) *" },
            { f: "height", l: "Height (m) *" },
          ].map(({ f, l, step }) => (
            <label key={f}>
              <span>{l}</span>
              <input type="number" step={step || "1"} value={form[f]} onChange={(e) => set(f, e.target.value)} />
              {errors[f] && <span className="field-error">{errors[f]}</span>}
            </label>
          ))}
          <label>
            <span>Port</span>
            <select value={form.portId} onChange={(e) => set("portId", e.target.value)}>
              <option value="">— not assigned —</option>
              {ports.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </label>
        </div>
        <div className="form-actions" style={{ marginTop: 24 }}>
          <button className="button button-primary" type="submit" disabled={saving}>
            {saving ? "Saving..." : "Save"}
          </button>
          <Link className="button" to={`/ships/${id}`}>Cancel</Link>
        </div>
      </form>
    </section>
  );
}