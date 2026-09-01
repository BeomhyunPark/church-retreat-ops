import { PublicRegisterForm } from "./PublicRegisterForm";
import { useAppIdentity } from "../../shared/identity/appIdentity";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function PublicRegisterPage() {
  const { identity, isLoading } = useAppIdentity();
  if (!isLoading && !identity.registrationOpen) {
    return (
      <section className="page-stack">
        <div className="page-heading">
          <div>
            <p className="eyebrow">Registration closed</p>
            <h1>신규 신청이 마감되었습니다</h1>
          </div>
        </div>
        <StatusMessage message="이미 신청한 참가자는 내 신청 조회·수정에서 운영 중에도 정보를 변경할 수 있습니다." />
      </section>
    );
  }
  return <PublicRegisterForm />;
}
