export function PlaceholderPage({ title }: { title: string }) {
  return (
    <section className="panel">
      <p className="eyebrow">Coming Next</p>
      <h1>{title}</h1>
      <p className="muted">이 메뉴는 다음 화면 작업에서 운영 흐름에 맞춰 연결합니다.</p>
    </section>
  );
}
