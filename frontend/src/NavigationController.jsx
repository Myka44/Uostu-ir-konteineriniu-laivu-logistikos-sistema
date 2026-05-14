import { Link, Navigate, Route, Routes } from "react-router-dom";
import ContainerCreate from "./Views/Port Operator/ContainerCreate";
import ContainerEdit from "./Views/Port Operator/ContainerEdit";
import ContainerList from "./Views/Port Operator/ContainerList";
import ContainerView from "./Views/Port Operator/ContainerView";
import OrderListView from "./Views/Client/OrderListView";
import OrderDetailView from "./Views/Client/OrderDetailView";
import OrderCreateView from "./Views/Client/OrderCreateView";
import OrderEditView from "./Views/Client/OrderEditView";
import ShipListView from "./Views/Dispatcher/ShipListView";
import ShipRegisterView from "./Views/Dispatcher/ShipRegisterView";
import ShipView from "./Views/Dispatcher/ShipView";
import ShipEditView from "./Views/Dispatcher/ShipEditView";
import RouteListView from "./Views/Dispatcher/RouteListView";
import RouteView from "./Views/Dispatcher/RouteView";
import RouteSegmentView from "./Views/Dispatcher/RouteSegmentView";
import CaptainMain from "./Views/ShipManager/CaptainMain";

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
            <Link to="/orders/new" className="nav-link nav-link-primary">
              Create order
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

          <div>
            <p className="eyebrow">Dispatcher</p>
            <Link to="/ships" className="nav-link">
              All Ships
            </Link>
            <Link to="/ships/new" className="nav-link nav-link-primary">
              Register Ship
            </Link>
            <Link to="/routes" className="nav-link">
              All Routes
            </Link>
          </div>

          <div>
            <p className="eyebrow">Ship Manager</p>
            <Link to="/captain" className="nav-link">
              Captain Dashboard
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
        <Route path="/orders/new" element={<OrderCreateView />} />
        <Route path="/orders/:id" element={<OrderDetailView />} />
        <Route path="/orders/:id/edit" element={<OrderEditView />} />
        <Route path="/ships" element={<ShipListView />} />
        <Route path="/ships/new" element={<ShipRegisterView />} />
        <Route path="/ships/:id" element={<ShipView />} />
        <Route path="/ships/:id/edit" element={<ShipEditView />} />
        <Route path="/routes" element={<RouteListView />} />
        <Route path="/routes/:id" element={<RouteView />} />
        <Route path="/routes/:id/segments/:segmentId" element={<RouteSegmentView />} />
        <Route path="/captain" element={<CaptainMain />} />
        <Route path="*" element={<Navigate to="/containers" replace />} />
      </Routes>
    </Layout>
  );
}
