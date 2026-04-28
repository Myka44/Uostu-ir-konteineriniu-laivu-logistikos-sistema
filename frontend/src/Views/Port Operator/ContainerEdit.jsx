import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ContainerForm from "./ContainerForm";
import {
  buildErrorMessage,
  getContainer,
  submitContainerEdit_b
} from "./containerApi";

export default function ContainerEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [container, setContainer] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState("");

  useEffect(() => {
    openContainerEdit();
  }, [id]);

  async function openContainerEdit() {
    setIsLoading(true);
    setServerError("");

    try {
      const data = await getContainer(id);
      setContainer(data);
    } catch (err) {
      setServerError(buildErrorMessage(err, "Failed to load the container."));
    } finally {
      setIsLoading(false);
    }
  }

  async function submitContainerEdit(payload) {
    setIsSubmitting(true);
    setServerError("");

    try {
      await submitContainerEdit_b(id, payload);
      //navigate(`/containers/${id}`, {
      navigate(`/containers`, {
        state: { message: `Container #${id} updated successfully.` }
      });
    } catch (err) {
      setServerError(buildErrorMessage(err, `Failed to update the container.`));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-heading">
        <div>
          <h2>Edit container</h2>
        </div>
        <Link to="/containers" className="button">
          Back to list
        </Link>
      </div>

      {isLoading ? (
        <div className="card">
          <p>Loading container...</p>
        </div>
      ) : !container ? (
        <div className="card">
          <p>Container not found.</p>
        </div>
      ) : (
        <ContainerForm
          initialValues={container}
          onSubmit={submitContainerEdit}
          submitLabel="Save changes"
          serverError={serverError}
          isSubmitting={isSubmitting}
        />
      )}
    </section>
  );
}
