import { Link, Navigate, Route, Routes } from "react-router-dom";
import ContainerCreatePage from "../pages/ContainerCreatePage";
import ContainerEditPage from "../pages/ContainerEditPage";
import ContainerListPage from "../pages/ContainerListPage";
import ContainerViewPage from "../pages/ContainerViewPage";

function Layout({ children }) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Container CRUD</p>
          <h1>Port operator container management</h1>
        </div>
        <nav className="top-nav">
          <Link to="/containers" className="nav-link">
            All containers
          </Link>
          <Link to="/containers/new" className="nav-link nav-link-primary">
            Create container
          </Link>
        </nav>
      </header>
      <main>{children}</main>
    </div>
  );
}

export default function AppRouter() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to="/containers" replace />} />
        <Route path="/containers" element={<ContainerListPage />} />
        <Route path="/containers/new" element={<ContainerCreatePage />} />
        <Route path="/containers/:id" element={<ContainerViewPage />} />
        <Route path="/containers/:id/edit" element={<ContainerEditPage />} />
        <Route path="*" element={<Navigate to="/containers" replace />} />
      </Routes>
    </Layout>
  );
}
