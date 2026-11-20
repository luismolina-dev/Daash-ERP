import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import POSPage from './pages/POSPage';
import CajaPage from './pages/CajaPage';
import VentasPage from './pages/VentasPage';
import InventarioPage from './pages/InventarioPage';
import ComprasPage from './pages/ComprasPage';
import ClientesPage from './pages/ClientesPage';
import ReportesPage from './pages/ReportesPage';
import { useStore } from './stores/useStore';
import { landingRoute } from './lib/roles';

function LoginRoute() {
  const { currentUser } = useStore();
  return currentUser ? <Navigate to={landingRoute(currentUser.role)} replace /> : <LoginPage />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginRoute />} />
        <Route element={<Layout />}>
          <Route path="/pos" element={<POSPage />} />
          <Route path="/caja" element={<CajaPage />} />
          <Route path="/ventas" element={<VentasPage />} />
          <Route path="/inventario" element={<InventarioPage />} />
          <Route path="/compras" element={<ComprasPage />} />
          <Route path="/clientes" element={<ClientesPage />} />
          <Route path="/reportes" element={<ReportesPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
