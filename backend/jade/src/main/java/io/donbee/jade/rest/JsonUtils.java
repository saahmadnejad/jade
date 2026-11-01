package io.donbee.jade.rest;

import io.vertx.core.json.JsonArray;

/**
 * Common JSON utility helpers for REST handlers.
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    /**
     * Converts a string array into a Vert.x JsonArray safely.
     *
     * @param arr the string array (may be null)
     * @return a JsonArray containing the non-null string elements
     */
    public static JsonArray jsonFromArray(String[] arr) {
        JsonArray j = new JsonArray();
        if (arr != null) {
            for (String s : arr) {
                j.add(s);
            }
        }
        return j;
    }
}
