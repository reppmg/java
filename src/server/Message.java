package server;


/**
 * Basic message class
 */
public class Message {
    final String author;

    Message(String author) {
        this.author = author.trim();
    }

    public String getAuthor() {
        return author;
    }
}
