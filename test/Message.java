package test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Immutable message passed between Topics and Agents.
 * All fields are public and final; the same byte array is represented
 * as text and, when numeric, as a double.
 */
public class Message {

    /** Raw byte payload. */
    public final byte[] data;

    /** UTF-8 string representation of the payload. */
    public final String asText;

    /**
     * Numeric value of the payload, or {@link Double#NaN} if the text
     * cannot be parsed as a double.
     */
    public final double asDouble;

    /** Timestamp of message creation. */
    public final Date date;

    /**
     * Primary constructor: builds all fields from a string.
     *
     * @param text string payload; {@code null} is treated as empty string
     */
    public Message(String text) {
        this.asText   = (text == null) ? "" : text;
        this.data     = this.asText.getBytes(StandardCharsets.UTF_8);
        this.asDouble = tryParseDouble(this.asText);
        this.date     = new Date();
    }

    /**
     * Convenience constructor from a byte array.
     *
     * @param data raw bytes; {@code null} or empty produces an empty-string message
     */
    public Message(byte[] data) {
        this(data == null || data.length == 0
                ? ""
                : new String(data, StandardCharsets.UTF_8));
    }

    /**
     * Convenience constructor from a double.
     *
     * @param value numeric value to encode
     */
    public Message(double value) {
        this(Double.toString(value));
    }

    private double tryParseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
