import React from "react";

export function EntryCard({ entry, onDelete }) {
  if (!entry) return null;

  const dateStr = new Date(entry.created_at).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });

  return (
    <div
      style={{
        backgroundColor: "#f4e4bc",
        color: "#2c1d11",
        border: "3px solid #86603a",
        borderTopColor: "#fff2d6",
        borderLeftColor: "#fff2d6",
        borderRightColor: "#4f3418",
        borderBottomColor: "#4f3418",
        padding: "16px 20px",
        boxShadow: "0 4px 12px rgba(0,0,0,0.5)",
        fontFamily: "'VT323', monospace",
        fontSize: "18px",
        display: "flex",
        flexDirection: "column",
        gap: "10px",
        position: "relative"
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
        <div>
          <h3
            style={{
              fontFamily: "'Press Start 2P', monospace",
              fontSize: "14px",
              color: "#3a2512",
              margin: 0
            }}
          >
            📜 {entry.title || "Untitled Log"}
          </h3>
          <span style={{ fontSize: "14px", color: "#6e4b2a" }}>Recorded: {dateStr}</span>
        </div>

        {onDelete && (
          <button
            onClick={() => onDelete(entry.id)}
            style={{
              background: "transparent",
              border: "none",
              color: "#a00",
              cursor: "pointer",
              fontSize: "16px",
              fontFamily: "'Press Start 2P', monospace"
            }}
            title="Burn Page"
          >
            ✖
          </button>
        )}
      </div>

      <p style={{ whiteSpace: "pre-wrap", lineHeight: "1.4", margin: "8px 0" }}>
        {entry.body}
      </p>

      {entry.tags && entry.tags.length > 0 && (
        <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "4px" }}>
          {entry.tags.map((tag, idx) => (
            <span
              key={idx}
              style={{
                fontSize: "12px",
                fontFamily: "'Press Start 2P', monospace",
                backgroundColor: "#d6c193",
                color: "#4f3418",
                padding: "2px 8px",
                border: "1px solid #86603a"
              }}
            >
              #{tag}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
