package com.mcjournal.client;

import org.joml.Matrix4f;

/**
 * High-performance View-Frustum Culling for voxel chunks.
 * Extracts the 6 frustum planes from the combined View-Projection matrix
 * and performs fast O(1) p-vertex/n-vertex AABB intersection testing.
 */
public class FrustumCuller {
    // 6 Frustum Planes (A, B, C, D) where A*x + B*y + C*z + D >= 0 is inside
    // 0: Left, 1: Right, 2: Bottom, 3: Top, 4: Near, 5: Far
    private final float[][] planes = new float[6][4];
    private final Matrix4f clipMatrix = new Matrix4f();

    public void update(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        clipMatrix.set(projectionMatrix).mul(viewMatrix);

        // Left: row 3 + row 0
        setPlane(0,
            clipMatrix.m03() + clipMatrix.m00(),
            clipMatrix.m13() + clipMatrix.m10(),
            clipMatrix.m23() + clipMatrix.m20(),
            clipMatrix.m33() + clipMatrix.m30()
        );

        // Right: row 3 - row 0
        setPlane(1,
            clipMatrix.m03() - clipMatrix.m00(),
            clipMatrix.m13() - clipMatrix.m10(),
            clipMatrix.m23() - clipMatrix.m20(),
            clipMatrix.m33() - clipMatrix.m30()
        );

        // Bottom: row 3 + row 1
        setPlane(2,
            clipMatrix.m03() + clipMatrix.m01(),
            clipMatrix.m13() + clipMatrix.m11(),
            clipMatrix.m23() + clipMatrix.m21(),
            clipMatrix.m33() + clipMatrix.m31()
        );

        // Top: row 3 - row 1
        setPlane(3,
            clipMatrix.m03() - clipMatrix.m01(),
            clipMatrix.m13() - clipMatrix.m11(),
            clipMatrix.m23() - clipMatrix.m21(),
            clipMatrix.m33() - clipMatrix.m31()
        );

        // Near: row 3 + row 2
        setPlane(4,
            clipMatrix.m03() + clipMatrix.m02(),
            clipMatrix.m13() + clipMatrix.m12(),
            clipMatrix.m23() + clipMatrix.m22(),
            clipMatrix.m33() + clipMatrix.m32()
        );

        // Far: row 3 - row 2
        setPlane(5,
            clipMatrix.m03() - clipMatrix.m02(),
            clipMatrix.m13() - clipMatrix.m12(),
            clipMatrix.m23() - clipMatrix.m22(),
            clipMatrix.m33() - clipMatrix.m32()
        );
    }

    private void setPlane(int index, float a, float b, float c, float d) {
        float len = (float) Math.sqrt(a * a + b * b + c * c);
        if (len > 0.00001f) {
            planes[index][0] = a / len;
            planes[index][1] = b / len;
            planes[index][2] = c / len;
            planes[index][3] = d / len;
        } else {
            planes[index][0] = a;
            planes[index][1] = b;
            planes[index][2] = c;
            planes[index][3] = d;
        }
    }

    public boolean isBoxInFrustum(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (int i = 0; i < 6; i++) {
            float[] p = planes[i];
            // Positive vertex selection along plane normal direction
            float px = (p[0] > 0) ? maxX : minX;
            float py = (p[1] > 0) ? maxY : minY;
            float pz = (p[2] > 0) ? maxZ : minZ;

            if (p[0] * px + p[1] * py + p[2] * pz + p[3] < 0) {
                return false; // Outside frustum
            }
        }
        return true;
    }

    public boolean isChunkInFrustum(int cx, int cz) {
        float minX = cx * 16.0f;
        float minZ = cz * 16.0f;
        float maxX = minX + 16.0f;
        float maxZ = minZ + 16.0f;
        return isBoxInFrustum(minX, 0.0f, minZ, maxX, 256.0f, maxZ);
    }
}
