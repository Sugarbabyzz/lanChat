import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class Server {

    private JFrame frame;
    private JTextArea contentArea;  //文本域
    private JTextField txt_message;  //用于显示文本信息
    private JTextField txt_port;    //设置端口号
    private JButton btn_start;      //开始按钮
    private JButton btn_stop;       //断开按钮
    private JButton btn_send;       //发送按钮
    private JPanel northPanel;      //北方面板
    private JPanel southPanel;      //南方面板
    private JScrollPane rightPanel; //左边滚动条
    private JScrollPane leftPanel;  //右边滚动条
    private JSplitPane centerSplit; //分割线
    private JList userList;         //列表组件
    private DefaultListModel listModel;

    private ServerSocket serverSocket;
    private ServerThread serverThread;
    private ArrayList<ClientThread> clients;

    private boolean isStart = false;

    // 主方法,程序执行入口
    public static void main(String[] args) {

        new Server();
    }


    // 构造方法
    public Server() {
        frame = new JFrame("服务器");
        contentArea = new JTextArea();
        contentArea.setEditable(false);
        contentArea.setForeground(Color.blue);
        txt_message = new JTextField();
        txt_port = new JTextField("6666");
        btn_start = new JButton("启动");
        btn_stop = new JButton("停止");
        btn_send = new JButton("发送");
        btn_stop.setEnabled(false);
        listModel = new DefaultListModel();
        userList = new JList(listModel);

        southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(new TitledBorder("写消息"));
        southPanel.add(txt_message, "Center");
        southPanel.add(btn_send, "East");
        leftPanel = new JScrollPane(userList);
        leftPanel.setBorder(new TitledBorder("在线用户"));

        rightPanel = new JScrollPane(contentArea);
        rightPanel.setBorder(new TitledBorder("消息显示区"));

        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel,
                rightPanel);
        centerSplit.setDividerLocation(100);
        northPanel = new JPanel();
        northPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 0));
        northPanel.add(new JLabel("端口："));
        northPanel.add(txt_port);
        northPanel.add(btn_start);
        northPanel.add(btn_stop);
        northPanel.setBorder(new TitledBorder("配置信息"));

        frame.setLayout(new BorderLayout());
        frame.add(northPanel, "North");
        frame.add(centerSplit, "Center");
        frame.add(southPanel, "South");
        frame.setSize(600, 400);

        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
        frame.setLocation((screen_width - frame.getWidth()) / 2,
                (screen_height - frame.getHeight()) / 2);
        frame.setVisible(true);

        // 关闭窗口时事件
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (isStart) {
                    closeServer();// 关闭服务器
                }
                System.exit(0);// 退出程序
            }
        });

        // 文本框按回车键时事件
        txt_message.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });

        // 单击发送按钮时事件
        btn_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                send();
            }
        });

        // 单击启动服务器按钮时事件
        btn_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isStart) {
                    JOptionPane.showMessageDialog(frame, "服务器已处于启动状态，不要重复启动！",
                            "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int max = 50;   //默认服务器最大人数为50
                int port;
                try {
                    try {
                        port = Integer.parseInt(txt_port.getText());
                    } catch (Exception e1) {
                        throw new Exception("端口号为正整数！");
                    }
                    if (port <= 0) {
                        throw new Exception("端口号为正整数！");
                    }
                    //启动服务器
                    serverStart(max, port);
                    contentArea.append("服务器已成功启动!人数上限：" + max + ",端口：" + port
                            + "\r\n");
                    btn_start.setEnabled(false);
                    txt_port.setEnabled(false);
                    btn_stop.setEnabled(true);
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, exc.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 单击停止服务器按钮时事件
        btn_stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isStart) {
                    JOptionPane.showMessageDialog(frame, "服务器还未启动，无需停止！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    closeServer();
                    btn_start.setEnabled(true);
                    txt_port.setEnabled(true);
                    btn_stop.setEnabled(false);
                    contentArea.append("服务器成功停止!\r\n");
                } catch (Exception exc) {
                    JOptionPane.showMessageDialog(frame, "停止服务器发生异常！", "错误",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    // 启动服务器
    public void serverStart(int max, int port) throws java.net.BindException {
        try {
            clients = new ArrayList<ClientThread>();
            serverSocket = new ServerSocket(port);
            serverThread = new ServerThread(serverSocket, max);
            serverThread.start();
            isStart = true;
        } catch (BindException e) {
            isStart = false;
            throw new BindException("端口号已被占用，请换一个！");
        } catch (Exception e1) {
            e1.printStackTrace();
            isStart = false;
            throw new BindException("启动服务器异常！");
        }
    }

    // 关闭服务器
    public void closeServer() {
        try {
            if (serverThread != null)
                serverThread.stop();// 停止服务器线程

            for (int i = clients.size() - 1; i >= 0; i--) {
                // 给所有在线用户发送关闭命令
                clients.get(i).getWriter().println("CLOSE");
                clients.get(i).getWriter().flush();
                // 释放资源
                clients.get(i).stop();// 停止此条为客户端服务的线程
                clients.get(i).reader.close();
                clients.get(i).writer.close();
                clients.get(i).socket.close();
                clients.remove(i);
            }
            if (serverSocket != null) {
                serverSocket.close();// 关闭服务器端连接
            }
            listModel.removeAllElements();// 清空用户列表
            isStart = false;
        } catch (IOException e) {
            e.printStackTrace();
            isStart = true;
        }
    }

    // 执行消息发送
    public void send() {
        if (!isStart) {
            JOptionPane.showMessageDialog(frame, "服务器还未启动,不能发送消息！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (clients.size() == 0) {
            JOptionPane.showMessageDialog(frame, "没有用户在线,不能发送消息！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String message = txt_message.getText().trim();
        if (message == null || message.equals("")) {
            JOptionPane.showMessageDialog(frame, "消息不能为空！", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        sendServerMessage(message);// 群发服务器消息
        contentArea.append("服务器：" + txt_message.getText() + "\r\n");
        txt_message.setText(null);
    }

    // 群发服务器消息
    public void sendServerMessage(String message) {
        for (int i = clients.size() - 1; i >= 0; i--) {
            clients.get(i).getWriter().println("***服务器管理员***：" + message);
            clients.get(i).getWriter().flush();
        }
    }

    // 服务器线程
    class ServerThread extends Thread {
        private ServerSocket serverSocket;
        private int max;// 人数上限
        private BufferedReader reader;

        // 服务器线程的构造方法
        public ServerThread(ServerSocket serverSocket, int max) {
            this.serverSocket = serverSocket;
            this.max = max;
        }

        public void run() {
            while (true) {// 不停的等待客户端的连接
                try {
                    Socket socket = serverSocket.accept();

                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    /**
                     * 判断聊天模式
                     * *Single : max = 2 ; Group : max = 30
                     */

                    String isSingle = reader.readLine();
                    System.out.println(isSingle);
                    if(isSingle.equals("true") || max==2){
                        max = 2;
                    }else {
                        max = 50;
                    }

                    if (clients.size() == max) {// 如果已达人数上限
                        reader= new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        PrintWriter w = new PrintWriter(socket
                                .getOutputStream());
                        // 接收客户端的基本用户信息
                        String inf = reader.readLine();
                        StringTokenizer st = new StringTokenizer(inf, "@");
                        User user = new User(st.nextToken(), st.nextToken());
                        // 反馈连接成功信息
                        w.println("MAX@服务器：对不起，" + user.getName()
                                + user.getIp() + "，服务器在线人数已达上限，请稍后尝试连接！");
                        w.flush();
                        // 释放资源
                        reader.close();
                        w.close();
                        socket.close();
                        continue;
                    }
                    ClientThread client = new ClientThread(socket, max);
                    client.start(); // 开启对此客户端服务的线程
                    clients.add(client);
                    listModel.addElement(client.getUser().getName());// 更新在线列表
                    contentArea.append(client.getUser().getName()
                            + client.getUser().getIp() + "上线!\r\n");
                    if (max == 2){
                        contentArea.append("当前服务器为【单聊】模式！人数上限：2 \r\n ");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 为一个客户端服务的线程
    class ClientThread extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private User user;

        public BufferedReader getReader() {
            return reader;
        }

        public PrintWriter getWriter() {
            return writer;
        }

        public User getUser() {
            return user;
        }

        // 客户端线程的构造方法
        public ClientThread(Socket socket, int max) {
            try {
                this.socket = socket;
                reader = new BufferedReader(new InputStreamReader(socket
                        .getInputStream()));
                writer = new PrintWriter(socket.getOutputStream());
                // 接收客户端的基本用户信息
                String inf = reader.readLine();
                StringTokenizer st = new StringTokenizer(inf, "@");
                user = new User(st.nextToken(), st.nextToken());
                // 反馈连接成功信息
                if(max ==2){
                    writer.println(user.getName() + user.getIp() + "与服务器连接成功！ 当前进入的是【单聊】服务器。 \n" );
                    writer.flush();
                }else {
                    writer.println(user.getName() + user.getIp() + "与服务器连接成功！ 当前进入的是【群聊】服务器。\n" );
                    writer.flush();
                }

                // 反馈当前在线用户信息
                if (clients.size() > 0) {
                    String temp = "";
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        temp += (clients.get(i).getUser().getName() + "/" + clients
                                .get(i).getUser().getIp())
                                + "@";
                    }
                    writer.println("USERLIST@" + clients.size() + "@" + temp);
                    writer.flush();
                }
                // 向所有在线用户发送该用户上线命令
                for (int i = clients.size() - 1; i >= 0; i--) {
                    clients.get(i).getWriter().println(
                            "ADD@" + user.getName() + user.getIp());
                    clients.get(i).getWriter().flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("deprecation")
        public void run() {// 不断接收客户端的消息，进行处理。
            String message = null;
            while (true) {
                try {
                    message = reader.readLine();// 接收客户端消息
                    if (message.equals("CLOSE"))// 下线命令
                    {
                        contentArea.append(this.getUser().getName()
                                + this.getUser().getIp() + "下线!\r\n");
                        // 断开连接释放资源
                        reader.close();
                        writer.close();
                        socket.close();

                        // 向所有在线用户发送该用户的下线命令
                        for (int i = clients.size() - 1; i >= 0; i--) {
                            clients.get(i).getWriter().println(
                                    "DELETE@" + user.getName());
                            clients.get(i).getWriter().flush();
                        }

                        listModel.removeElement(user.getName());// 更新在线列表

                        // 删除此条客户端服务线程
                        for (int i = clients.size() - 1; i >= 0; i--) {
                            if (clients.get(i).getUser() == user) {
                                ClientThread temp = clients.get(i);
                                clients.remove(i);// 删除此用户的服务线程
                                temp.stop();// 停止这条服务线程
                                return;
                            }
                        }
                    } else {
                        dispatcherMessage(message);// 转发消息
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 转发消息
        public void dispatcherMessage(String message) {
            StringTokenizer stringTokenizer = new StringTokenizer(message, "@");
            String source = stringTokenizer.nextToken();
            String owner = stringTokenizer.nextToken();
            String content = stringTokenizer.nextToken();
            message = source + "：" + content;
            contentArea.append(message + "\r\n");
            if (owner.equals("ALL")) {// 群发
                for (int i = clients.size() - 1; i >= 0; i--) {
                    clients.get(i).getWriter().println(message );
                    clients.get(i).getWriter().flush();
                }
            }
        }
    }
}