package server;

/**
 * Class to wrap login messages from client
 */
public class LoginMessage extends Message{

    private LoginMessage(String author) {
        super(author);
    }

    /**
     * Parses LoginMessage from raw text
     * @param text raw text from client
     * @return LoginMessage object
     */
    public static LoginMessage parse(String text) {
        String authorName = text.substring(text.indexOf(" ") + 1);
        return new LoginMessage(authorName);
    }

    /**
     *
     * @return TextMessage to be broadcasted to clients
     */
    public TextMessage buildSystemNotification() {
        return new TextMessage(author + " connected;", Server.serverName);
    }
}
