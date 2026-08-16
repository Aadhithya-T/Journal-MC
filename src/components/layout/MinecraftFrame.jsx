import React from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useWorld } from "../../hooks/useWorld";

export function MinecraftFrame({ children, title }) {
  const { currentWorld, selectWorld, worlds } = useWorld();
  const navigate = useNavigate();
  const location = useLocation();

  const isSetupRoute = location.pathname === "/setup";
  const isModeSelect = location.pathname === "/";

  return (
    <div className="mc-dirt-background">
      <div className="mc-container">
        {/* Navigation / Header Bar */}
        {!isModeSelect && (
          <header
            style={{
              width: "100%",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "20px",
              backgroundColor: "rgba(0, 0, 0, 0.75)",
              border: "3px solid #555",
              borderTopColor: "#888",
              borderLeftColor: "#888",
              borderRightColor: "#222",
              borderBottomColor: "#222",
              padding: "10px 16px",
              gap: "12px",
              flexWrap: "wrap"
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
              <button
                onClick={() => navigate("/")}
                className="mc-button"
                style={{ padding: "8px 14px", fontSize: "15px" }}
              >
                🏠 Main Menu
              </button>

              {currentWorld && (
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <span style={{ color: "#ffff55", fontSize: "15px", textShadow: "1px 1px 0 #000" }}>
                    🌍 World: <strong>{currentWorld.name}</strong>
                  </span>
                  <span style={{ color: "#ff5555", fontSize: "14px", textShadow: "1px 1px 0 #2a0000" }} title="Hardcore Mode Enabled">
                    🖤 Hardcore
                  </span>
                </div>
              )}
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
              {currentWorld && (
                <>
                  <Link
                    to="/journal"
                    className="mc-button"
                    style={{
                      padding: "8px 14px",
                      fontSize: "15px",
                      backgroundColor: location.pathname === "/journal" ? "#388e3c" : "#737373"
                    }}
                  >
                    📖 Journal
                  </Link>

                  <Link
                    to="/journal/profile"
                    className="mc-button"
                    style={{
                      padding: "8px 14px",
                      fontSize: "15px",
                      backgroundColor: location.pathname === "/journal/profile" ? "#388e3c" : "#737373"
                    }}
                  >
                    👤 Profile
                  </Link>
                </>
              )}

              <button
                onClick={() => navigate("/setup")}
                className="mc-button"
                style={{ padding: "8px 14px", fontSize: "15px" }}
              >
                ➕ New World
              </button>
            </div>
          </header>
        )}

        {/* Title display */}
        {title && (
          <h1 className="mc-title" style={{ marginBottom: "24px" }}>
            {title}
          </h1>
        )}

        {/* Content Children */}
        <main style={{ width: "100%", display: "flex", justifyContent: "center" }}>
          {children}
        </main>
      </div>
    </div>
  );
}
