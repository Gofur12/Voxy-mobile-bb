package me.cortex.voxy.client.core.rendering.util;

import org.joml.*;
import org.lwjgl.system.MemoryUtil;

/**
 * Writes JOML values to native memory without relying on JOML's Unsafe field offsets.
 * Android runtimes can lay out object fields differently from the assumptions made by
 * JOML's MemUtilUnsafe implementation.
 */
public final class JomlMemoryUtil {
    private JomlMemoryUtil() {}

    public static void putMatrix4f(Matrix4fc value, long address) {
        MemoryUtil.memPutFloat(address, value.m00());
        MemoryUtil.memPutFloat(address + 4, value.m01());
        MemoryUtil.memPutFloat(address + 8, value.m02());
        MemoryUtil.memPutFloat(address + 12, value.m03());
        MemoryUtil.memPutFloat(address + 16, value.m10());
        MemoryUtil.memPutFloat(address + 20, value.m11());
        MemoryUtil.memPutFloat(address + 24, value.m12());
        MemoryUtil.memPutFloat(address + 28, value.m13());
        MemoryUtil.memPutFloat(address + 32, value.m20());
        MemoryUtil.memPutFloat(address + 36, value.m21());
        MemoryUtil.memPutFloat(address + 40, value.m22());
        MemoryUtil.memPutFloat(address + 44, value.m23());
        MemoryUtil.memPutFloat(address + 48, value.m30());
        MemoryUtil.memPutFloat(address + 52, value.m31());
        MemoryUtil.memPutFloat(address + 56, value.m32());
        MemoryUtil.memPutFloat(address + 60, value.m33());
    }

    public static void putVector2f(Vector2fc value, long address) {
        MemoryUtil.memPutFloat(address, value.x());
        MemoryUtil.memPutFloat(address + 4, value.y());
    }

    public static void putVector3f(Vector3fc value, long address) {
        MemoryUtil.memPutFloat(address, value.x());
        MemoryUtil.memPutFloat(address + 4, value.y());
        MemoryUtil.memPutFloat(address + 8, value.z());
    }

    public static void putVector4f(Vector4fc value, long address) {
        MemoryUtil.memPutFloat(address, value.x());
        MemoryUtil.memPutFloat(address + 4, value.y());
        MemoryUtil.memPutFloat(address + 8, value.z());
        MemoryUtil.memPutFloat(address + 12, value.w());
    }

    public static void putVector2i(Vector2ic value, long address) {
        MemoryUtil.memPutInt(address, value.x());
        MemoryUtil.memPutInt(address + 4, value.y());
    }

    public static void putVector3i(Vector3ic value, long address) {
        MemoryUtil.memPutInt(address, value.x());
        MemoryUtil.memPutInt(address + 4, value.y());
        MemoryUtil.memPutInt(address + 8, value.z());
    }
}
