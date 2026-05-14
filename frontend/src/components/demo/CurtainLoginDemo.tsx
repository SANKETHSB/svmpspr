import { useState, useEffect, useRef } from "react";
import AuthPage from "../../pages/auth/AuthPage";

type Stage = "curtain" | "main";

export default function TheaterApp() {
  const [stage, setStage] = useState<Stage>("curtain");
  const [curtainOpen, setCurtainOpen] = useState(false);
  // Lights are auto-on once the curtain finishes opening (no manual toggle).
  const [lightsOn, setLightsOn] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [ropeHeight, setRopeHeight] = useState(180);

  const dragStartY = useRef(0);
  const currentDelta = useRef(0);
  const curtainOpened = useRef(false);

  const onRopeMouseDown = (e: React.MouseEvent) => {
    setDragging(true);
    dragStartY.current = e.clientY;
    e.preventDefault();
  };
  const onRopeTouchStart = (e: React.TouchEvent) => {
    setDragging(true);
    dragStartY.current = e.touches[0].clientY;
  };

  useEffect(() => {
    const onMove = (e: MouseEvent | TouchEvent) => {
      if (!dragging) return;
      const y =
        "touches" in e
          ? (e as TouchEvent).touches[0].clientY
          : (e as MouseEvent).clientY;
      const delta = Math.max(0, Math.min(150, y - dragStartY.current));
      currentDelta.current = delta;
      setRopeHeight(180 + delta);
    };
    const onUp = () => {
      if (!dragging) return;
      setDragging(false);
      if (currentDelta.current > 70 && !curtainOpened.current) {
        curtainOpened.current = true;
        setCurtainOpen(true);
        setTimeout(() => {
          setStage("main");
          // Auto-illuminate the stage once revealed (no on/off button).
          setTimeout(() => setLightsOn(true), 250);
        }, 1150);
      } else {
        setRopeHeight(180);
      }
      currentDelta.current = 0;
    };
    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup", onUp);
    window.addEventListener("touchmove", onMove as EventListener);
    window.addEventListener("touchend", onUp);
    return () => {
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseup", onUp);
      window.removeEventListener("touchmove", onMove as EventListener);
      window.removeEventListener("touchend", onUp);
    };
  }, [dragging]);

  return (
    <div
      style={{
        width: "100vw",
        height: "100vh",
        background: lightsOn
          ? "radial-gradient(ellipse at top, #1e1b4b 0%, #0a0b1e 60%, #050614 100%)"
          : "#050614",
        overflow: "hidden",
        position: "relative",
        fontFamily: "'Inter', 'Plus Jakarta Sans', system-ui, sans-serif",
        userSelect: "none",
        transition: "background 0.8s ease",
      }}
    >
      {/* ══ CURTAIN STAGE ══ */}
      {stage === "curtain" && (
        <div style={{ position: "absolute", inset: 0, zIndex: 10 }}>
          {/* Left curtain */}
          <div
            style={{
              position: "absolute",
              top: 0,
              left: 0,
              width: "50%",
              height: "100%",
              transform: curtainOpen ? "translateX(-101%)" : "translateX(0)",
              transition: "transform 1.15s cubic-bezier(.4,0,.15,1)",
              background:
                "linear-gradient(90deg,#050614 0%,#0a0b1e 25%,#1e1b4b 55%,#0a0b1e 80%,#050614 100%)",
              boxShadow: "inset -30px 0 80px rgba(0,0,0,.6)",
              overflow: "hidden",
            }}
          >
            {[...Array(7)].map((_, i) => (
              <div
                key={i}
                style={{
                  position: "absolute",
                  top: 0,
                  bottom: 0,
                  left: `${i * 14.5}%`,
                  width: "10%",
                  background: `linear-gradient(90deg,rgba(0,0,0,${0.45 + (i % 2) * 0.15}) 0%,rgba(129,140,248,0.10) 45%,rgba(0,0,0,${0.40 + (i % 2) * 0.12}) 100%)`,
                }}
              />
            ))}
          </div>

          {/* Right curtain */}
          <div
            style={{
              position: "absolute",
              top: 0,
              right: 0,
              width: "50%",
              height: "100%",
              transform: curtainOpen ? "translateX(101%)" : "translateX(0)",
              transition: "transform 1.15s cubic-bezier(.4,0,.15,1)",
              background:
                "linear-gradient(270deg,#050614 0%,#0a0b1e 25%,#312e81 55%,#0a0b1e 80%,#050614 100%)",
              boxShadow: "inset 30px 0 80px rgba(0,0,0,.6)",
              overflow: "hidden",
            }}
          >
            {[...Array(7)].map((_, i) => (
              <div
                key={i}
                style={{
                  position: "absolute",
                  top: 0,
                  bottom: 0,
                  left: `${i * 14.5}%`,
                  width: "10%",
                  background: `linear-gradient(90deg,rgba(0,0,0,${0.40 + (i % 2) * 0.12}) 0%,rgba(192,132,252,0.10) 45%,rgba(0,0,0,${0.45 + (i % 2) * 0.15}) 100%)`,
                }}
              />
            ))}
          </div>

          {/* Rope */}
          {!curtainOpen && (
            <div
              style={{
                position: "absolute",
                top: 0,
                right: 44,
                zIndex: 50,
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
              }}
            >
              <div
                style={{
                  width: 9,
                  height: ropeHeight,
                  background:
                    "linear-gradient(180deg,#a06818 0%,#e8b84b 40%,#c8902a 70%,#e8b84b 100%)",
                  borderRadius: 4,
                  boxShadow: "0 0 10px rgba(200,144,42,.5)",
                  transition: dragging ? "none" : "height .3s ease",
                }}
              />
              <div
                onMouseDown={onRopeMouseDown}
                onTouchStart={onRopeTouchStart}
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: "50%",
                  background:
                    "radial-gradient(circle at 38% 32%,#fde080,#c8902a 70%)",
                  cursor: dragging ? "grabbing" : "grab",
                  marginTop: -4,
                  boxShadow:
                    "0 3px 10px rgba(0,0,0,.6),inset 0 1px 3px rgba(255,220,100,.4)",
                  border: "2px solid #e0a830",
                }}
              />
            </div>
          )}

          {/* Pull hint */}
          {!curtainOpen && (
            <div
              style={{
                position: "absolute",
                bottom: 36,
                left: "50%",
                transform: "translateX(-50%)",
                color: "rgba(255,255,255,.7)",
                fontSize: 13,
                letterSpacing: 3,
                textTransform: "uppercase",
                whiteSpace: "nowrap",
                animation: "pulse 2.2s ease-in-out infinite",
              }}
            >
              Pull the rope to begin
            </div>
          )}
        </div>
      )}

      {/* ══ MAIN STAGE ══ */}
      {stage === "main" && (
        <div
          style={{
            position: "absolute",
            inset: 0,
            background: lightsOn
              ? "radial-gradient(ellipse at top, #1e1b4b 0%, #0a0b1e 60%, #050614 100%)"
              : "#050614",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            transition: "background 0.8s ease",
          }}
        >

          {/* Spotlight beam layers */}
          {lightsOn && (
            <div
              style={{
                position: "absolute",
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                pointerEvents: "none",
                zIndex: 6,
              }}
            >
              {/* Outer soft cone */}
              <div
                style={{
                  position: "absolute",
                  top: 0,
                  left: "50%",
                  transform: "translateX(-50%)",
                  width: 700,
                  height: "100%",
                  background: `conic-gradient(
                    from 270deg at 50% 0%,
                    transparent 0deg,
                    transparent 62deg,
                    rgba(255,200,60,.06) 70deg,
                    rgba(255,180,40,.12) 82deg,
                    rgba(255,180,40,.12) 98deg,
                    rgba(255,200,60,.06) 110deg,
                    transparent 118deg,
                    transparent 360deg
                  )`,
                  animation: "flicker 4s ease-in-out infinite",
                }}
              />
              {/* Inner bright beam */}
              <div
                style={{
                  position: "absolute",
                  top: 0,
                  left: "50%",
                  transform: "translateX(-50%)",
                  width: 380,
                  height: "100%",
                  background: `conic-gradient(
                    from 270deg at 50% 0%,
                    transparent 0deg,
                    transparent 72deg,
                    rgba(255,230,90,.08) 78deg,
                    rgba(255,240,120,.22) 86deg,
                    rgba(255,255,160,.28) 90deg,
                    rgba(255,240,120,.22) 94deg,
                    rgba(255,230,90,.08) 102deg,
                    transparent 108deg,
                    transparent 360deg
                  )`,
                  animation: "flicker 4s ease-in-out infinite",
                }}
              />
              {/* Dust particles shimmer */}
              <div
                style={{
                  position: "absolute",
                  top: "10%",
                  left: "50%",
                  transform: "translateX(-50%)",
                  width: 300,
                  height: "70%",
                  background:
                    "linear-gradient(180deg,rgba(255,230,100,.04) 0%,rgba(255,210,60,.02) 60%,transparent 100%)",
                  animation: "shimmer 3s ease-in-out infinite alternate",
                }}
              />
              {/* Pool of light at bottom (on card) */}
              <div
                style={{
                  position: "absolute",
                  bottom: "5%",
                  left: "50%",
                  transform: "translateX(-50%)",
                  width: 500,
                  height: 240,
                  background:
                    "radial-gradient(ellipse 55% 50% at 50% 55%,rgba(255,220,80,.2) 0%,rgba(255,180,40,.1) 45%,transparent 100%)",
                  filter: "blur(20px)",
                }}
              />
            </div>
          )}

          {/* Auth Page Container - Full Screen */}
          <div
            style={{
              position: "absolute",
              inset: 0,
              zIndex: 15,
              opacity: lightsOn ? 1 : 0,
              transform: lightsOn ? "scale(1)" : "scale(0.95)",
              transition: "opacity .55s ease, transform .55s ease",
              pointerEvents: lightsOn ? "auto" : "none",
            }}
          >
            <AuthPage />
          </div>
        </div>
      )}

      <style>{`
        @keyframes pulse { 0%,100%{opacity:.5} 50%{opacity:.95} }
        @keyframes flicker { 0%,100%{opacity:1} 45%{opacity:.88} 50%{opacity:.96} 55%{opacity:.84} 60%{opacity:1} }
        @keyframes shimmer { from{opacity:.6} to{opacity:1} }
        *{box-sizing:border-box;margin:0;padding:0;}
      `}</style>
    </div>
  );
}
