package com.mcjournal.test;

import com.mcjournal.client.ProceduralTextureGenerator;
import com.mcjournal.client.TextureAtlas;

import java.nio.ByteBuffer;

public class StoneAndCobblestoneRedesignTest {

    public static void main(String[] args) {
        testStoneTextureAuthenticity();
        testCobblestoneTextureAuthenticity();
        System.out.println("All StoneAndCobblestoneRedesignTest assertions passed successfully!");
    }

    private static void testStoneTextureAuthenticity() {
        ByteBuffer buf = ByteBuffer.allocateDirect(TextureAtlas.ATLAS_SIZE * TextureAtlas.ATLAS_SIZE * 4);
        ProceduralTextureGenerator.generateAtlas(buf, TextureAtlas.ATLAS_GRID);

        // Stone is slot 3
        int slot = 3;
        int col = slot % TextureAtlas.ATLAS_GRID;
        int row = slot / TextureAtlas.ATLAS_GRID;
        int slotX = col * ProceduralTextureGenerator.TILE_SIZE;
        int slotY = ((TextureAtlas.ATLAS_GRID - 1) - row) * ProceduralTextureGenerator.TILE_SIZE;

        long rSum = 0, gSum = 0, bSum = 0;
        int minLuma = 255, maxLuma = 0;

        for (int y = 0; y < ProceduralTextureGenerator.TILE_SIZE; y++) {
            for (int x = 0; x < ProceduralTextureGenerator.TILE_SIZE; x++) {
                int idx = ((slotY + y) * TextureAtlas.ATLAS_SIZE + (slotX + x)) * 4;
                int r = buf.get(idx) & 0xFF;
                int g = buf.get(idx + 1) & 0xFF;
                int b = buf.get(idx + 2) & 0xFF;
                int a = buf.get(idx + 3) & 0xFF;

                assert a == 255 : "Stone must be 100% opaque, got alpha: " + a;

                rSum += r;
                gSum += g;
                bSum += b;

                int luma = (r + g + b) / 3;
                if (luma < minLuma) minLuma = luma;
                if (luma > maxLuma) maxLuma = luma;

                // Neutral gray assertion: difference between R, G, B should be within 6 (no strong tint)
                assert Math.abs(r - g) <= 6 && Math.abs(g - b) <= 6 : "Stone must be neutral gray: (" + r + "," + g + "," + b + ")";
            }
        }

        int totalPix = ProceduralTextureGenerator.TILE_SIZE * ProceduralTextureGenerator.TILE_SIZE;
        int avgR = (int)(rSum / totalPix);
        int avgG = (int)(gSum / totalPix);
        int avgB = (int)(bSum / totalPix);

        System.out.println(String.format("[Authentic Stone] Avg RGB: (%d, %d, %d) | Luma Range: [%d..%d]",
                avgR, avgG, avgB, minLuma, maxLuma));

        // Authentic Minecraft stone average is around 125 (#7d7d7d)
        assert avgR >= 118 && avgR <= 135 : "Stone average red out of expected range: " + avgR;
        assert avgG >= 118 && avgG <= 135 : "Stone average green out of expected range: " + avgG;
        assert avgB >= 120 && avgB <= 138 : "Stone average blue out of expected range: " + avgB;

        // Fissure depth to quartz highlight range
        assert minLuma <= 95 : "Stone must feature natural fissure micro-depth <= 85, got: " + minLuma;
        assert maxLuma >= 165 : "Stone must feature natural mineral highlights >= 165, got: " + maxLuma;
    }

    private static void testCobblestoneTextureAuthenticity() {
        ByteBuffer buf = ByteBuffer.allocateDirect(TextureAtlas.ATLAS_SIZE * TextureAtlas.ATLAS_SIZE * 4);
        ProceduralTextureGenerator.generateAtlas(buf, TextureAtlas.ATLAS_GRID);

        // Cobblestone is slot 4
        int slot = 4;
        int col = slot % TextureAtlas.ATLAS_GRID;
        int row = slot / TextureAtlas.ATLAS_GRID;
        int slotX = col * ProceduralTextureGenerator.TILE_SIZE;
        int slotY = ((TextureAtlas.ATLAS_GRID - 1) - row) * ProceduralTextureGenerator.TILE_SIZE;

        int darkCreviceCount = 0;
        int highlightCount = 0;
        int minLuma = 255, maxLuma = 0;

        for (int y = 0; y < ProceduralTextureGenerator.TILE_SIZE; y++) {
            for (int x = 0; x < ProceduralTextureGenerator.TILE_SIZE; x++) {
                int idx = ((slotY + y) * TextureAtlas.ATLAS_SIZE + (slotX + x)) * 4;
                int r = buf.get(idx) & 0xFF;
                int g = buf.get(idx + 1) & 0xFF;
                int b = buf.get(idx + 2) & 0xFF;
                int a = buf.get(idx + 3) & 0xFF;

                assert a == 255 : "Cobblestone must be 100% opaque, got alpha: " + a;

                int luma = (r + g + b) / 3;
                if (luma < minLuma) minLuma = luma;
                if (luma > maxLuma) maxLuma = luma;

                if (luma < 60) darkCreviceCount++;
                if (luma > 165) highlightCount++;
            }
        }

        System.out.println(String.format("[Authentic Cobblestone] Crevice Pixels: %d | Highlight Pixels: %d | Luma Range: [%d..%d]",
                darkCreviceCount, highlightCount, minLuma, maxLuma));

        // Must have deep crevice seams winding between rocks (at least 300 pixels out of 4096)
        assert darkCreviceCount >= 300 : "Cobblestone must feature deep crevice seams, found: " + darkCreviceCount;
        // Must have crisp directional rim highlights (at least 150 pixels)
        assert highlightCount >= 150 : "Cobblestone must feature 3D directional rock highlights, found: " + highlightCount;
        // Total dynamic range from shadow to highlight
        assert minLuma <= 50 : "Cobblestone shadow seam must reach <= 50, got: " + minLuma;
        assert maxLuma >= 175 : "Cobblestone sunlit rim must reach >= 175, got: " + maxLuma;
    }
}
