package org.peyto.common.processor.utils;

public class CheckerUtils {

    private CheckerUtils() {
    }

    public static void checkArg(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static <T> T checkNotNull(T object, String name) {
        if (object == null) {
            throw new IllegalArgumentException(String.format("Object arg %s shouldn't be null", name));
        }
        return object;
    }

    public static void checkArg(boolean condition, String messageTemplate, Object... args) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(messageTemplate, args));
        }
    }
}
