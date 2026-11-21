package me.cortex.voxy.client.core;

/**
 * Small renderer-owned snapshot of fog values. Keeping this independent from
 * Sodium's package lets the renderer share one viewport contract across targets.
 */
public record VoxyFogParameters(
        float environmentalStart,
        float environmentalEnd,
        float red,
        float green,
        float blue,
        float alpha
) {
    /** Safe fallback for render hooks that run before Sodium has published fog state. */
    public static final VoxyFogParameters NO_FOG = new VoxyFogParameters(
            0.0f, Float.MAX_VALUE, 0.0f, 0.0f, 0.0f, 0.0f
    );

    public static VoxyFogParameters from(Object source) {
        if (source == null) return NO_FOG;
        if (source instanceof VoxyFogParameters fog) return fog;
        try {
            Class<?> type = source.getClass();
            return new VoxyFogParameters(
                    read(type, source, "environmentalStart"),
                    read(type, source, "environmentalEnd"),
                    read(type, source, "red"),
                    read(type, source, "green"),
                    read(type, source, "blue"),
                    read(type, source, "alpha")
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Unsupported Sodium fog parameter object: " + source.getClass().getName(), e);
        }
    }

    private static float read(Class<?> type, Object instance, String accessor) throws ReflectiveOperationException {
        Object value = type.getMethod(accessor).invoke(instance);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Fog accessor " + accessor + " did not return a number");
        }
        return number.floatValue();
    }
}
