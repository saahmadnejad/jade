package io.donbee.jade.rest.scenario;

import java.util.List;

/**
 * One agent of a scenario instance.
 *
 * <p><b>Old GUI implementation:</b> No direct Swing GUI equivalent; corresponds
 * to one specifier of a {@code Boot -agents} command line.</p>
 */
public class AgentSpec {

    private final String nameSuffix;
    private final String className;
    private final List<Object> args;

    public AgentSpec(String nameSuffix, String className, List<Object> args) {
        this.nameSuffix = nameSuffix;
        this.className = className;
        this.args = args != null ? List.copyOf(args) : List.of();
    }

    /** Unique suffix within a scenario; deployed as {@code <instance>-<nameSuffix>}. */
    public String getNameSuffix() {
        return nameSuffix;
    }

    public String getClassName() {
        return className;
    }

    public List<Object> getArgs() {
        return args;
    }
}
