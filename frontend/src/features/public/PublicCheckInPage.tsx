export function PublicCheckInPage() {
  return (
    <section className="panel">
      <p className="eyebrow">Public</p>
      <h1>체크인</h1>
      <p className="muted">
        QR 체크인 공개 API가 추가되면 이 화면에 카메라/토큰 입력 플로우를 붙입니다. 지금은 관리자 체크인 API만
        연결되어 있습니다.
      </p>
      <div className="info-list">
        <span>현장 도착</span>
        <span>안내 데스크에서 이름 확인</span>
        <span>조 배정 및 숙소 안내 확인</span>
      </div>
    </section>
  );
}
