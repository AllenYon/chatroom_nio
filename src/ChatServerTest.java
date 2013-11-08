import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: Link
 * Date: 13-11-8
 * Time: AM11:40
 * To change this template use File | Settings | File Templates.
 */
public class ChatServerTest implements Observer, Runnable {
    private static final String BLANK = " ";
    private HashMap<InetSocketAddress, String> onLineMap;
    private ChatServer server;

    public ChatServerTest() {
        server = new ChatServer(8899);
        server.addObserver(this);
        server.start();
        new Thread(this).start();
    }

    @Override
    public void update(Observable observable, Object arg) {
        //ToDo
        ChatServer server = (ChatServer) observable;
        switch (server.getStatus()) {
            case ChatServer.SEV_ON:
                System.out.println("服务器开启了");
                onLineMap = new HashMap<InetSocketAddress, String>();
                break;
            case ChatServer.SEV_OFF:
                System.out.println("服务器关闭了");
                onLineMap = null;
                break;
            case ChatServer.CLT_CONNECT:

                break;
            case ChatServer.CLT_DISCONNECT:
                quit((InetSocketAddress) arg);
                break;
            case ChatServer.MSG_SEND:
                break;
            case ChatServer.MSG_RECEIVE:
                Message msg = Message.getInstance();
                msg.create(server.getReceiveMessage());
                msg.setFromIp((InetSocketAddress) arg);
                handleMsg(msg);
                break;
            case ChatServer.ERROR:
                System.out.println("Error");
                break;
        }
    }

    private void handleMsg(Message msg) {
        int type = msg.getType();
        InetSocketAddress formIp = msg.getFromIp();
        switch (type) {
            case Message.MSG_1:
                System.out.println(msg.getMsg() + BLANK + msg.toIpString(formIp) + BLANK + "登录了");
                onLineMap.put(formIp, msg.getMsg());
                msg.setType(Message.MSG_2);
                server.send(msg.toString());
                msg.setType(Message.MSG_3);
                msg.setOnLineMap(onLineMap);
                server.send(msg.toString(), formIp);
                break;
            case Message.MSG_4:
                System.out.println(onLineMap.get(msg.getFromIp()) + BLANK + "said" + BLANK + msg.getMsg());
                msg.setType(Message.MSG_6);
                server.send(msg.toString());
                break;
            case Message.MSG_7:
                quit(formIp);
                break;
        }
    }

    private void quit(InetSocketAddress address) {
        if (onLineMap.get(address) != null) {
            System.out.println(onLineMap.get(address) + BLANK +
                    Message.getInstance().toIpString(address) + BLANK +
                    "退出了");
            onLineMap.remove(address);
            Message msg = Message.getInstance();
            msg.setType(Message.MSG_8);
            msg.setFromIp(address);
            server.send(msg.toString());
        }

    }


    @Override
    public void run() {
        //ToDo
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String inputLine = br.readLine();
                if (inputLine.trim().toLowerCase().equals("off")) {
                    server.close();
                } else {
                    server.send(Message.MSG_4 + Message.SEPARATOR + inputLine);
                    System.out.println("通知:" + inputLine);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        new ChatServerTest();
    }

}
