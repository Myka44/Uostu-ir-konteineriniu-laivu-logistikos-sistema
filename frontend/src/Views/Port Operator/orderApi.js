import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api"
});

export function buildErrorMessage(error, fallbackMessage) {
  if (error?.response?.data?.message) return error.response.data.message;
  if (typeof error?.response?.data === "string") return error.response.data;
  if (error?.message) return error.message;
  return fallbackMessage;
}

export async function getAll() {
  const response = await api.get("/orders");
  return Array.isArray(response.data) ? response.data : [];
}

export async function getOrder(id) {
  const response = await api.get(`/orders/${id}`);
  return response.data;
}

export async function submitOrderCreate_b(payload) {
  throw new Error("Not implemented");
}

export async function submitOrderEdit_b(id, payload) {
  throw new Error("Not implemented");
}

export async function deleteOrder(id) {
  throw new Error("Not implemented");
}
