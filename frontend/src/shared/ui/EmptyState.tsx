export function EmptyState({ title, message }: { title: string; message: string }) {
  return (
    <div className="empty-state">
      <span className="empty-state__icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <path d="M5 8.5 12 5l7 3.5v8L12 20l-7-3.5v-8Z" />
          <path d="m5 8.5 7 3.5 7-3.5M12 12v8" />
        </svg>
      </span>
      <div>
        <strong>{title}</strong>
        <p>{message}</p>
      </div>
    </div>
  );
}
