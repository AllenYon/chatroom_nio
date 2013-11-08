import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: Link
 * Date: 13-11-7
 * Time: PM4:02
 * To change this template use File | Settings | File Templates.
 */
public class TCPServerSelector {
    private static final int BUFSIZE = 256;
    private static final int TIMEOUT = 3000;

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();

//        for (String arg : args) {
            ServerSocketChannel listnChannel = ServerSocketChannel.open();
//            listnChannel.socket().bind(new InetSocketAddress(Integer.parseInt(arg)));
        listnChannel.socket().bind(new InetSocketAddress(5225));

            listnChannel.configureBlocking(false);
            listnChannel.register(selector, SelectionKey.OP_ACCEPT);
//        }

        TCPProtocol protocol = new ChatRoomSelectorProtocol(BUFSIZE);

        while (true) {
            //Run forever , processing available I/O operations
            //Wait for some channel to be read(or timeout)
            if (selector.select(TIMEOUT) == 0) {
                //return # of ready channs
                System.out.println(".");
                continue;
            }

            //Get iterator on set of keys with I/O to process
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();
                if (key.isAcceptable()) {
                    protocol.handleAccept(key);
                }
                if (key.isReadable()) {
                    protocol.handleRead(key);
                }
                if (key.isValid() && key.isWritable()) {
                    protocol.handleWrite(key);
                }
                keyIter.remove();
            }
        }
    }
}
