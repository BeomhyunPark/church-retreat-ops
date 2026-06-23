export function StatusMessage({ message, tone = "info" }: { message: string; tone?: "info" | "error" | "success" }) {
  return (
    <div aria-live="polite" className={`status-message status-message--${tone}`} role={tone === "error" ? "alert" : "status"}>
      {message}
    </div>
  );
}
