import { Link, Navigate, Route, Routes } from "react-router-dom";
import ContainerCreate from "./Views/Port Operator/ContainerCreate";
import ContainerEdit from "./Views/Port Operator/ContainerEdit";
import ContainerList from "./Views/Port Operator/ContainerList";
import ContainerView from "./Views/Port Operator/ContainerView";

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

export default function NavigationController() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to="/containers" replace />} />
        <Route path="/containers" element={<ContainerList />} />
        <Route path="/containers/new" element={<ContainerCreate />} />
        <Route path="/containers/:id" element={<ContainerView />} />
        <Route path="/containers/:id/edit" element={<ContainerEdit />} />
        <Route path="*" element={<Navigate to="/containers" replace />} />
      </Routes>
    </Layout>
  );
}
