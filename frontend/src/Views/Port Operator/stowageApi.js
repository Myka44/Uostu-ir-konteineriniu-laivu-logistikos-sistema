import axios from "axios";

const API_URL = "/api/stowage-plans";

export async function getStowagePlans() {
  const { data } = await axios.get(API_URL);
  return data;
}

export async function getStowagePlan(id) {
  const { data } = await axios.get(`${API_URL}/${id}`);
  return data;
}

export async function getLoadShips() {
  const { data } = await axios.get(`${API_URL}/load-ships`);
  return data;
}

export async function getPorts() {
  const { data } = await axios.get(`${API_URL}/ports`);
  return data;
}

export async function getContainersForShip(shipId) {
  const { data } = await axios.get(`${API_URL}/ships/${shipId}/containers`);
  return data;
}

export async function getAssignableContainers(shipId) {
  const { data } = await axios.get(`${API_URL}/ships/${shipId}/assignable-containers`);
  return data;
}

export async function assignContainerToShip(shipId, containerIds) {
  const { data } = await axios.post(`${API_URL}/ships/${shipId}/containers`, { containerIds });
  return data;
}

export async function unassignContainerFromShip(shipId, containerId) {
  const { data } = await axios.delete(`${API_URL}/ships/${shipId}/containers/${containerId}`);
  return data;
}

export async function submitStowageCreate(payload) {
  const { data } = await axios.post(API_URL, payload);
  return data;
}

export async function selectPlan(planId, shipId) {
  const { data } = await axios.post(`${API_URL}/${planId}/load-ship/${shipId}`);
  return data;
}

export function buildErrorMessage(error, fallback) {
  return error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback;
}
