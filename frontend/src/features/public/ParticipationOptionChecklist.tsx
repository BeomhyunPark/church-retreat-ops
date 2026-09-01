import type { ParticipationOption } from "./publicApi";

type Props = {
  options: ParticipationOption[];
  selectedIds: number[];
  onChange: (selectedIds: number[]) => void;
  disabled?: boolean;
};

export function ParticipationOptionChecklist({ options, selectedIds, onChange, disabled = false }: Props) {
  const grouped = options.reduce((result, option) => {
    const dateOptions = result.get(option.eventDate) ?? [];
    dateOptions.push(option);
    result.set(option.eventDate, dateOptions);
    return result;
  }, new Map<string, ParticipationOption[]>());

  function toggle(optionId: number, checked: boolean) {
    const next = checked
      ? [...new Set([...selectedIds, optionId])]
      : selectedIds.filter((id) => id !== optionId);
    onChange(next);
  }

  if (options.length === 0) {
    return <p className="muted">운영진이 참석 항목을 준비 중입니다.</p>;
  }

  return (
    <div className="check-grid">
      {[...grouped.entries()].map(([eventDate, dateOptions]) => (
        <div key={eventDate}>
          <h3 className="checklist-day-label">
            {new Intl.DateTimeFormat("ko-KR", { month: "long", day: "numeric", weekday: "short" })
              .format(new Date(`${eventDate}T00:00:00`))}
          </h3>
          <div className="check-chip-row">
            {dateOptions.map((option) => (
              <label className="check-chip" key={option.id}>
                <input
                  checked={selectedIds.includes(option.id)}
                  disabled={disabled}
                  onChange={(event) => toggle(option.id, event.target.checked)}
                  type="checkbox"
                />
                <span>{option.label}{option.optionType === "MEAL" ? " · 식사" : ""}</span>
              </label>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
