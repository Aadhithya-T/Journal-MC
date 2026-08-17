import React, { createContext, useState, useEffect } from "react";
import { supabase, isSupabaseConfigured } from "../lib/supabaseClient";

export const WorldContext = createContext(null);

const STORAGE_KEY_WORLDS = "mc_journal_worlds_v1";
const STORAGE_KEY_ACTIVE = "mc_journal_active_world_id_v1";

export function WorldProvider({ children }) {
  const [worlds, setWorlds] = useState([]);
  const [currentWorld, setCurrentWorld] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Initialize worlds from localStorage or Supabase
  useEffect(() => {
    async function loadWorlds() {
      setLoading(true);
      setError(null);
      let loadedWorlds = [];

      if (isSupabaseConfigured) {
        try {
          const { data, error: dbError } = await supabase
            .from("worlds")
            .select("*")
            .order("created_at", { ascending: false });

          if (!dbError && data && data.length > 0) {
            loadedWorlds = data;
          }
        } catch (err) {
          console.warn("Supabase fetch failed, falling back to localStorage", err);
        }
      }

      if (loadedWorlds.length === 0) {
        const localData = localStorage.getItem(STORAGE_KEY_WORLDS);
        if (localData) {
          try {
            loadedWorlds = JSON.parse(localData);
          } catch (e) {
            console.error("Failed to parse local worlds", e);
          }
        }
      }

      setWorlds(loadedWorlds);

      const savedActiveId = localStorage.getItem(STORAGE_KEY_ACTIVE);
      const matched = loadedWorlds.find((w) => w.id === savedActiveId);
      
      if (matched) {
        setCurrentWorld(matched);
      } else if (loadedWorlds.length > 0) {
        setCurrentWorld(loadedWorlds[0]);
        localStorage.setItem(STORAGE_KEY_ACTIVE, loadedWorlds[0].id);
      } else {
        setCurrentWorld(null);
      }

      setLoading(false);
    }

    loadWorlds();
  }, []);

  const persistLocalWorlds = (newWorlds) => {
    localStorage.setItem(STORAGE_KEY_WORLDS, JSON.stringify(newWorlds));
  };

  const createWorld = async ({ name, hardcore = false, biome = "plains" }) => {
    setLoading(true);
    setError(null);

    const newWorldObj = {
      id: typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `w-${Date.now()}`,
      name: name.trim(),
      hardcore: Boolean(hardcore),
      biome: biome || "plains",
      created_at: new Date().toISOString()
    };

    let persistedWorld = newWorldObj;

    if (isSupabaseConfigured) {
      try {
        const { data, error: insertError } = await supabase
          .from("worlds")
          .insert({
            name: newWorldObj.name,
            hardcore: newWorldObj.hardcore,
            biome: newWorldObj.biome
          })
          .select()
          .single();

        if (!insertError && data) {
          persistedWorld = data;
        }
      } catch (err) {
        console.warn("Supabase exception during world creation", err);
      }
    }

    const updatedWorlds = [persistedWorld, ...worlds];
    setWorlds(updatedWorlds);
    persistLocalWorlds(updatedWorlds);

    setCurrentWorld(persistedWorld);
    localStorage.setItem(STORAGE_KEY_ACTIVE, persistedWorld.id);
    setLoading(false);

    return persistedWorld;
  };

  const selectWorld = (worldId) => {
    const target = worlds.find((w) => w.id === worldId);
    if (target) {
      setCurrentWorld(target);
      localStorage.setItem(STORAGE_KEY_ACTIVE, target.id);
    }
  };

  const deleteWorld = async (worldId) => {
    if (isSupabaseConfigured) {
      try {
        await supabase.from("worlds").delete().eq("id", worldId);
      } catch (e) {
        console.warn("Supabase delete failed", e);
      }
    }

    const updated = worlds.filter((w) => w.id !== worldId);
    setWorlds(updated);
    persistLocalWorlds(updated);

    if (currentWorld?.id === worldId) {
      const nextWorld = updated.length > 0 ? updated[0] : null;
      setCurrentWorld(nextWorld);
      if (nextWorld) {
        localStorage.setItem(STORAGE_KEY_ACTIVE, nextWorld.id);
      } else {
        localStorage.removeItem(STORAGE_KEY_ACTIVE);
      }
    }
  };

  return (
    <WorldContext.Provider
      value={{
        currentWorld,
        worlds,
        loading,
        error,
        createWorld,
        selectWorld,
        deleteWorld
      }}
    >
      {children}
    </WorldContext.Provider>
  );
}
