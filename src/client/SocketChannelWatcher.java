package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Prints messages from server
 */
public class SocketChannelWatcher {
    private static final int BUFFER_SIZE = 1024;

    private Selector selector;

    /**
     *
     * @param channel opened SocketChannel to server
     */
    SocketChannelWatcher(SocketChannel channel) throws IOException {
        selector = Selector.open();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }

    /**
     * initiate watching in new thread
     */
    public void watchAsync() {
        Thread thread = new Thread(this::watch);
        thread.start();
    }

    /**
     * watches for messages from the channel
     */
    private void watch() {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        while (true) {
            try {
                int numSelected = selector.select();
                if (numSelected == 0) {
                    continue;
                }
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    SocketChannel channel = ((SocketChannel) key.channel());
                    int numReadBytes = channel.read(buffer);
                    boolean noMessage = numReadBytes == 0;
                    if (noMessage) {
                        continue;
                    }

                    String message = new String(buffer.array(), StandardCharsets.UTF_8);
                    System.out.print(message);
                    Arrays.fill(buffer.array(), (byte) 0);
                    buffer.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }


}
