import sun.tools.tree.ShiftLeftExpression;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.util.Iterator;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Link
 * Date: 13-11-8
 * Time: AM11:22
 * To change this template use File | Settings | File Templates.
 */
public class ChatClient extends Chat implements Runnable {

    private boolean isPrepared = false;
    private Selector selector;
    private SelectionKey clientKey;
    private InetSocketAddress address;

    public ChatClient(String host, int port) {
        address = new InetSocketAddress(host, port);
        try {
            selector = Selector.open();
        } catch (IOException e) {
            notifyStateChanged(ERROR, e);
            e.printStackTrace();
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public void send(String msg) {
        notifyStateChanged(MSG_SEND, msg);
        if (null == clientKey) {
            return;
        }
        clientKey.attach(msg);
        clientKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        clientKey.selector().wakeup();
    }


    @Override
    public void run() {
        //ToDo
        try {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(address);
            clientKey = sc.register(selector, SelectionKey.OP_CONNECT);
            isPrepared = true;

            while (isPrepared) {
                int keysCount = selector.select();
                if (keysCount < 1) {
                    continue;
                }
                Set<SelectionKey> set = selector.selectedKeys();
                Iterator<SelectionKey> it = set.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    if (key.isConnectable()) {
                        doConnect(key);
                    }
                    if (key.isValid() && key.isReadable()) {
                        doRead(key);
                    }
                    if (key.isValid() && key.isWritable()) {
                        doWrite(key);
                    }
                }
                set.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
            notifyStateChanged(ERROR, e);
        } finally {
            notifyStateChanged(CLT_DISCONNECT, null);
        }
    }

    private void doConnect(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            sc.finishConnect();
            key.interestOps(SelectionKey.OP_READ);
            notifyStateChanged(CLT_CONNECT, null);
        } catch (IOException e) {
            e.printStackTrace();
            disconnect(key);
        }
    }

    private void doRead(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer bb = ByteBuffer.allocate(BUFFERSIZE);
        StringBuffer sb = new StringBuffer();
        try {
            int count = 0;
            while ((count = sc.read(bb)) > 0) {
                bb.flip();
                sb.append(decoder.decode(bb));
            }
            if (count == -1) {
                disconnect(key);
            } else {
                notifyStateChanged(MSG_RECEIVE, sb.toString().trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
            disconnect(key);
        }
    }

    private void doWrite(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        String msg = (String) key.attachment();
        if (null == msg) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }

        try {
            sc.write(encoder.encode(CharBuffer.wrap(msg)));
        } catch (CharacterCodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            disconnect(key);
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    /**
     * 断开连接
     */
    private void disconnect(SelectionKey key) {
        notifyStateChanged(CLT_DISCONNECT, null);
        try {
            key.cancel();
            key.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        isPrepared = false;
        try {
            if (null != clientKey) {
                clientKey.channel().close();
            }
            if (null != selector) {
                selector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
