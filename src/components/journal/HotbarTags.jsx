import React from "react";

const TAG_SLOTS = [
  { id: "all", label: "All", icon: "📦", number: 1 },
  { id: "quest", label: "Quest", icon: "🛡️", number: 2 },
  { id: "building", label: "Build", icon: "🏰", number: 3 },
  { id: "mining", label: "Mining", icon: "💎", number: 4 },
  { id: "combat", label: "Combat", icon: "⚔️", number: 5 },
  { id: "exploration", label: "Explore", icon: "🧭", number: 6 },
  { id: "gathering", label: "Gather", icon: "🪵", number: 7 },
  { id: "lore", label: "Lore", icon: "📜", number: 8 },
  { id: "alchemy", label: "Brewing", icon: "🧪", number: 9 }
];

export function HotbarTags({ activeTag = "all", onSelectTag }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "8px" }}>
      <div style={{ fontSize: "11px", color: "#ffff55", textShadow: "1px 1px 0px #000" }}>
        HOTBAR FILTER
      </div>

      <div className="mc-slot-grid">
        {TAG_SLOTS.map((slot) => {
          const isActive = activeTag === slot.id;
          return (
            <div
              key={slot.id}
              className={`mc-slot ${isActive ? "active" : ""}`}
              onClick={() => onSelectTag && onSelectTag(slot.id)}
              title={`${slot.number}. ${slot.label}`}
            >
              <span style={{ fontSize: "18px" }}>{slot.icon}</span>
              <span
                style={{
                  position: "absolute",
                  top: "2px",
                  left: "4px",
                  fontSize: "9px",
                  color: "#ffff55",
                  textShadow: "1px 1px 0 #000"
                }}
              >
                {slot.number}
              </span>
            </div>
          );
        })}
      </div>
      
      {activeTag !== "all" && (
        <span style={{ fontSize: "10px", color: "#aaaaaa" }}>
          Active Filter: <strong style={{ color: "#ffff55" }}>{activeTag}</strong>
        </span>
      )}
    </div>
  );
}
