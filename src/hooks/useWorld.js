import { useContext } from "react";
import { WorldContext } from "../context/WorldContext";

export function useWorld() {
  const context = useContext(WorldContext);
  if (!context) {
    throw new Error("useWorld must be used within a WorldProvider");
  }
  return context;
}
