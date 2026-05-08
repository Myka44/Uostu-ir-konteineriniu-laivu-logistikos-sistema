export default function DeleteOrderModal({ order, isDeleting, onCancel, onConfirm }) {
  if (!order) return null;

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal card">
        <h2>Cancel order</h2>
        <p>
          Are you sure you want to cancel order <strong>#{order.id}</strong>?
        </p>
        <p className="muted">This will mark the order as cancelled.</p>
        <div className="modal-actions">
          <button className="button" onClick={onCancel} disabled={isDeleting}>
            Cancel
          </button>
          <button
            className="button button-danger"
            onClick={() => onConfirm(order.id)}
            disabled={isDeleting}
          >
            {isDeleting ? "Deleting..." : "Delete"}
          </button>
        </div>
      </div>
    </div>
  );
}
