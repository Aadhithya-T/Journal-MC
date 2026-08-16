import React from "react";

// Web Audio API click sound generator
function playMcClickSound() {
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    
    osc.type = "square";
    osc.frequency.setValueAtTime(440, ctx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(120, ctx.currentTime + 0.05);

    gain.gain.setValueAtTime(0.15, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.05);

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.start();
    osc.stop(ctx.currentTime + 0.05);
  } catch (e) {
    // Ignore audio error
  }
}

export function Button({
  children,
  onClick,
  variant = "default",
  disabled = false,
  tooltip,
  className = "",
  type = "button",
  ...props
}) {
  const handleClick = (e) => {
    if (disabled) {
      e.preventDefault();
      return;
    }
    playMcClickSound();
    if (onClick) {
      onClick(e);
    }
  };

  let variantClass = "";
  if (variant === "green") variantClass = "mc-button-green";

  const buttonElement = (
    <button
      type={type}
      className={`mc-button ${variantClass} ${className}`}
      onClick={handleClick}
      disabled={disabled}
      {...props}
    >
      {children}
    </button>
  );

  if (tooltip) {
    return (
      <div className="tooltip-wrapper">
        {buttonElement}
        <div className="tooltip-box">{tooltip}</div>
      </div>
    );
  }

  return buttonElement;
}
