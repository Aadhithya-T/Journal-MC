import React from "react";
import { HashRouter, Routes, Route, Navigate } from "react-router-dom";
import { WorldProvider } from "./context/WorldContext";
import { BackgroundProvider } from "./context/BackgroundContext";
import { VideoBackground } from "./components/background/VideoBackground";
import { ModeSelect } from "./routes/ModeSelect";
import { WorldSetup } from "./routes/WorldSetup";
import { Journal } from "./routes/Journal";
import "./styles/theme.css";

export default function App() {
  return (
    <BackgroundProvider>
      <WorldProvider>
        <VideoBackground />
        <HashRouter>
          <Routes>
            <Route path="/" element={<ModeSelect />} />
            <Route path="/setup" element={<WorldSetup />} />
            <Route path="/journal" element={<Journal />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </HashRouter>
      </WorldProvider>
    </BackgroundProvider>
  );
}
