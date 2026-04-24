export default function DeleteContainerModal({
  container,
  isDeleting,
  onCancel,
  onConfirm
}) {
  if (!container) return null;

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal card">
        <h2>Delete container</h2>
        <p>
          Are you sure you want to delete container <strong>#{container.id}</strong>?
        </p>
        <p className="muted">This action cannot be undone.</p>
        <div className="modal-actions">
          <button className="button" onClick={onCancel} disabled={isDeleting}>
            Cancel
          </button>
          <button
            className="button button-danger"
            onClick={() => onConfirm(container.id)}
            disabled={isDeleting}
          >
            {isDeleting ? "Deleting..." : "Delete"}
          </button>
        </div>
      </div>
    </div>
  );
}
