import React, { useState } from "react";
import { Button } from "../ui/Button";

export function BookAndQuillModal({ isOpen, onClose, onSaveEntry, initialTag = "quest", poi = null }) {
  const [title, setTitle] = useState(poi ? `Discovered ${poi.name}` : "");
  const [body, setBody] = useState(
    poi ? `Steve discovered ${poi.name} while exploring the Minecraft world!` : ""
  );
  const [tag, setTag] = useState(initialTag);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!body.trim()) return;

    onSaveEntry({
      title: title.trim() || "Steve's World Journal",
      body: body.trim(),
      tags: [tag]
    });

    setTitle("");
    setBody("");
    onClose();
  };

  return (
    <div
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: "rgba(0, 0, 0, 0.75)",
        zIndex: 9999,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "16px"
      }}
      onClick={onClose}
    >
      {/* Minecraft Book & Quill Parchment Frame */}
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          width: "100%",
          maxWidth: "480px",
          backgroundColor: "#e3c090", // Parchment leather color
          border: "8px solid #54391e",
          outline: "4px solid #000000",
          boxShadow: "0 12px 32px rgba(0,0,0,0.8)",
          padding: "24px",
          display: "flex",
          flexDirection: "column",
          gap: "16px",
          position: "relative",
          fontFamily: "'Press Start 2P', monospace"
        }}
      >
        {/* Book Header */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            borderBottom: "2px dashed #8c6239",
            paddingBottom: "12px"
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <span style={{ fontSize: "20px" }}>📖</span>
            <span style={{ fontSize: "14px", color: "#36200d", textShadow: "1px 1px 0 #fce8c5" }}>
              BOOK &amp; QUILL
            </span>
          </div>
          <button
            onClick={onClose}
            style={{
              background: "#aa0000",
              color: "#fff",
              border: "2px solid #000",
              fontSize: "12px",
              padding: "4px 8px",
              cursor: "pointer",
              fontFamily: "inherit"
            }}
          >
            ✕
          </button>
        </div>

        {poi && (
          <div
            style={{
              backgroundColor: "#d1a36a",
              padding: "8px 12px",
              border: "2px solid #8c6239",
              fontSize: "10px",
              color: "#2a1708"
            }}
          >
            📍 Near Location: <strong>{poi.name}</strong>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          {/* Title Input */}
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            <label style={{ fontSize: "10px", color: "#472911" }}>ENTRY TITLE</label>
            <input
              type="text"
              className="mc-input"
              placeholder="e.g., Steve Built First Base"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              style={{
                backgroundColor: "#fdf5e6",
                color: "#1a0d03",
                border: "2px solid #8c6239",
                fontSize: "11px",
                padding: "8px"
              }}
              autoFocus
            />
          </div>

          {/* Body TextArea */}
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            <label style={{ fontSize: "10px", color: "#472911" }}>RECORD JOURNAL NOTES</label>
            <textarea
              className="mc-input"
              rows={5}
              placeholder="Write your adventurous logs..."
              value={body}
              onChange={(e) => setBody(e.target.value)}
              style={{
                backgroundColor: "#fdf5e6",
                color: "#1a0d03",
                border: "2px solid #8c6239",
                fontSize: "11px",
                padding: "10px",
                lineHeight: "1.6",
                resize: "vertical"
              }}
            />
          </div>

          {/* Tag Selector */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <label style={{ fontSize: "10px", color: "#472911" }}>HOTBAR TAG:</label>
            <select
              value={tag}
              onChange={(e) => setTag(e.target.value)}
              style={{
                fontFamily: "'Press Start 2P', monospace",
                fontSize: "10px",
                padding: "6px 8px",
                backgroundColor: "#36200d",
                color: "#ffaa00",
                border: "2px solid #000"
              }}
            >
              <option value="quest">⚡ Quest</option>
              <option value="building">🪵 Build</option>
              <option value="mining">⛏️ Mining</option>
              <option value="combat">⚔️ Combat</option>
              <option value="exploration">🧭 Explore</option>
              <option value="gathering">🌾 Gather</option>
              <option value="lore">📜 Lore</option>
              <option value="alchemy">🧪 Brewing</option>
            </select>
          </div>

          {/* Action Buttons */}
          <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
            <Button variant="gray" type="button" onClick={onClose} style={{ fontSize: "11px", height: "36px" }}>
              Cancel
            </Button>
            <Button variant="green" type="submit" style={{ fontSize: "11px", height: "36px" }}>
              ✍️ Sign &amp; Record
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
