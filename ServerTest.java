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
                line += "\n";
                message += line;
                if (line.equals("\n")) {
//                    message = message.substring(0, message.length()-2);
                    isFinish = true;
                }
            }
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
            int i = message.split("\n").length;
            for (int j = 0; j < i; j++) {
                String temp = message.split("\n")[j];
                writer.println(temp);
            }
            writer.println("Connection: Close");//大坑！！！！！
            writer.println();

            //线程关闭两秒，等待服务器响应，拿到服务器返回的响应报头以及html文件
            String line1;
            String message1 = "";
            Thread.sleep(2000);
            if (reader1.ready()) {
                while ((line1 = reader1.readLine()) != null) {
                    line1 += "\n";
                    message1 += line1;
                }
            }
            System.out.println(message1);

            //将html文件和响应报头发送给浏览器
            PrintWriter toBrowser = new PrintWriter(sock.getOutputStream());
//            i = message1.split("\n").length;
//            for (int j = 0; j < i; j++) {
//                String temp = message1.split("\n")[j];
//                toBrowser.println(temp);
//            }
            toBrowser.print("HTTP/1.1 200 OK\r\n");
//            toBrowser.println("Cache-Control: no-cache");
//            toBrowser.println("Pragma: no-cache");
//            toBrowser.println("Content-Type: text/html; charset=utf-8");
//            toBrowser.println("Content-Encoding:");
//            toBrowser.println("Expires: -1");
//            toBrowser.println("Vary: Accept-Encoding");
//            toBrowser.println("Server: Microsoft-IIS/8.5");
//            toBrowser.println("X-AspNetMvc-Version: 5.1");
//            toBrowser.println("X-AspNet-Version: 4.0.30319");
//            toBrowser.println("X-Powered-By: ASP.NET");
//            toBrowser.println("Date: Fri, 25 Nov 2016 12:32:11 GMT");
//            toBrowser.println("Connection: close");
//            toBrowser.println("Content-Length: 0");
            toBrowser.print("Content-Type: text/html\r\n");
            toBrowser.println();
            toBrowser.print("Hello");
            toBrowser.println();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
