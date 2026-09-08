package io.donbee.jade.examples.devteam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Artifacts from LLM output. Convention: a fenced code block whose
 * opening line carries a file path declares an Artifact:
 *
 * <pre>
 * ```python src/app.py
 * print("hello")
 * ```
 * </pre>
 */
public final class ArtifactParser {

    // ```<optional language> <path>  ... content ... ```
    private static final Pattern BLOCK = Pattern.compile(
        "```[^\\n]*?([A-Za-z0-9_./-]+\\.[A-Za-z0-9]+)[ \\t]*\\n(.*?)```",
        Pattern.DOTALL);

    private ArtifactParser() {
    }

    /**
     * @return artifacts in order of appearance; later blocks with the same
     *         path overwrite earlier ones
     */
    public static Map<String, String> parse(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        Matcher matcher = BLOCK.matcher(text);
        while (matcher.find()) {
            String path = matcher.group(1).trim();
            String content = matcher.group(2).stripTrailing() + "\n";
            result.put(path, content);
        }
        return result;
    }
}
