package proxy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.ZipFile;

/**
 * Created by Ningyu He on 2016/11/25.
 */
public class ServerTest {
    public static void main(String[] args) {

        ServerSocket serverSocket = null;
        Socket sock = null;

        try {
            //建一个文件写入流，向其中写入响应和请求信息
            File writename = new File("D:\\java_project\\src\\proxy\\request.txt");
            writename.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
            File writeresponse = new File("D:\\java_project\\src\\proxy\\response.txt");
            writeresponse.createNewFile();
            BufferedWriter outresponse = new BufferedWriter(new FileWriter(writeresponse));

            //建立一个浏览器到代理的socket
            serverSocket = new ServerSocket(1234);
            sock = serverSocket.accept();

            //把请求报头读到message里
            InputStreamReader stream = new InputStreamReader(sock.getInputStream());
            BufferedReader reader = new BufferedReader(stream);
            String line;
            String message = "";
            boolean isFinish = false;
            while (!isFinish && (line = reader.readLine()) != null) {
                line += "\r\n";
                message += line;
                if (line.equals("\r\n")) {
                    isFinish = true;
                }
            }
            out.write(message);
            out.flush();
            out.close();
            System.out.print(message);

            //提取url
            String url = message.split(" ")[1];
            url = url.substring(7, url.length() - 1);
            System.out.println(url);

            //建立代理服务器到指定主机的socket
            PrintWriter writer = null;
            Socket chatSocket = new Socket(url, 80);
            writer = new PrintWriter(chatSocket.getOutputStream(), true);//强制输出，必须要
            InputStreamReader stream1 = new InputStreamReader(chatSocket.getInputStream());
            BufferedReader reader1 = new BufferedReader(stream1);

            //将请求报文切开，发送请求到指定主机
            int i = message.split("\r\n").length;
            for (int j = 0; j < i; j++) {
                String temp = message.split("\r\n")[j];
                if (temp.contains("Accept-Encoding")) {//必须要这句话，把压缩格式去掉，省掉解压缩的烦恼
                    continue;
                }
                writer.print(temp+"\r\n");
            }
            writer.println("Connection: Close");//大坑！！！！！如果没有这句话对面不会把消息flush，因此收不到响应
            writer.print("\r\n");
            writer.flush();

            //线程关闭两秒，等待服务器响应，拿到服务器返回的响应报头以及html文件
            String line1;
            String message1 = "";
            Thread.sleep(1000);
            if (reader1.ready()) {
                while ((line1 = reader1.readLine()) != null) {
                    line1 += "\r\n";
                    message1 += line1;
                }
            }
            System.out.println(message1);
            outresponse.write(message1);
            outresponse.flush();
            outresponse.close();

            //将html文件和响应报头发送给浏览器
            PrintWriter toBrowser = new PrintWriter(sock.getOutputStream());
            i = message1.split("\r\n").length;
            for (int j = 0; j < i; j++) {
                String temp = message1.split("\r\n")[j];
                toBrowser.println(temp);
            }
            toBrowser.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
