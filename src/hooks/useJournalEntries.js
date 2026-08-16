import { useState, useEffect, useCallback } from "react";
import { supabase, isSupabaseConfigured } from "../lib/supabaseClient";

const STORAGE_KEY_ENTRIES = "mc_journal_entries_v1";

export function useJournalEntries(worldId) {
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchEntries = useCallback(async () => {
    if (!worldId) {
      setEntries([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    let fetched = [];

    if (isSupabaseConfigured) {
      try {
        const { data, error: dbError } = await supabase
          .from("entries")
          .select("*")
          .eq("world_id", worldId)
          .order("created_at", { ascending: false });

        if (!dbError && data) {
          fetched = data;
        }
      } catch (err) {
        console.warn("Supabase entries fetch failed, checking local storage", err);
      }
    }

    // Local storage fallback
    if (fetched.length === 0) {
      const raw = localStorage.getItem(`${STORAGE_KEY_ENTRIES}_${worldId}`);
      if (raw) {
        try {
          fetched = JSON.parse(raw);
        } catch (e) {
          console.error("Local entry parse error", e);
        }
      }
    }

    setEntries(fetched);
    setLoading(false);
  }, [worldId]);

  useEffect(() => {
    fetchEntries();
  }, [fetchEntries]);

  const addEntry = async ({ title, body, tags = [] }) => {
    if (!worldId) return null;

    const newEntryObj = {
      id: typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `e-${Date.now()}`,
      world_id: worldId,
      title: title || "Untitled Quest Log",
      body: body || "",
      tags: tags || [],
      created_at: new Date().toISOString()
    };

    let persisted = newEntryObj;

    if (isSupabaseConfigured) {
      try {
        const { data, error: insertError } = await supabase
          .from("entries")
          .insert({
            world_id: worldId,
            title: newEntryObj.title,
            body: newEntryObj.body,
            tags: newEntryObj.tags
          })
          .select()
          .single();

        if (!insertError && data) {
          persisted = data;
        }
      } catch (err) {
        console.warn("Supabase entry insert warning", err);
      }
    }

    const updated = [persisted, ...entries];
    setEntries(updated);
    localStorage.setItem(`${STORAGE_KEY_ENTRIES}_${worldId}`, JSON.stringify(updated));

    return persisted;
  };

  const deleteEntry = async (entryId) => {
    if (isSupabaseConfigured) {
      try {
        await supabase.from("entries").delete().eq("id", entryId);
      } catch (e) {
        console.warn("Supabase delete failed", e);
      }
    }

    const updated = entries.filter((e) => e.id !== entryId);
    setEntries(updated);
    localStorage.setItem(`${STORAGE_KEY_ENTRIES}_${worldId}`, JSON.stringify(updated));
  };

  return {
    entries,
    loading,
    error,
    addEntry,
    deleteEntry,
    refetch: fetchEntries
  };
}
