package test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts simple math expressions into a GenericConfig .conf string.
 *
 * <h3>Supported syntax (one expression per line)</h3>
 * <pre>
 * OUT = A + B          → test.PlusAgent      (A,B → OUT)
 * OUT = A * B          → test.MultiplyAgent  (A,B → OUT)
 * OUT = avg(A, B)      → test.AverageAgent   (A,B → OUT)
 * OUT = max(A, B)      → test.MaxAgent       (A,B → OUT)
 * OUT = min(A, B)      → test.MinAgent       (A,B → OUT)
 * OUT = inc(A)         → test.IncAgent       (A → OUT)
 * OUT = A + 1          → test.IncAgent       (A → OUT)
 * </pre>
 * Blank lines and lines starting with {@code #} are ignored.
 * Any unrecognised line throws {@link IllegalArgumentException}.
 */
public class ExpressionParser {

    // OUT = A + B  or  OUT = A * B
    private static final Pattern BIN_OP =
            Pattern.compile("^(\\w+)\\s*=\\s*(\\w+)\\s*([+*])\\s*(\\w+)\\s*$");

    // OUT = func(A, B)   — avg / max / min
    private static final Pattern FUNC2 =
            Pattern.compile("^(\\w+)\\s*=\\s*(avg|max|min)\\s*\\(\\s*(\\w+)\\s*,\\s*(\\w+)\\s*\\)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    // OUT = inc(A)
    private static final Pattern FUNC1 =
            Pattern.compile("^(\\w+)\\s*=\\s*inc\\s*\\(\\s*(\\w+)\\s*\\)\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Parses {@code text} (one expression per line) and returns a .conf string
     * ready to be saved to a file and loaded by {@link GenericConfig}.
     *
     * @param text expression text (may contain blank lines / comments)
     * @return multi-line .conf string
     * @throws IllegalArgumentException if any expression cannot be parsed
     */
    public static String parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("No expressions provided.");
        }

        List<String> confLines = new ArrayList<>();

        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String entry = tryParse(line);
            if (entry == null) {
                throw new IllegalArgumentException(
                        "Cannot parse: \"" + line + "\"\n"
                        + "Supported forms:\n"
                        + "  OUT = A + B\n"
                        + "  OUT = A * B\n"
                        + "  OUT = avg(A, B)\n"
                        + "  OUT = max(A, B)\n"
                        + "  OUT = min(A, B)\n"
                        + "  OUT = inc(A)  or  OUT = A + 1");
            }
            confLines.add(entry);
        }

        if (confLines.isEmpty()) {
            throw new IllegalArgumentException("No valid expressions found.");
        }

        return String.join("\n", confLines) + "\n";
    }

    /**
     * Tries to match {@code line} against each supported pattern.
     *
     * @return a 3-line conf block string, or {@code null} if no match
     */
    private static String tryParse(String line) {
        Matcher m;

        // OUT = inc(A)
        m = FUNC1.matcher(line);
        if (m.matches()) {
            return block("test.IncAgent", m.group(2), m.group(1));
        }

        // OUT = func(A, B)
        m = FUNC2.matcher(line);
        if (m.matches()) {
            String func = m.group(2).toLowerCase();
            String agent = func.equals("avg") ? "test.AverageAgent"
                         : func.equals("max") ? "test.MaxAgent"
                         :                      "test.MinAgent";
            return block(agent, m.group(3) + "," + m.group(4), m.group(1));
        }

        // OUT = A + B  or  OUT = A * B  or  OUT = A + 1
        m = BIN_OP.matcher(line);
        if (m.matches()) {
            String out  = m.group(1);
            String left = m.group(2);
            String op   = m.group(3);
            String right = m.group(4);

            if (op.equals("+") && right.equals("1")) {
                // e.g. D = C + 1  → IncAgent
                return block("test.IncAgent", left, out);
            }
            if (op.equals("+")) {
                return block("test.PlusAgent", left + "," + right, out);
            }
            if (op.equals("*")) {
                return block("test.MultiplyAgent", left + "," + right, out);
            }
        }

        return null;
    }

    /** Formats one agent block as three lines: className, inputs, output. */
    private static String block(String className, String inputs, String output) {
        return className + "\n" + inputs + "\n" + output;
    }
}
