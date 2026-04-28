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
  const response = await api.get("/containers");
  return Array.isArray(response.data) ? response.data : [];
}

export async function getContainer(id) {
  const response = await api.get(`/containers/${id}`);
  return response.data;
}

export async function submitContainerCreate_b(payload) {
  const response = await api.post("/containers", payload);
  return response.data;
}

export async function submitContainerEdit_b(id, payload) {
  const response = await api.put(`/containers/${id}`, payload);
  return response.data;
}

export async function deleteContainer(id) {
  await api.delete(`/containers/${id}`);
}

export async function getContainerTypes() {
  const response = await api.get(`/container-types`);
  return Array.isArray(response.data) ? response.data : [];
}
