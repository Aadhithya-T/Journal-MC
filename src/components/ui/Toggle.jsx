import React from "react";

export function Toggle({ label, checked, onChange, disabled = false, description }) {
  return (
    <div
      onClick={() => !disabled && onChange(!checked)}
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        backgroundColor: "#181818",
        border: "3px solid #373737",
        borderTopColor: "#111",
        borderLeftColor: "#111",
        borderRightColor: "#555",
        borderBottomColor: "#555",
        padding: "12px 16px",
        cursor: disabled ? "not-allowed" : "pointer",
        userSelect: "none",
        width: "100%",
        opacity: disabled ? 0.6 : 1
      }}
    >
      <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
        <span style={{ color: "#ffff55", fontSize: "14px", display: "flex", alignItems: "center", gap: "8px" }}>
          {checked && <span className="hardcore-heart">🖤</span>}
          {!checked && <span style={{ color: "#ff5555" }}>❤️</span>}
          {label}
        </span>
        {description && (
          <span style={{ color: "#aaaaaa", fontSize: "10px", lineHeight: "1.3" }}>
            {description}
          </span>
        )}
      </div>

      <div
        style={{
          width: "48px",
          height: "24px",
          backgroundColor: checked ? "#ff2222" : "#555555",
          border: "2px solid #000",
          position: "relative",
          transition: "background-color 0.2s"
        }}
      >
        <div
          style={{
            width: "20px",
            height: "20px",
            backgroundColor: "#ffffff",
            border: "2px solid #000",
            position: "absolute",
            top: "0px",
            left: checked ? "24px" : "0px",
            transition: "left 0.15s ease-in-out",
            boxShadow: "inset -2px -2px 0px #aaa"
          }}
        />
      </div>
    </div>
  );
}
