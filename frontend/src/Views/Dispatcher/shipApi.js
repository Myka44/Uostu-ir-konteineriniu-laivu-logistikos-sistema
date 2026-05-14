import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
});

export function buildErrorMessage(error, fallback) {
  if (error?.response?.data?.message) return error.response.data.message;
  if (typeof error?.response?.data === "string") return error.response.data;
  if (error?.message) return error.message;
  return fallback;
}

// ── Ships ──────────────────────────────────────────────────────────────────
export async function getAllShips() {
  const r = await api.get("/ships");
  return Array.isArray(r.data) ? r.data : [];
}

export async function getShip(id) {
  const r = await api.get(`/ships/${id}`);
  return r.data;
}

export async function registerShip(payload) {
  const r = await api.post("/ships", payload);
  return r.data;
}

export async function updateShip(id, payload) {
  const r = await api.put(`/ships/${id}`, payload);
  return r.data;
}

export async function deleteShip(id) {
  await api.delete(`/ships/${id}`);
}

export async function reportDocking(id) {
  const r = await api.post(`/ships/${id}/report-docking`);
  return r.data;
}

export async function receiveShip(id) {
  const r = await api.post(`/ships/${id}/receive`);
  return r.data;
}

export async function departShip(id) {
  const r = await api.post(`/ships/${id}/depart`);
  return r.data;
}

// ── Routes ─────────────────────────────────────────────────────────────────
export async function getAllRoutes() {
  const r = await api.get("/routes");
  return Array.isArray(r.data) ? r.data : [];
}

export async function getRoute(id) {
  const r = await api.get(`/routes/${id}`);
  return r.data;
}

export async function getRouteSegments(routeId) {
  const r = await api.get(`/routes/${routeId}/segments`);
  return Array.isArray(r.data) ? r.data : [];
}

export async function getShipRoutes(shipId) {
  const r = await api.get(`/ships/${shipId}/routes`);
  return Array.isArray(r.data) ? r.data : [];
}

export async function getActiveRoute(shipId) {
  const r = await api.get(`/ships/${shipId}/active-route`);
  return r.data;
}

export async function generateRoute(shipId, payload) {
  const r = await api.post(`/ships/${shipId}/generate-route`, payload);
  return r.data;
}

export async function recalculateRoute(routeId) {
  const r = await api.post(`/routes/${routeId}/recalculate`);
  return r.data;
}

// ── Ports ──────────────────────────────────────────────────────────────────
export async function getAllPorts() {
  const r = await api.get("/ports");
  return Array.isArray(r.data) ? r.data : [];
}
