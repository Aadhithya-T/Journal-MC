/**
 * Biome provider module.
 * getBiome(world) returns the raw biome string off the current world row.
 */
export function getBiome(world) {
  return world?.biome || "plains";
}

export const BIOME_METADATA = {
  plains: {
    name: "Plains",
    color: "#5b8731",
    bgGradient: "linear-gradient(135deg, #385e19 0%, #5b8731 100%)",
    icon: "🌿",
    temperature: "0.8 (Warm)",
    mobs: ["Sheep", "Cow", "Pig", "Horse"],
    primaryBlock: "Grass Block",
    description: "Grassy plains with temperate weather, abundant livestock, and clear blue skies."
  },
  birch_forest: {
    name: "Birch Forest",
    color: "#7e9f3b",
    bgGradient: "linear-gradient(135deg, #4f6820 0%, #85aa39 100%)",
    icon: "🌳",
    temperature: "0.6 (Temperate)",
    mobs: ["Sheep", "Cow", "Wolf", "Bee"],
    primaryBlock: "Birch Wood",
    description: "A tall, bright forest filled with distinctive white-barked birch trees."
  },
  nether: {
    name: "Nether Wastes",
    color: "#852323",
    bgGradient: "linear-gradient(135deg, #4d0a0a 0%, #991c1c 100%)",
    icon: "🔥",
    temperature: "2.0 (Scorching)",
    mobs: ["Piglin", "Zombified Piglin", "Ghast", "Magma Cube"],
    primaryBlock: "Netherrack",
    description: "A dangerous infernal dimension of lava seas, netherrack, and fiery hazards."
  },
  lush_caves: {
    name: "Lush Caves",
    color: "#2e6f40",
    bgGradient: "linear-gradient(135deg, #0e3b1c 0%, #2e6f40 100%)",
    icon: "🍃",
    temperature: "0.5 (Humid)",
    mobs: ["Axolotl", "Glow Squid", "Tropical Fish"],
    primaryBlock: "Moss Block & Spore Blossom",
    description: "Underground caves blooming with moss, cave vines, clay pools, and glow berries."
  },
  deep_dark: {
    name: "Deep Dark",
    color: "#0a2228",
    bgGradient: "linear-gradient(135deg, #030d10 0%, #0d363f 100%)",
    icon: "🌌",
    temperature: "0.2 (Subterranean)",
    mobs: ["Warden"],
    primaryBlock: "Sculk & Catalyst",
    description: "Deep subterranean chambers coated in echoic sculk blocks and ancient ruins."
  },
  snowy_taiga: {
    name: "Snowy Taiga",
    color: "#4e738c",
    bgGradient: "linear-gradient(135deg, #1e3545 0%, #4e738c 100%)",
    icon: "❄️",
    temperature: "-0.5 (Freezing)",
    mobs: ["Fox", "Wolf", "Stray", "Polar Bear"],
    primaryBlock: "Snow & Spruce Wood",
    description: "Frigid pine forests covered in soft snow layers and icy rivers."
  }
};

export function getBiomeDetails(biomeKey) {
  const key = (biomeKey || "plains").toLowerCase();
  return BIOME_METADATA[key] || BIOME_METADATA.plains;
}
