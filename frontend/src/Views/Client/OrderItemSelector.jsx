import React from 'react'

export default function OrderItemSelector({ availableItems, selectedItems, onChange }) {
  const [search, setSearch] = React.useState('')

  function addItem(item) {
    const existing = selectedItems.find(s => s.item.id === item.id)
    if (existing) {
      const next = selectedItems.map(s => s.item.id === item.id ? { ...s, quantity: s.quantity + 1 } : s)
      onChange(next)
    } else {
      onChange([...selectedItems, { item: item, quantity: 1 }])
    }
  }

  function updateQuantity(itemId, val) {
    const v = parseInt(val || '0', 10)
    const next = selectedItems.map(s => s.item.id === itemId ? { ...s, quantity: Math.max(0, v) } : s)
    onChange(next.filter(s => s.quantity > 0))
  }

  function remove(itemId) {
    onChange(selectedItems.filter(s => s.item.id !== itemId))
  }

  const filtered = availableItems.filter(p => (p.name || '').toLowerCase().includes(search.toLowerCase()))

  return (
    <div className="order-item-selector">
      <div style={{ marginBottom: 8 }}>
        <input placeholder="Search items..." value={search} onChange={e => setSearch(e.target.value)} style={{ width: '100%', padding: '6px' }} />
      </div>

      <div className="available-items" style={{ marginBottom: 12 }}>
        {filtered.length === 0 ? (
          <div className="muted">No items found.</div>
        ) : (
          filtered.map(p => (
            <div key={p.id} className="available-item" style={{ display: 'flex', justifyContent: 'space-between', padding: 6, borderBottom: '1px solid #eee' }}>
              <div>
                <div style={{ fontWeight: 600 }}>{p.name}</div>
                <div style={{ fontSize: 12, color: '#666' }}>Weight: {p.weight || 0} kg · Volume: {p.volume || 0} m³</div>
              </div>
                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <button type="button" className="button" onClick={() => addItem(p)}>Add</button>
              </div>
            </div>
          ))
        )}
      </div>

      <div>
        <table className="items-table">
          <thead>
            <tr><th>Item</th><th>Quantity</th><th>Weight</th><th>Volume</th><th></th></tr>
          </thead>
          <tbody>
            {selectedItems.map(s => (
              <tr key={s.item.id}>
                <td>{s.item.name}</td>
                <td><input type="number" value={s.quantity} min={1} onChange={e => updateQuantity(s.item.id, e.target.value)} /></td>
                <td>{((s.item.weight || 0) * s.quantity).toFixed(2)}</td>
                <td>{((s.item.volume || 0) * s.quantity).toFixed(3)}</td>
                <td><button type="button" onClick={() => remove(s.item.id)}>Remove</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
