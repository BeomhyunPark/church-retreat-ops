import { useEffect, useRef, useState } from "react";
import type { IScannerControls } from "@zxing/browser";

export function AdminQrScanner({
  onDetected,
  onClose,
  pending
}: {
  onDetected: (token: string) => void;
  onClose: () => void;
  pending: boolean;
}) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const detectedRef = useRef(false);
  const onDetectedRef = useRef(onDetected);
  const [cameraError, setCameraError] = useState("");

  useEffect(() => { onDetectedRef.current = onDetected; }, [onDetected]);

  useEffect(() => {
    let controls: IScannerControls | undefined;
    let active = true;

    void import("@zxing/browser").then(({ BrowserQRCodeReader }) => {
      const reader = new BrowserQRCodeReader();
      return reader.decodeFromConstraints(
        { audio: false, video: { facingMode: { ideal: "environment" } } },
        videoRef.current ?? undefined,
        (result, _error, scannerControls) => {
          if (!result || detectedRef.current) return;
          detectedRef.current = true;
          scannerControls.stop();
          onDetectedRef.current(result.getText());
        }
      );
    }).then((scannerControls) => {
      if (!active) scannerControls.stop();
      else controls = scannerControls;
    }).catch(() => {
      if (active) setCameraError("카메라를 열 수 없습니다. 브라우저의 카메라 권한과 HTTPS 연결을 확인해 주세요.");
    });

    return () => {
      active = false;
      controls?.stop();
    };
  }, []);

  return (
    <div className="qr-scanner-panel">
      <div className="qr-scanner-panel__heading">
        <div>
          <p className="eyebrow">QR scanner</p>
          <h2>참가자 도착 QR 스캔</h2>
        </div>
        <button className="button button--ghost button--sm" onClick={onClose} type="button">닫기</button>
      </div>
      <div className="qr-scanner-viewfinder">
        <video ref={videoRef} muted playsInline />
        <span aria-hidden="true" />
      </div>
      {cameraError ? <p className="status-message status-message--error">{cameraError}</p> : null}
      <p className="muted">참가자 휴대폰의 QR을 사각형 안에 맞추면 도착 체크인이 자동 처리됩니다.</p>
      {pending ? <p className="qr-scanner-pending">체크인 처리 중...</p> : null}
    </div>
  );
}
