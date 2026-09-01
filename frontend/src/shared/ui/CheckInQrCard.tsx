import { useEffect, useState } from "react";

export function CheckInQrCard({
  token,
  expiresAt,
  participantName
}: {
  token: string;
  expiresAt: string;
  participantName: string;
}) {
  const [imageUrl, setImageUrl] = useState("");

  useEffect(() => {
    let active = true;
    void import("qrcode").then(({ default: QRCode }) =>
      QRCode.toDataURL(token, {
        width: 320,
        margin: 2,
        errorCorrectionLevel: "M",
        color: { dark: "#17211B", light: "#FFFFFF" }
      })
    ).then((url) => {
      if (active) setImageUrl(url);
    });
    return () => { active = false; };
  }, [token]);

  return (
    <section className="check-in-qr-card" aria-label="도착 체크인 QR">
      <div className="check-in-qr-card__heading">
        <div>
          <p className="eyebrow">Arrival QR</p>
          <h2>도착 체크인 QR</h2>
        </div>
        <span className="status-pill status-pill--info">입장 전용</span>
      </div>
      <p className="muted">수련회장 도착 후 운영자에게 이 QR을 보여주세요. 퇴장할 때는 사용하지 않습니다.</p>
      {imageUrl ? <img className="check-in-qr-card__image" src={imageUrl} alt={`${participantName} 도착 체크인 QR`} /> : <div className="check-in-qr-card__loading">QR 생성 중...</div>}
      <div className="check-in-qr-card__meta">
        <strong>{participantName}</strong>
        <span>2026년 8월 18일 밤 11:59까지 유효</span>
      </div>
      {imageUrl ? (
        <a className="button button--primary" href={imageUrl} download={`${participantName}-수련회-체크인-QR.png`}>
          QR 이미지 저장
        </a>
      ) : null}
      <small className="check-in-qr-card__notice">재발급하면 이전에 저장한 QR은 사용할 수 없습니다. 만료: {new Date(expiresAt).toLocaleString("ko-KR")}</small>
    </section>
  );
}
