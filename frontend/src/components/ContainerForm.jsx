import { useEffect, useMemo, useState } from "react";

const TYPE_OPTIONS = [
  { value: "STANDARTINIS", label: "Standartinis" },
  { value: "SALDOMAS", label: "Šaldomas" },
  { value: "CISTERNA", label: "Cisterna" }
];

const WARNING_OPTIONS = [
  { value: "", label: "No warning label" },
  { value: "DEGUS", label: "Degus" },
  { value: "TOKSISKAS", label: "Toksiškas" },
  { value: "SPROGUS", label: "Sprogus" },
  { value: "RADIOAKTYVUS", label: "Radioaktyvus" }
];

function normalizeInitialValues(initialValues) {
  return {
    type: initialValues?.type ?? "",
    weight: initialValues?.weight ?? "",
    volume: initialValues?.volume ?? "",
    maxWeight: initialValues?.maxWeight ?? "",
    maxVolume: initialValues?.maxVolume ?? "",
    warningLabel: initialValues?.warningLabel ?? ""
  };
}

function validate(form) {
  const nextErrors = {};

  if (!form.type) nextErrors.type = "Container type is required.";

  for (const field of ["weight", "volume", "maxWeight", "maxVolume"]) {
    if (form[field] === "") {
      nextErrors[field] = "This field is required.";
      continue;
    }

    const numericValue = Number(form[field]);
    if (Number.isNaN(numericValue)) {
      nextErrors[field] = "Value must be numeric.";
    } else if (numericValue < 0) {
      nextErrors[field] = "Value cannot be negative.";
    }
  }

  if (
    form.weight !== "" &&
    form.maxWeight !== "" &&
    Number(form.weight) > Number(form.maxWeight)
  ) {
    nextErrors.weight = "Weight cannot be greater than max weight.";
  }

  if (
    form.volume !== "" &&
    form.maxVolume !== "" &&
    Number(form.volume) > Number(form.maxVolume)
  ) {
    nextErrors.volume = "Volume cannot be greater than max volume.";
  }

  return nextErrors;
}

export default function ContainerForm({
  initialValues,
  onSubmit,
  submitLabel,
  serverError,
  isSubmitting
}) {
  const normalizedInitialValues = useMemo(
    () => normalizeInitialValues(initialValues),
    [initialValues]
  );

  const [form, setForm] = useState(normalizedInitialValues);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    setForm(normalizedInitialValues);
  }, [normalizedInitialValues]);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: undefined }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationErrors = validate(form);
    setErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    await onSubmit({
      type: form.type,
      weight: Number(form.weight),
      volume: Number(form.volume),
      maxWeight: Number(form.maxWeight),
      maxVolume: Number(form.maxVolume),
      warningLabel: form.warningLabel || null
    });
  }

  return (
    <form onSubmit={handleSubmit} className="card form-card">
      <div className="section-heading">
        <h2>Container data</h2>
      </div>

      {serverError ? <div className="alert alert-error">{serverError}</div> : null}

      <div className="form-grid">
        <label>
          <span>Type</span>
          <select name="type" value={form.type} onChange={handleChange}>
            <option value="">Select type</option>
            {TYPE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          {errors.type ? <small className="field-error">{errors.type}</small> : null}
        </label>

        <label>
          <span>Warning label</span>
          <select
            name="warningLabel"
            value={form.warningLabel}
            onChange={handleChange}
          >
            {WARNING_OPTIONS.map((option) => (
              <option key={option.value || "EMPTY"} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Weight</span>
          <input
            type="number"
            step="0.01"
            name="weight"
            value={form.weight}
            onChange={handleChange}
          />
          {errors.weight ? (
            <small className="field-error">{errors.weight}</small>
          ) : null}
        </label>

        <label>
          <span>Volume</span>
          <input
            type="number"
            step="0.01"
            name="volume"
            value={form.volume}
            onChange={handleChange}
          />
          {errors.volume ? (
            <small className="field-error">{errors.volume}</small>
          ) : null}
        </label>

        <label>
          <span>Max weight</span>
          <input
            type="number"
            step="0.01"
            name="maxWeight"
            value={form.maxWeight}
            onChange={handleChange}
          />
          {errors.maxWeight ? (
            <small className="field-error">{errors.maxWeight}</small>
          ) : null}
        </label>

        <label>
          <span>Max volume</span>
          <input
            type="number"
            step="0.01"
            name="maxVolume"
            value={form.maxVolume}
            onChange={handleChange}
          />
          {errors.maxVolume ? (
            <small className="field-error">{errors.maxVolume}</small>
          ) : null}
        </label>
      </div>

      <div className="form-actions">
        <button type="submit" className="button button-primary" disabled={isSubmitting}>
          {isSubmitting ? "Saving..." : submitLabel}
        </button>
      </div>
    </form>
  );
}
