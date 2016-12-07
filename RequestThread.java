package proxy;




import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.net.*;



/**
 * Created by Ningyu He on 2016/11/29.
 */
public class RequestThread implements Runnable {

    private Map<String, String> header = new HashMap();//存储除请求行外的请求报文

    private BufferedInputStream clientInput;  //用户输入流

    private DataInputStream serverInput;  //服务器输入流

    private Socket clientSocket;       //proxy与client的socket

    private Socket chatSocket;         //proxy与server的socket

    private String url_for_sending = null;    //网页文件路径

    private String requestMethod = null;      //请求头的HTTP方法

    private String HttpVersion = null;        //HTTP版本

    private String fileURL = null;            //完整的URL

    private int flag_cache = 0;        //本地有无缓存的标记

    private int flag_for_interrupt_thread = 0;   //要不要终止线程的标记

    private byte[] response = null;    //从server返回的数据

    private ByteArrayOutputStream bo = new ByteArrayOutputStream(); //用于储存图片、视频的数据流

    private String requestHeadline_2 = null;

    private String host = null;

    public RequestThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void clientToProxy() throws IOException {
        //writename将所有的request保存到文件中，可能会有空文件情况，暂未解决
        File writename = new File("C:\\Users\\zbh\\Desktop\\record\\record" + Proxy.count + ".txt");
        //File writename = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\record" + Proxy.count + ".txt");
        writename.createNewFile();
        BufferedWriter outrecord = new BufferedWriter(new FileWriter(writename));

        File file = new File("C:\\Users\\zbh\\Desktop\\lalala\\list.txt");   //黑名单
        FileInputStream fis = new FileInputStream(file);
        String list = null;
        byte[] bytes = new byte[1024];
        int length = 0;
        while((length = fis.read(bytes)) != -1){
            list += new String(bytes,0,length);
        }

        int tempReader;
        int lineNumber = 0;
        int isEndOfRequest = 0;
        String key = new String();
        String value = new String();

        StringBuilder request = new StringBuilder();
        clientInput = new BufferedInputStream(clientSocket.getInputStream());

        //以字节为单位从流中读入信息
        while ((tempReader = clientInput.read()) != -1) {
            if ((char) (tempReader) == '\r' || (char) (tempReader) == '\n') {
                isEndOfRequest++;
                if (isEndOfRequest == 2) {//连续读到\r\n说明已经到达行末，处理这一行
                    String requestHeadline = request.toString();
                    lineNumber += 1;
                    if (lineNumber == 1) {
                        requestMethod = requestHeadline.split(" ")[0];
                        fileURL = requestHeadline.split(" ")[1];//完整的有文件路径、主机名的url地址
                        URL url = new URL(fileURL);
                        host = url.getHost();
                        if(list.contains(host)){                //黑名单

                            //这里还要有一些内容，跳转到小宇哥写的网页

                            return;
                        }

                        if (!requestMethod.toLowerCase().equals("get") && !requestMethod.toLowerCase().equals("post")) {
                            if(writename.exists()){
                                writename.delete();
                            }
                            Proxy.count -= 1;
                            flag_for_interrupt_thread = 1;
                            return;
                        }                  //对于请求不是get或者post的数据包，直接舍弃

                        /*                   //有没有缓存
                        for (Map.Entry<String, byte[]> entry : Proxy.header_1.entrySet()) {//header_1是一个键值对为fileurl和完整响应报文的映射表
                            if (entry.getKey().equals(fileURL)){
                                flag_cache = 1;                      //有缓存的标记
                                response = entry.getValue();         //这个for循环判断本地有无缓存
                                break;
                            }
                        }*/
                        iscache();/////////////////////////////////////////////////////////////

                        url_for_sending = url.getFile();//用URL类将文件路径提取出来，成为标准的请求报头，没有主机名
                        HttpVersion = requestHeadline.split(" ")[2];
                    }
                    if (lineNumber > 1) {
                        key = requestHeadline.split(": ")[0].toLowerCase();
                        value = requestHeadline.split(": ")[1].toLowerCase();
                        header.put(key, value);
                    }
                    request.delete(0, request.length());//将StringBuilder清空，以便下一行接受
                }
                if (isEndOfRequest == 3) {//如果收到了3个连续的\r或\n，说明已到请求行末尾，可以结束
                    break;
                }
            } else {//未读到行末的换行符，将读到的字节转换成字符添加到动态StringBuilder中
                isEndOfRequest = 0;
                request.append((char) tempReader);
            }
        }
        header.put("connection", "close");           //need to add this "connection: close"

        outrecord.write("HttpVersion" + ":" + HttpVersion + "\r\n");
        for (Map.Entry<String, String> entry : header.entrySet()) {
            if(entry.getKey().toLowerCase().equals("host") ||
                    entry.getKey().toLowerCase().equals("accept-language") ||
                    entry.getKey().toLowerCase().equals("user-agent")){
                outrecord.write(entry.getKey() + ":" + entry.getValue() + "\r\n");
            }
        }
                         //获得服务器ip
        InetAddress address = InetAddress.getByName(host);
        String ip = address.getHostAddress();
        outrecord.write("IP" + ":" + ip + "\r\n");
        outrecord.write("\r\n");

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
                    break;
                }
            }
        }
        outrecord.flush();
        outrecord.close();
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
            if(requestMethod.toLowerCase().equals("post")){
                writer.print(requestHeadline_2);
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void serverToProxy() throws IOException {
        try{
            if (chatSocket.isClosed()) {//处理socket关闭的异常情况
                System.out.println("Socket is closed");
                return;
            }

            File writename = new File("C:\\Users\\zbh\\Desktop\\cache\\response" + Proxy.count + ".txt");
            //File writename = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\response" + Proxy.count + ".txt");
            writename.createNewFile();
            BufferedWriter outresponse = new BufferedWriter(new FileWriter(writename));
            FileOutputStream fos=new FileOutputStream(writename,true);

            serverInput = new DataInputStream(chatSocket.getInputStream());

            int length;
            byte[] tempByteArray = new byte[1024];
            while ((length = serverInput.read(tempByteArray)) != -1) {
                bo.write(tempByteArray, 0, length);
            }//bo是动态比特数组

            response = bo.toByteArray();
            Proxy.header_1.put(fileURL , response);
            outresponse.write(fileURL + " ");
            fos.write(response);
            outresponse.write("\r\n");
            fos.flush();
            fos.close();
            outresponse.flush();
            outresponse.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private void proxyToClient() throws IOException {
        try {
            BufferedOutputStream writeprint = new BufferedOutputStream(clientSocket.getOutputStream());
            writeprint.write(bo.toByteArray());//传给浏览器比特数组，可以浏览图片和视频
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

    private void iscache() throws IOException {
        try {
            File file=new File("C:\\Users\\zbh\\Desktop\\cache");
            String files[];
            files = file.list();
            int num = files.length;
            System.out.println("XX下文件夹个数："+num);
            int i = 1;
            while(i++ <= num){
                FileInputStream fis = new FileInputStream(files[i]);
                String list = null;
                byte[] bytes = new byte[1024];
                int length = 0;
                while ((length = fis.read(bytes)) != -1) {
                   list += new String(bytes, 0, length);
                }
                String lalala = list.split(" ")[0];
                String lalala_1 = list.split(" ")[1];
                if(lalala.equals(fileURL)){
                    flag_cache = 1;
                    response = lalala_1.getBytes();
                    break;
                }
            }
        }catch (IOException e) {
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
                proxyToClientCache();
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
