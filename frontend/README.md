# Container CRUD frontend

This frontend matches the container CRUD flow from your diagrams:
- Container list (`ContainerList`)
- Container details (`ContainerView`)
- Create container (`ContainerCreate`)
- Edit container (`ContainerEdit`)
- Delete confirmation dialog
- Routing acts as the `NavigationController`

## 1. Copy into your repo

Put this folder into your project root as:

```text
frontend/
```

## 2. Install and run

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173`.

## 3. Run your backend locally

This frontend expects the backend API at:

```text
/api/containers
```

Because `vite.config.js` proxies `/api` to `http://localhost:8080`, your Spring Boot backend should run on port `8080`.

## 4. Backend payload shape

The form expects these fields:

```json
{
  "type": "STANDARTINIS",
  "weight": 1500,
  "volume": 20,
  "maxWeight": 2200,
  "maxVolume": 33,
  "warningLabel": "DEGUS"
}
```

If your Java enum values differ, change the option values in `src/components/ContainerForm.jsx`.
