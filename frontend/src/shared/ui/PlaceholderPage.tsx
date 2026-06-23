export function PlaceholderPage({ title }: { title: string }) {
  return (
    <section className="panel">
      <p className="eyebrow">준비 중</p>
      <h1>{title}</h1>
      <p className="muted">백엔드 API는 준비되어 있고, 이 라우트에 화면만 붙이면 됩니다.</p>
    </section>
  );
}
