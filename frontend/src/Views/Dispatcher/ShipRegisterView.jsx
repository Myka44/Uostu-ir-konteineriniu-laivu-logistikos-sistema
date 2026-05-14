import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { registerShip, getAllPorts, buildErrorMessage } from "./shipApi";
import { useEffect } from "react";

const SHIP_TYPES = ["KLAIPEDA", "SMETONA", "LDK", "KAUNAS", "VALDAS", "KARALIUS_MINDAUGAS", "VYTAUTAS"];
const COUNTRIES = ["LIETUVA", "MAZOJI_LIETUVA", "NAUJOJI_LIETUVA", "VALDIJA"];

const EMPTY = {
  name: "", type: "LDK", country: "LIETUVA",
  weight: "", capacity: "", baseFuelConsumption: "", fuelAmount: "",
  length: "", width: "", height: "", portId: "", state: "ARRIVED",
};

export default function ShipRegisterView() {
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [ports, setPorts] = useState([]);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getAllPorts().then(setPorts).catch(() => {});
  }, []);

  function set(field, value) {
    setForm((p) => ({ ...p, [field]: value }));
    setErrors((p) => ({ ...p, [field]: "" }));
  }

  function validate() {
    const e = {};
    if (!form.name.trim()) e.name = "Name is required";
    if (!form.weight || isNaN(form.weight) || +form.weight <= 0) e.weight = "Must be a positive number";
    if (!form.capacity || isNaN(form.capacity) || +form.capacity < 1) e.capacity = "Minimum 1";
    if (!form.baseFuelConsumption || isNaN(form.baseFuelConsumption) || +form.baseFuelConsumption <= 0) e.baseFuelConsumption = "Must be positive";
    if (form.fuelAmount === "" || isNaN(form.fuelAmount) || +form.fuelAmount < 0) e.fuelAmount = "Cannot be negative";
    if (!form.length || isNaN(form.length) || +form.length <= 0) e.length = "Must be positive";
    if (!form.width || isNaN(form.width) || +form.width <= 0) e.width = "Must be positive";
    if (!form.height || isNaN(form.height) || +form.height <= 0) e.height = "Must be positive";
    return e;
  }

  async function submitData(e) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }
    setSaving(true);
    setApiError("");
    try {
      const payload = {
        name: form.name.trim(),
        type: form.type,
        country: form.country,
        weight: +form.weight,
        capacity: +form.capacity,
        baseFuelConsumption: +form.baseFuelConsumption,
        fuelAmount: +form.fuelAmount,
        length: +form.length,
        width: +form.width,
        height: +form.height,
        state: "ARRIVED",
        ...(form.portId ? { port: { id: +form.portId } } : {}),
      };
      await registerShip(payload);
      navigate("/ships", { state: { message: `Ship "${form.name}" successfully registered.` } });
    } catch (err) {
      setApiError(buildErrorMessage(err, "Registration failed."));
    } finally {
      setSaving(false);
    }
  }

  return (
      <section className="stack-lg">
        <div className="page-heading">
          <div>
            <h2>Register ship</h2>
            <p className="muted">Fill in the form fields and save.</p>
          </div>
          <Link className="button" to="/ships">← Back</Link>
        </div>

        {apiError && <div className="alert alert-error">{apiError}</div>}

        <form className="card" onSubmit={submitData} noValidate>
          <div className="form-grid">
            <Field label="Name *" error={errors.name}>
              <input value={form.name} onChange={(e) => set("name", e.target.value)} />
            </Field>
            <Field label="Type *">
              <select value={form.type} onChange={(e) => set("type", e.target.value)}>
                {SHIP_TYPES.map((t) => <option key={t}>{t}</option>)}
              </select>
            </Field>
            <Field label="Country *">
              <select value={form.country} onChange={(e) => set("country", e.target.value)}>
                {COUNTRIES.map((c) => <option key={c}>{c}</option>)}
              </select>
            </Field>
            <Field label="Weight (t) *" error={errors.weight}>
              <input type="number" step="0.1" value={form.weight} onChange={(e) => set("weight", e.target.value)} />
            </Field>
            <Field label="Capacity (containers) *" error={errors.capacity}>
              <input type="number" value={form.capacity} onChange={(e) => set("capacity", e.target.value)} />
            </Field>
            <Field label="Base fuel consumption (l/km) *" error={errors.baseFuelConsumption}>
              <input type="number" step="0.01" value={form.baseFuelConsumption} onChange={(e) => set("baseFuelConsumption", e.target.value)} />
            </Field>
            <Field label="Fuel amount (l) *" error={errors.fuelAmount}>
              <input type="number" step="1" value={form.fuelAmount} onChange={(e) => set("fuelAmount", e.target.value)} />
            </Field>
            <Field label="Length (m) *" error={errors.length}>
              <input type="number" value={form.length} onChange={(e) => set("length", e.target.value)} />
            </Field>
            <Field label="Width (m) *" error={errors.width}>
              <input type="number" value={form.width} onChange={(e) => set("width", e.target.value)} />
            </Field>
            <Field label="Height (m) *" error={errors.height}>
              <input type="number" value={form.height} onChange={(e) => set("height", e.target.value)} />
            </Field>
            <Field label="Assign port">
              <select value={form.portId} onChange={(e) => set("portId", e.target.value)}>
                <option value="">— not assigned —</option>
                {ports.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
              </select>
            </Field>
          </div>
          <div className="form-actions" style={{ marginTop: 24 }}>
            <button className="button button-primary" type="submit" disabled={saving}>
              {saving ? "Saving..." : "Register"}
            </button>
            <Link className="button" to="/ships">Cancel</Link>
          </div>
        </form>
      </section>
  );
}

function Field({ label, error, children }) {
  return (
      <label>
        <span>{label}</span>
        {children}
        {error && <span className="field-error">{error}</span>}
      </label>
  );
}