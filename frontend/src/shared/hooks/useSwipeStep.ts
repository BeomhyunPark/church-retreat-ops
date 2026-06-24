import { useRef, type TouchEvent } from "react";

const SWIPE_THRESHOLD_PX = 48;

export function useSwipeStep(onSwipeLeft: () => void, onSwipeRight: () => void) {
  const startX = useRef<number | null>(null);
  const startY = useRef<number | null>(null);

  function onTouchStart(event: TouchEvent) {
    startX.current = event.touches[0].clientX;
    startY.current = event.touches[0].clientY;
  }

  function onTouchEnd(event: TouchEvent) {
    if (startX.current === null || startY.current === null) {
      return;
    }

    const deltaX = event.changedTouches[0].clientX - startX.current;
    const deltaY = event.changedTouches[0].clientY - startY.current;
    startX.current = null;
    startY.current = null;

    if (Math.abs(deltaX) < SWIPE_THRESHOLD_PX || Math.abs(deltaX) < Math.abs(deltaY)) {
      return;
    }

    if (deltaX < 0) {
      onSwipeLeft();
    } else {
      onSwipeRight();
    }
  }

  return { onTouchStart, onTouchEnd };
}
