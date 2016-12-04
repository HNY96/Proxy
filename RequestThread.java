package proxy;



import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by Ningyu He on 2016/11/29.
 */
public class RequestThread implements Runnable {

    private Map<String, String> header;  //存储除请求行外的请求报文

    private BufferedInputStream clientInput;  //用户输入流

    private DataInputStream serverInput;  //服务器输入流

    private Socket clientSocket;       //proxy与client的socket

    private Socket chatSocket;         //proxy与server的socket

    private String url_for_sending;    //网页文件路径

    private String requestMethod;      //请求头的HTTP方法

    private String HttpVersion;        //HTTP版本

    private String fileURL;            //完整的URL

    private int flag_cache = 0;        //本地有无缓存的标记

    private int flag_for_interrupt_thread = 0;   //要不要终止线程的标记

    private String response = null;    //从server返回的数据

    public RequestThread(Socket clientSocket) {
        header = new HashMap();
        this.clientSocket = clientSocket;
    }

    public void clientToProxy() throws IOException {
        StringBuilder request = new StringBuilder();
        clientInput = new BufferedInputStream(clientSocket.getInputStream());

        //改一下路径名，到时候你用的时候把我的注释掉就可以了
        File writename = new File("C:\\Users\\zbh\\Desktop\\request\\request" + Proxy.count + ".txt");
        //File writename = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\request" + Proxy.count + ".txt");
        writename.createNewFile();
        BufferedWriter outrequest = new BufferedWriter(new FileWriter(writename));

        int tempReader;
        int lineNumber = 0;
        int isEndOfRequest = 0;
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
                        requestMethod = requestHeadline.split(" ")[0];
                        if (!requestMethod.toLowerCase().equals("get") && !requestMethod.toLowerCase().equals("post")) {
                            //requestMethod = null;
                            if(writename.exists()){
                                writename.delete();
                            }
                            Proxy.count -= 1;
                            flag_for_interrupt_thread = 1;
                            return;
                        }                  //对于请求不是get 或者 post 的数据包，直接舍弃
                        outrequest.write(requestHeadline + "\r\n");
                        fileURL = requestHeadline.split(" ")[1];

                        for (Map.Entry<String, String> entry : Proxy.header_1.entrySet()) {
                            if (entry.getKey().equals(fileURL)){
                                flag_cache = 1;                      //有缓存的标记
                                response = entry.getValue();         //这个for循环判断本地有无缓存
                                break;
                            }
                        }

                        URL url = new URL(fileURL);
                        url_for_sending = url.getFile();
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

        if (requestMethod.toLowerCase().equals("post")) {     //对post请求头后面的字段进行处理
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

        //System.out.println(requestMethod + " " + url_for_sending + " " + HttpVersion);
        /*for (Map.Entry<String, String> entry : header.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println();
        System.out.println(requestHeadline_2);                                     //测试代码*/

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
            //writer.close();                  //不去掉会使server到proxy的socket close掉，无法接收文件
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serverToProxy() throws IOException {
        try{
            if (chatSocket.isClosed()) {
                System.out.println("Socket is closed");
                return;
            }
            if (response == null){
                //StringBuilder response = new StringBuilder();

                serverInput = new DataInputStream(chatSocket.getInputStream());

                File writename = new File("C:\\Users\\zbh\\Desktop\\response\\response" + Proxy.count + ".txt");
                //File writename = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\response" + Proxy.count + ".txt");
                writename.createNewFile();
                BufferedWriter outresponse = new BufferedWriter(new FileWriter(writename));

                /*File writemap = new File("C:\\Users\\zbh\\Desktop\\map\\map.txt");
                //File writemap = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\request" + Proxy.count + ".txt");
                writemap.createNewFile();
                BufferedWriter outmap = new BufferedWriter(new FileWriter(writemap));*/

                int length;
                byte[] tempByteArray = new byte[1024];
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                while ((length = serverInput.read(tempByteArray)) != -1) {
                    bo.write(tempByteArray, 0, length);
                }
                response = new String(bo.toByteArray(), "UTF-8");
                System.out.println(response);
                outresponse.write(response);
                Proxy.header_1.put(fileURL , response);
                /*outmap.write(fileURL + " " + responseAll);
                outmap.flush();
                outmap.close();*/
                outresponse.flush();
                outresponse.close();
            }
            if (response != null){
                File writename = new File("C:\\Users\\zbh\\Desktop\\response\\response" + Proxy.count + ".txt");
                //File writename = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\response" + Proxy.count + ".txt");
                writename.createNewFile();
                BufferedWriter outresponse = new BufferedWriter(new FileWriter(writename));

                /*File writemap = new File("C:\\Users\\zbh\\Desktop\\map\\map.txt");
                //File writemap = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\request" + Proxy.count + ".txt");
                writemap.createNewFile();
                BufferedWriter outmap = new BufferedWriter(new FileWriter(writemap));*/

                System.out.println(response);
                outresponse.write(response);
                Proxy.header_1.put(fileURL , response);
                /*outmap.write(fileURL + " " + responseAll);
                outmap.flush();
                outmap.close();*/
                outresponse.flush();
                outresponse.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void proxyToClient() throws IOException {     //不是很懂这个函数怎么写。。
        try{
            PrintWriter toBrowser = new PrintWriter(clientSocket.getOutputStream());
            toBrowser.print(response);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            Proxy.count += 1;
            clientToProxy();
            if (flag_for_interrupt_thread == 1) {       //对于不是get和post的数据包，终止线程舍去
                return;
            }
            if (flag_cache == 1){                       //如果本地有缓存
                proxyToClient();
            }
            else if(flag_cache == 0){                   //如果本地没有缓存
                proxyToServer();
                serverToProxy();
                proxyToClient();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
