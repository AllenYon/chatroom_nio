import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Link
 * Date: 13-11-8
 * Time: PM1:20
 * To change this template use File | Settings | File Templates.
 */

public class ChatClientTest implements Observer, Runnable {

    private static final String BLANK = " ";

    private HashMap<InetSocketAddress, String> onLineMap;
    private String name;
    private ChatClient client;

    public ChatClientTest(String name) {
        this.onLineMap = new HashMap<InetSocketAddress, String>();
        this.name = name;
        client = new ChatClient("127.0.0.1", 8899);
        client.addObserver(this);
        client.start();
        new Thread(this).start();
    }


    @Override
    public void run() {
        //ToDo
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String inputLine = br.readLine();
                if (inputLine.trim().toLowerCase().equals("quit")) {
                    client.send(Message.MSG_7 + "");
                    client.close();
                } else {
                    client.send(Message.MSG_4 + Message.SEPARATOR + inputLine);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void update(Observable observable, Object arg) {
        //ToDo
        ChatClient client = (ChatClient) observable;
        switch (client.getStatus()) {
            case ChatClient.CLT_CONNECT:
                Message msg1 = Message.getInstance();
                msg1.setType(Message.MSG_1);
                msg1.setMsg(name);
                client.send(msg1.toString());
                break;
            case Chat.CLT_DISCONNECT:
                System.out.println("断开服务器连接");
                System.exit(1);
                break;
            case ChatClient.MSG_SEND:
                break;
            case ChatClient.MSG_RECEIVE:
                Message msg = Message.getInstance();
                msg.create((String) arg);
                handleMsg(msg);
                break;
            case ChatClient.ERROR:
                System.out.println("Error");
                break;
        }
    }

    private void handleMsg(Message msg) {
        int type = msg.getType();
        switch (type) {
            case Message.MSG_2:
                System.out.println(msg.getMsg() + BLANK +
                        msg.toIpString(msg.getFromIp()) + BLANK +
                        "登录了");
                onLineMap.put(msg.getFromIp(), msg.getMsg());
                break;
            case Message.MSG_3:
                onLineMap = msg.getOnLineMap();
                System.out.println("连接上了服务器");
                System.out.println("/***在线成员***/");
                Iterator<Map.Entry<InetSocketAddress, String>> it = onLineMap

                        .entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<InetSocketAddress, String> entry = it.next();

                    InetSocketAddress address = entry.getKey();
                    System.out.println(entry.getValue() + BLANK
                            + address.getAddress().getHostAddress() + ":"
                            + address.getPort());
                }
                System.out.println("/***在线成员***/");
                break;
            case Message.MSG_4:
                System.out.println("通知:" + msg.getMsg());
                break;
            case Message.MSG_6:
                System.out.println(onLineMap.get(msg.getFromIp()) + BLANK + "said"
                        + BLANK + msg.getMsg());
                break;
            case Message.MSG_8:
                InetSocketAddress address = msg.getFromIp();
                System.out
                        .println(onLineMap.get(address) + BLANK
                                + Message.getInstance().toIpString(address) + BLANK

                                + "退出了");
                onLineMap.remove(address);
                break;

        }

    }

    public static void main(String[] args) {
        new ChatClientTest("用户" + new Random().nextInt(100));
    }


}
