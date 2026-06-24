import { Outlet } from "react-router-dom";
import { BrandHeader } from "./BrandHeader";

export function PublicLayout() {
  return (
    <div className="public-shell">
      <header className="public-header">
        <BrandHeader />
      </header>
      <main className="public-main">
        <Outlet />
      </main>
    </div>
  );
}
