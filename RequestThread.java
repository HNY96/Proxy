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

    private Map<String, String> header;  // 存储除请求行外的请求报文

    private BufferedInputStream clientInput;  // 用户输入流

    private DataInputStream serverInput;  // 服务器输入流

    private Socket clientSocket;       //proxy 与 client 的 socket

    private Socket chatSocket;         //proxy 与 server 的 socket

    private String url_for_sending;    // 网页文件路径

    private String requestMethod;      // 请求头的 HTTP 方法

    private String HttpVersion;        //HTTP 版本

    private String fileURL;            // 完整的 URL

    private int flag_cache = 0;        // 本地有无缓存的标记

    private int flag_for_interrupt_thread = 0;   // 要不要终止线程的标记

    private byte[] response = null;    // 从 server 返回的数据

    private ByteArrayOutputStream bo = new ByteArrayOutputStream(); // 用于储存图片、视频的数据流

    private String requestHeadline_2 = null;

    public RequestThread(Socket clientSocket) {
        header = new HashMap();
        this.clientSocket = clientSocket;
    }

    public void clientToProxy() {

        try {
            //writename 将所有的 request 保存到文件中，可能会有空文件情况，暂未解决
            //File writename = new File("C:\\Users\\zbh\\Desktop\\request\\request" + Proxy.count + ".txt");
            File writename = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\request" + Proxy.count + ".txt");
            writename.createNewFile();
            BufferedWriter outrequest = new BufferedWriter(new FileWriter(writename));

            int tempReader;
            int lineNumber = 0;
            int isEndOfRequest = 0;
            String key = new String();
            String value = new String();

            StringBuilder request = new StringBuilder();
            clientInput = new BufferedInputStream(clientSocket.getInputStream());

            // 以字节为单位从流中读入信息
            while ((tempReader = clientInput.read()) != -1) {
                if ((char) (tempReader) == '\r' || (char) (tempReader) == '\n') {
                    isEndOfRequest++;
                    if (isEndOfRequest == 2) {// 连续读到 \r\n 说明已经到达行末，处理这一行
                        String requestHeadline = request.toString();
                        lineNumber += 1;
                        if (lineNumber == 1) {
                            requestMethod = requestHeadline.split(" ")[0];

                            if (requestMethod == null) {
                                System.out.println("lisdjflaks");
                            }

                            if (!requestMethod.toLowerCase().equals("get") && !requestMethod.toLowerCase().equals("post")) {
                                if (writename.exists()) {
                                    writename.delete();
                                }
                                Proxy.count -= 1;
                                flag_for_interrupt_thread = 1;
                                return;
                            }                  // 对于请求不是 get 或者 post 的数据包，直接舍弃
                            outrequest.write(requestHeadline + "\r\n");

                            fileURL = requestHeadline.split(" ")[1];// 完整的有文件路径、主机名的 url 地址

                            for (Map.Entry<String, byte[]> entry : Proxy.header_1.entrySet()) {//header_1 是一个键值对为 fileurl 和完整响应报文的映射表
                                if (entry.getKey().equals(fileURL)) {
                                    flag_cache = 1;                      // 有缓存的标记
                                    response = entry.getValue();         // 这个 for 循环判断本地有无缓存
                                    break;
                                }
                            }

                            URL url = new URL(fileURL);
                            url_for_sending = url.getFile();// 用 URL 类将文件路径提取出来，成为标准的请求报头，没有主机名
                            HttpVersion = requestHeadline.split(" ")[2];
                        }
                        if (lineNumber > 1) {
                            key = requestHeadline.split(": ")[0].toLowerCase();
                            value = requestHeadline.split(": ")[1].toLowerCase();
                            if (!key.equals("cookie") && !key.equals("accept-encoding") &&
                                    !key.equals("proxy-connection") && !key.equals("user-agent")) {
                                outrequest.write(requestHeadline + "\r\n");
                            }// 不打印指定的请求头
                            // 过滤掉指定的请求头
                            header.put(key, value);
                            if (header.containsKey("cookie")) {
                                header.remove("cookie");
                                continue;
                            }
                            if (header.containsKey("accept-encoding")) {
                                header.remove("accept-encoding");
                                continue;
                            }
                            if (header.containsKey("proxy-connection")) {
                                header.remove("proxy-connection");
                                continue;
                            }
                            if (header.containsKey("user-agent")) {
                                header.remove("user-agent");
                                continue;
                            }
                        }
                        request.delete(0, request.length());// 将 StringBuilder 清空，以便下一行接受
                    }
                    if (isEndOfRequest == 3) {// 如果收到了 3 个连续的 \ r 或 \ n，说明已到请求行末尾，可以结束
                        outrequest.write("\r\n");
                        break;
                    }
                } else {// 未读到行末的换行符，将读到的字节转换成字符添加到动态 StringBuilder 中
                    isEndOfRequest = 0;
                    request.append((char) tempReader);
                }
            }
            header.put("connection", "close");           //need to add this "connection: close"

            if (requestMethod == null) {
                System.out.println("5465465465");
            }
            if (requestMethod.toLowerCase().equals("post")) {     // 对 post 请求头后面的字段进行处理
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
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void proxyToServer() {
        try {
            chatSocket = new Socket(header.get("host"), 80);
            PrintWriter writer = new PrintWriter(chatSocket.getOutputStream());

            writer.print(requestMethod + " " + url_for_sending + " " + HttpVersion + "\r\n");
            for (Map.Entry<String, String> entry : header.entrySet()) {
                writer.print(entry.getKey() + ":" + entry.getValue() + "\r\n");
            }

            writer.print("\r\n");

            if (requestMethod.toLowerCase().equals("post")) {
                writer.print(requestHeadline_2);
            }

            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void serverToProxy() throws IOException {
        try{
            if (chatSocket.isClosed()) {// 处理 socket 关闭的异常情况
                System.out.println("Socket is closed");
                return;
            }
            serverInput = new DataInputStream(chatSocket.getInputStream());

            int length;
            byte[] tempByteArray = new byte[1024];
            while ((length = serverInput.read(tempByteArray)) != -1) {
                bo.write(tempByteArray, 0, length);
            }//bo 是动态比特数组

            response = bo.toByteArray();
            Proxy.header_1.put(fileURL , response);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private void proxyToClient() throws IOException {
        try {
            BufferedOutputStream writeprint = new BufferedOutputStream(clientSocket.getOutputStream());
            writeprint.write(bo.toByteArray());// 传给浏览器比特数组，可以浏览图片和视频
            writeprint.flush();
            writeprint.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void proxyToClientCache() throws IOException {
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(clientSocket.getOutputStream());
            bufferedOutputStream.write(response);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            Proxy.count += 1;
            clientToProxy();
            if (flag_for_interrupt_thread == 1) {       // 对于不是 get 和 post 的数据包，终止线程舍去
                return;
            }
            if (flag_cache == 1){                       // 如果本地有缓存
                proxyToClientCache();
            }
            else if(flag_cache == 0){                   // 如果本地没有缓存
                proxyToServer();
                serverToProxy();
                proxyToClient();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}