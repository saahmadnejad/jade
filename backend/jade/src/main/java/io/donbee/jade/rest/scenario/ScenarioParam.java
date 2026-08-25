package io.donbee.jade.rest.scenario;

/**
 * Declaration of one configurable scenario parameter.
 *
 * <p><b>Old GUI implementation:</b> No direct Swing GUI equivalent.</p>
 */
public class ScenarioParam {

    public enum Type { INT, STRING, BOOLEAN }

    private final String name;
    private final Type type;
    private final Object defaultValue;
    private final Long minValue;
    private final Long maxValue;
    private final String description;

    public ScenarioParam(String name, Type type, Object defaultValue,
                         Long minValue, Long maxValue, String description) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.description = description;
    }

    public static ScenarioParam intParam(String name, int defaultValue, long min, long max, String description) {
        return new ScenarioParam(name, Type.INT, defaultValue, min, max, description);
    }

    public static ScenarioParam stringParam(String name, String defaultValue, String description) {
        return new ScenarioParam(name, Type.STRING, defaultValue, null, null, description);
    }

    public static ScenarioParam boolParam(String name, boolean defaultValue, String description) {
        return new ScenarioParam(name, Type.BOOLEAN, defaultValue, null, null, description);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Long getMinValue() {
        return minValue;
    }

    public Long getMaxValue() {
        return maxValue;
    }

    public String getDescription() {
        return description;
    }
}
