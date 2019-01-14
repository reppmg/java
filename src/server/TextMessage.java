package server;

/**
 * Wrapper for text messages sent to/from clients
 */
public class TextMessage extends Message {
    private final String text;

    TextMessage(String text, String author) {
        super(author);
        this.text = text.trim();
    }

    /**
     *
     * @return text to be sent to client
     */
    public String getFullText() {
        return author + ": " + text;
    }
}
