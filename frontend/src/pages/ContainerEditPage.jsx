import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ContainerForm from "../components/ContainerForm";
import {
  buildErrorMessage,
  getContainer,
  updateContainer
} from "../api/containerApi";

export default function ContainerEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [container, setContainer] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState("");

  useEffect(() => {
    loadContainer();
  }, [id]);

  async function loadContainer() {
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

  async function handleSubmit(payload) {
    setIsSubmitting(true);
    setServerError("");

    try {
      await updateContainer(id, payload);
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
          onSubmit={handleSubmit}
          submitLabel="Save changes"
          serverError={serverError}
          isSubmitting={isSubmitting}
        />
      )}
    </section>
  );
}
