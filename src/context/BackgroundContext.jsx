import React, { createContext, useState, useEffect, useContext } from "react";

export const BACKGROUNDS = [
  {
    id: "aurora-night",
    name: "Aurora Night Cabin",
    src: "/backgrounds/aurora-night.mp4",
    icon: "🌌",
    tagline: "Northern lights glowing over a snowy biome cabin"
  },
  {
    id: "cozy-campfire",
    name: "Cozy Campfire",
    src: "/backgrounds/cozy-campfire.mp4",
    icon: "🔥",
    tagline: "Warm campfire crackling under the starry night sky"
  },
  {
    id: "coral-reef",
    name: "Coral Reef",
    src: "/backgrounds/coral-reef.mp4",
    icon: "🐠",
    tagline: "Lush tropical ocean biome with vibrant coral reefs"
  }
];

// Helper to pick a random background
const getRandomBackground = () => {
  const randomIndex = Math.floor(Math.random() * BACKGROUNDS.length);
  return BACKGROUNDS[randomIndex];
};

const BackgroundContext = createContext(null);

export function BackgroundProvider({ children }) {
  // Always select a random background when the site/app loads
  const [currentBackground, setCurrentBackground] = useState(() => getRandomBackground());
  const [isMuted, setIsMuted] = useState(true);

  const selectBackground = (id) => {
    const found = BACKGROUNDS.find((bg) => bg.id === id);
    if (found) {
      setCurrentBackground(found);
    }
  };

  const pickRandomBackground = () => {
    setCurrentBackground(getRandomBackground());
  };

  return (
    <BackgroundContext.Provider
      value={{
        backgrounds: BACKGROUNDS,
        currentBackground,
        selectBackground,
        pickRandomBackground,
        isMuted,
        setIsMuted
      }}
    >
      {children}
    </BackgroundContext.Provider>
  );
}

export function useBackground() {
  const context = useContext(BackgroundContext);
  if (!context) {
    throw new Error("useBackground must be used within a BackgroundProvider");
  }
  return context;
}
