export function PublicCheckInPage() {
  return (
    <section className="panel">
      <div className="section-heading">
        <p className="eyebrow">Arrival</p>
        <h1>현장 체크인</h1>
        <p className="muted">도착 후 안내 데스크에서 등록 정보를 확인하고 체크인을 진행합니다.</p>
      </div>

      <div className="arrival-card">
        <strong>도착하면 안내 데스크로 와 주세요</strong>
        <p>이름과 등록 정보를 확인한 뒤 조 배정과 숙소 안내를 받을 수 있습니다.</p>
      </div>

      <ol className="info-list">
        <li>
          <span>1</span>
          <p>안내 데스크에서 이름을 알려 주세요.</p>
        </li>
        <li>
          <span>2</span>
          <p>참가비와 등록 상태를 확인합니다.</p>
        </li>
        <li>
          <span>3</span>
          <p>조 배정과 숙소 안내를 받습니다.</p>
        </li>
      </ol>

      <div className="checkin-note">
        <strong>미리 준비하면 좋아요</strong>
        <p>등록 조회 키를 가지고 있으면 확인이 더 빠릅니다. 조회 키를 잊은 경우에도 안내 데스크에서 이름과 전화번호로 확인할 수 있습니다.</p>
      </div>
    </section>
  );
}
