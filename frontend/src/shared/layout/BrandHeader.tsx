import { Link } from "react-router-dom";
import { brandInitials, useAppIdentity } from "../identity/appIdentity";

export function BrandHeader() {
  const { identity, isLoading } = useAppIdentity();

  return (
    <Link className="brand-header" to="/">
      <span className="brand-mark">{isLoading ? "" : brandInitials(identity.organizationName)}</span>
      <div>
        <p className="eyebrow">{isLoading ? "불러오는 중..." : identity.organizationName}</p>
        <strong>{isLoading ? "" : identity.appName}</strong>
      </div>
    </Link>
  );
}
