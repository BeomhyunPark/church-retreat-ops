import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getAdminRegistrations } from "./adminApi";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusMessage } from "../../shared/ui/StatusMessage";

export function AdminParticipantsPage() {
  const [searchText, setSearchText] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [feeFilter, setFeeFilter] = useState("ALL");
  const [tagFilter, setTagFilter] = useState("ALL");
  const query = useQuery({
    queryKey: ["admin", "registrations"],
    queryFn: () => getAdminRegistrations()
  });
  const participants = useMemo(() => query.data?.content ?? [], [query.data?.content]);
  const filteredParticipants = useMemo(() => {
    const keyword = searchText.trim().toLowerCase();

    return participants.filter((item) => {
      const searchableText = [
        item.name,
        item.phoneNumber,
        item.churchCellName,
        item.churchCellDepartment,
        item.middleGroupName,
        item.retreatGroupName,
        attendanceLabel(item.attendanceType),
        item.attendanceSlots.map(attendanceSlotLabel).join(" "),
        transportationLabel(item.transportationType),
        item.transportationNote
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      const matchesKeyword = keyword.length === 0 || searchableText.includes(keyword);
      const matchesStatus = statusFilter === "ALL" || item.status === statusFilter;
      const matchesFee =
        feeFilter === "ALL" || (feeFilter === "PAID" && item.feePaid) || (feeFilter === "UNPAID" && !item.feePaid);
      const matchesTag =
        tagFilter === "ALL" ||
        (tagFilter === "NEWCOMER" && item.newcomer) ||
        (tagFilter === "CARE_TARGET" && item.careTarget);

      return matchesKeyword && matchesStatus && matchesFee && matchesTag;
    });
  }, [feeFilter, participants, searchText, statusFilter, tagFilter]);

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Participants</p>
          <h1>참가자 관리</h1>
        </div>
        <span className="pill">상세 조회 시 개인정보 접근 로그가 남습니다</span>
      </div>

      {query.isError ? <StatusMessage message={query.error.message} tone="error" /> : null}

      <section className="filter-panel" aria-label="참가자 목록 필터">
        <label>
          검색
          <input
            onChange={(event) => setSearchText(event.target.value)}
            placeholder="이름, 연락처, 공동체, 조"
            type="search"
            value={searchText}
          />
        </label>
        <label>
          등록 상태
          <select onChange={(event) => setStatusFilter(event.target.value)} value={statusFilter}>
            <option value="ALL">전체</option>
            <option value="REGISTERED">등록 완료</option>
            <option value="CANCELLED">취소</option>
          </select>
        </label>
        <label>
          참가비
          <select onChange={(event) => setFeeFilter(event.target.value)} value={feeFilter}>
            <option value="ALL">전체</option>
            <option value="PAID">납부</option>
            <option value="UNPAID">미납</option>
          </select>
        </label>
        <label>
          관리 태그
          <select onChange={(event) => setTagFilter(event.target.value)} value={tagFilter}>
            <option value="ALL">전체</option>
            <option value="NEWCOMER">새가족</option>
            <option value="CARE_TARGET">돌봄</option>
          </select>
        </label>
        <div className="filter-summary">
          <span>현재 페이지</span>
          <strong>
            {filteredParticipants.length} / {participants.length}
          </strong>
        </div>
      </section>

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>이름</th>
              <th>연락처</th>
              <th>상태</th>
              <th>참가비</th>
              <th>참석</th>
              <th>이동</th>
              <th>공동체</th>
              <th>수련회 조</th>
            </tr>
          </thead>
          <tbody>
            {filteredParticipants.map((item) => (
              <tr key={item.id}>
                <td>
                  <Link className="table-link" to={`/admin/participants/${item.id}`}>
                    {item.name}
                  </Link>
                  {item.newcomer ? <span className="table-note">새가족</span> : null}
                  {item.careTarget ? <span className="table-note">돌봄</span> : null}
                </td>
                <td>{item.phoneNumber}</td>
                <td>
                  <span className={item.status === "REGISTERED" ? "status-pill status-pill--success" : "status-pill status-pill--danger"}>
                    {item.status === "REGISTERED" ? "등록 완료" : "취소"}
                  </span>
                </td>
                <td>
                  <span className={item.feePaid ? "status-pill status-pill--success" : "status-pill status-pill--warning"}>
                    {item.feePaid ? "납부" : "미납"}
                  </span>
                </td>
                <td>
                  {attendanceLabel(item.attendanceType)}
                  {item.attendanceType === "PARTIAL" ? (
                    <span className="table-note">{item.attendanceSlots.map(attendanceSlotLabel).join(", ") || "시간 미입력"}</span>
                  ) : null}
                </td>
                <td>
                  {transportationLabel(item.transportationType)}
                  {item.carpoolNeeded ? <span className="table-note">카풀 희망</span> : null}
                  {item.carpoolOffer ? <span className="table-note">카풀 가능 {item.carpoolSeats ?? 0}명</span> : null}
                </td>
                <td>{item.churchCellName ?? item.churchCellDepartment ?? "-"}</td>
                <td>{item.retreatGroupName ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {query.isLoading ? <EmptyState title="참가자 목록을 불러오는 중입니다" message="잠시만 기다려 주세요." /> : null}
        {!query.isLoading && !participants.length ? (
          <EmptyState title="등록된 참가자가 없습니다" message="공개 등록 화면에서 참가자가 등록되면 이곳에 표시됩니다." />
        ) : null}
        {!query.isLoading && participants.length > 0 && !filteredParticipants.length ? (
          <EmptyState title="조건에 맞는 참가자가 없습니다" message="검색어나 필터 조건을 조금 넓혀 보세요." />
        ) : null}
      </div>
    </section>
  );
}

function attendanceLabel(value: "FULL" | "PARTIAL") {
  return value === "FULL" ? "전체참석" : "부분참석";
}

function transportationLabel(value: "OWN_CAR" | "PUBLIC_TRANSPORT" | "UNDECIDED") {
  if (value === "OWN_CAR") return "자차";
  if (value === "PUBLIC_TRANSPORT") return "대중교통";
  return "미정";
}

function attendanceSlotLabel(value: string) {
  const labels: Record<string, string> = {
    DAY1_MORNING: "첫째날 오전",
    DAY1_AFTERNOON: "첫째날 오후",
    DAY1_GATHERING: "첫째날 집회",
    DAY2_MORNING: "둘째날 오전",
    DAY2_AFTERNOON: "둘째날 오후",
    DAY2_GATHERING: "둘째날 집회",
    DAY3_MORNING: "셋째날 오전",
    DAY3_AFTERNOON: "셋째날 오후",
    DAY3_GATHERING: "셋째날 집회"
  };

  return labels[value] ?? value;
}
