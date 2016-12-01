package Proxy;


import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ningyu He on 2016/11/29.
 */
public class RequestThread implements Runnable {

    private Map<String, String> header;

    private BufferedInputStream clientInput;

    private DataInputStream serverInput;

    private Socket clientSocket;

    private Socket chatSocket;

    //private ByteArrayOutputStream bos = new ByteArrayOutputStream();

    private String url_for_sending;    //直接就是文件路径了

    private String requestMethod;

    private String HttpVersion;

    private String fileURL = new String();

    public RequestThread(Socket clientSocket) {
        header = new HashMap();
        this.clientSocket = clientSocket;
    }

    public void clientToProxy() throws IOException {
        StringBuilder request = new StringBuilder();
        clientInput = new BufferedInputStream(clientSocket.getInputStream());


        //改一下路径名，到时候你用的时候把我的注释掉就可以了
        //File writename = new File("C:\\Users\\zbh\\Desktop\\request.txt");
        File writename = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\request.txt");
        writename.createNewFile();
        BufferedWriter outrequest = new BufferedWriter(new FileWriter(writename));

        int tempReader;
        int lineNumber = 0;
        int isEndOfRequest = 0;
        //String requestMethod = new String();
        //String HttpVersion = new String();
        String key = new String();
        String value = new String();
        String requestHeadline_2 = new String();

        while ((tempReader = clientInput.read()) != -1) {
            if ((char) (tempReader) == '\r' || (char) (tempReader) == '\n') {
                isEndOfRequest++;
                if (isEndOfRequest == 2) {
                    String requestHeadline = request.toString();
                    lineNumber += 1;
                    if (lineNumber == 1) {
                        outrequest.write(requestHeadline + "\r\n");
                        requestMethod = requestHeadline.split(" ")[0];
                        if (!requestMethod.toLowerCase().equals("get") && !requestMethod.toLowerCase().equals("post")) {
                            requestMethod = null;
                            break;
                        }
                        fileURL = requestHeadline.split(" ")[1];
                        URL url = new URL(fileURL);
                        url_for_sending = url.getFile();

//                        System.out.println(url_for_sending);

                        HttpVersion = requestHeadline.split(" ")[2];
                    }
                    if (lineNumber > 1) {
                        key = requestHeadline.split(": ")[0].toLowerCase();
                        value = requestHeadline.split(": ")[1].toLowerCase();
                        if (!key.equals("cookie") && !key.equals("accept-encoding") &&
                                !key.equals("proxy-connection") && !key.equals("user-agent")) {
                            outrequest.write(requestHeadline + "\r\n");
                        }
                        header.put(key, value);
                        if (header.containsKey("cookie")) header.remove("cookie");
                        if (header.containsKey("accept-encoding")) header.remove("accept-encoding");
                        if (header.containsKey("proxy-connection")) header.remove("proxy-connection");
                        if (header.containsKey("user-agent")) header.remove("user-agent");
                    }
                    request.delete(0, request.length());
                }
                if (isEndOfRequest == 3) {
                    outrequest.write("\r\n");
                    break;
                }
            } else {
                isEndOfRequest = 0;
                request.append((char) tempReader);
            }
        }
        //need to add that "connection: closed"
        header.put("connection", "close");

        if (requestMethod.toLowerCase().equals("post")) {
            int flag = 0;
            while ((tempReader = clientInput.read()) != -1) {
                if ((char) (tempReader) == '\r' || (char) (tempReader) == '\n') {
                    flag += 1;
                    request.append((char) tempReader);
                } else {
                    flag = 0;
                    request.append((char) tempReader);
                }
                if (flag == 4) {
                    requestHeadline_2 = request.toString();
                    outrequest.write(requestHeadline_2);
                    break;
                }
            }
        }

        outrequest.flush();
        outrequest.close();

        System.out.println(requestMethod + " " + url_for_sending + " " + HttpVersion);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println(requestHeadline_2);                                     //测试代码

        /*while ((tempReader = clientInput.read()) != -1) {
            bos.write(tempReader);
        }*/
    }

    public void proxyToServer() {
        try {
            chatSocket = new Socket(header.get("host"), 80);
            PrintWriter writer = new PrintWriter(chatSocket.getOutputStream());
            writer.print(requestMethod + " " + url_for_sending + " " + HttpVersion + "\r\n");

            System.out.print(requestMethod + " " + url_for_sending + " " + HttpVersion + "\r\n");

            for (Map.Entry<String, String> entry : header.entrySet()) {
                writer.print(entry.getKey() + ":" + entry.getValue() + "\r\n");
                System.out.print(entry.getKey() + ":" + entry.getValue() + "\r\n");
            }
            System.out.println();
            writer.print("\r\n");
            writer.flush();
//            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serverToProxy() throws IOException {

        if (chatSocket.isClosed()) {
            System.out.println("Socket is closed");
            return;
        }

        StringBuilder response = new StringBuilder();
        serverInput = new DataInputStream(chatSocket.getInputStream());

        //File writename = new File("C:\\Users\\zbh\\Desktop\\request.txt");
        File writename = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\response.txt");
        writename.createNewFile();
        BufferedWriter outresponse = new BufferedWriter(new FileWriter(writename));

        int tempReader;

        while ((tempReader = serverInput.read()) != -1) {
            response.append((char) tempReader);
        }
        String responseAll = response.toString();
        System.out.println(responseAll);
        outresponse.write(responseAll);
        outresponse.flush();
        outresponse.close();
    }

    @Override
    public void run() {
        try {
            clientToProxy();
            proxyToServer();
            serverToProxy();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
