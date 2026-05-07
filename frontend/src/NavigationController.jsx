import { Link, Navigate, Route, Routes } from "react-router-dom";
import ContainerCreate from "./Views/Port Operator/ContainerCreate";
import ContainerEdit from "./Views/Port Operator/ContainerEdit";
import ContainerList from "./Views/Port Operator/ContainerList";
import ContainerView from "./Views/Port Operator/ContainerView";
import OrderListView from "./Views/Client/OrderListView";
import OrderDetailView from "./Views/Client/OrderDetailView";

function Layout({ children }) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Container CRUD</p>
          <h1>Port operator container management</h1>
        </div>
        <nav className="top-nav">
          <div>
            <p className="eyebrow">Client</p>
            <Link to="/orders" className="nav-link">
              Orders
            </Link>
          </div>

          <div>
            <p className="eyebrow">Operator</p>
            <Link to="/containers" className="nav-link">
              All containers
            </Link>
            <Link to="/containers/new" className="nav-link nav-link-primary">
              Create container
            </Link>
          </div>
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
        <Route path="/orders" element={<OrderListView />} />
        <Route path="/orders/new" element={<div>Create order - coming soon</div>} />
        <Route path="/orders/:id" element={<OrderDetailView />} />
        <Route path="/orders/:id/edit" element={<div>Edit order - coming soon</div>} />
        <Route path="*" element={<Navigate to="/containers" replace />} />
      </Routes>
    </Layout>
  );
}
