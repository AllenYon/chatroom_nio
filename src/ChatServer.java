import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Link
 * Date: 13-11-8
 * Time: AM10:59
 * To change this template use File | Settings | File Templates.
 */
public class ChatServer extends Chat implements Runnable {
    private boolean isPrePared = false;
    private ServerSocketChannel ssc;
    private Selector selector;
    private ArrayList<SelectionKey> serverKeyList;
    private String receiveMessage;

    public ChatServer(int port) {
        try {
            selector = Selector.open();
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(port));
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            serverKeyList = new ArrayList<SelectionKey>();
            isPrePared = true;
        } catch (IOException e) {
            e.printStackTrace();
            notifyStateChanged(ERROR, e);
        }
    }

    public void start() {
        if (isPrePared) {
            new Thread(this).start();
        }
    }

    public void send(String msg, InetSocketAddress... toIps) {
        notifyStateChanged(MSG_SEND, msg);
        if (null == serverKeyList || serverKeyList.size() <= 0) {
            return;
        }

        if (null != toIps && toIps.length >= 1) {
            for (SelectionKey serverKey : serverKeyList) {
                SocketChannel sc = (SocketChannel) serverKey.channel();
                SocketAddress ip = sc.socket().getRemoteSocketAddress();
                for (InetSocketAddress toIp : toIps) {
                    if (toIp.equals(ip)) {
                        serverKey.attach(msg);
                        serverKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        serverKey.selector().wakeup();
                        break;
                    }
                }
            }
        } else {
            //Send all
            for (SelectionKey serverKey : serverKeyList) {
                serverKey.attach(msg);
                serverKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                serverKey.selector().wakeup();
            }

        }

    }

    @Override
    public void run() {
        //ToDo
        notifyStateChanged(SEV_ON, null);
        try {
            while (isPrePared) {
                int keysCount = selector.select();
                if (keysCount < 1) {
                    continue;
                }
                Set<SelectionKey> set = selector.selectedKeys();
                Iterator<SelectionKey> it = set.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    if (key.isAcceptable()) {
                        doAccept(key);
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
        } finally {
            notifyStateChanged(SEV_OFF, null);
        }
    }

    private void doAccept(SelectionKey key) {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        try {
            SocketChannel sc = ssc.accept();
            sc.configureBlocking(false);
            SelectionKey newKey = sc.register(selector, SelectionKey.OP_READ);
            serverKeyList.add(newKey);
            notifyStateChanged(CLT_CONNECT, sc.socket().getRemoteSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
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
                disconnect(key, sc);
            } else {
                receiveMessage = sb.toString().trim();
                notifyStateChanged(MSG_RECEIVE, sc.socket().getRemoteSocketAddress());
            }

        } catch (IOException e) {
            disconnect(key, sc);
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
            disconnect(key, sc);
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    private void disconnect(SelectionKey key, SocketChannel sc) {
        serverKeyList.remove(key);
        notifyStateChanged(CLT_DISCONNECT, sc.socket().getRemoteSocketAddress());
        try {
            key.cancel();
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getReceiveMessage() {
        return receiveMessage;
    }

    public void close() {
        isPrePared = false;
        try {
            if (null != serverKeyList) {
                for (SelectionKey key : serverKeyList) {
                    key.channel().close();
                }
            }
            if (null != selector) {
                selector.close();
            }
            if (null != ssc) {
                ssc.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

