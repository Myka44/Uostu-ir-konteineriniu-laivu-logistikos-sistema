import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import ContainerForm from "./ContainerForm";
import { buildErrorMessage, createContainer } from "./containerApi";

export default function ContainerCreate() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function submitContainerCreate(payload) {
    setIsSubmitting(true);
    setServerError("");

    try {
      const created = await createContainer(payload);
      //navigate(`/containers/${created.id ?? ""}`.replace(/\/$/, ""), {
      navigate(`/containers`, {
        state: { message: `Container created successfully.` }
      });
    } catch (err) {
      setServerError(buildErrorMessage(err, "Failed to create the container."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Create container</h2>
          <p className="muted">Enter the data and submit.</p>
        </div>
        <Link to="/containers" className="button">
          Back to list
        </Link>
      </div>

      <ContainerForm
        initialValues={{}}
        onSubmit={submitContainerCreate}
        submitLabel="Create container"
        serverError={serverError}
        isSubmitting={isSubmitting}
      />
    </section>
  );
}
