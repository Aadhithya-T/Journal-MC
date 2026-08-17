import React from "react";

export function MinecraftFrame({ children, title }) {
  return (
    <div className="mc-dirt-background" style={{ minHeight: "100vh", display: "flex", flexDirection: "column", alignItems: "center" }}>
      <div className="mc-container" style={{ minHeight: "100vh", display: "flex", flexDirection: "column", justifyContent: "space-between", padding: "20px 16px" }}>
        {/* Title display */}
        {title && (
          <div style={{ textAlign: "center", paddingTop: "20px", marginBottom: "16px" }}>
            <h1
              className="mc-title"
              style={{
                fontSize: "18px",
                color: "#ffffff",
                textShadow: "2px 2px 0 #000000",
                margin: 0,
                letterSpacing: "1px"
              }}
            >
              {title}
            </h1>
          </div>
        )}

        {/* Content Children */}
        <main style={{ width: "100%", flex: 1, display: "flex", justifyContent: "center", alignItems: "center" }}>
          {children}
        </main>
      </div>
    </div>
  );
}
