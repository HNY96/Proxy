//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package Proxy;

import Proxy.Proxy;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class RequestThread implements Runnable {
    private Map<String, String> header = new HashMap();
    private BufferedInputStream clientInput;
    private DataInputStream serverInput;
    private Socket clientSocket;
    private Socket chatSocket;
    private String url_for_sending;
    private String requestMethod;
    private String HttpVersion;
    private String fileURL;
    private int flag_cache = 0;
    private int flag_for_interrupt_thread = 0;
    private String response = null;
    byte[] tempByteArray = new byte[1024];
    ByteArrayOutputStream bo = new ByteArrayOutputStream();

    public RequestThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void clientToProxy() throws IOException {
        StringBuilder request = new StringBuilder();
        this.clientInput = new BufferedInputStream(this.clientSocket.getInputStream());
        File writename = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\request" + Proxy.count + ".txt");
        writename.createNewFile();
        BufferedWriter outrequest = new BufferedWriter(new FileWriter(writename));
        int lineNumber = 0;
        int isEndOfRequest = 0;
        new String();
        new String();
        new String();

        int tempReader;
        while((tempReader = this.clientInput.read()) != -1) {
            if((char)tempReader != 13 && (char)tempReader != 10) {
                isEndOfRequest = 0;
                request.append((char)tempReader);
            } else {
                ++isEndOfRequest;
                if(isEndOfRequest == 2) {
                    String flag = request.toString();
                    ++lineNumber;
                    if(lineNumber == 1) {
                        this.requestMethod = flag.split(" ")[0];
                        if(!this.requestMethod.toLowerCase().equals("get") && !this.requestMethod.toLowerCase().equals("post")) {
                            if(writename.exists()) {
                                writename.delete();
                            }

                            --Proxy.count;
                            this.flag_for_interrupt_thread = 1;
                            return;
                        }

                        outrequest.write(flag + "\r\n");
                        this.fileURL = flag.split(" ")[1];
                        Iterator url = Proxy.header_1.entrySet().iterator();

                        while(url.hasNext()) {
                            Entry entry = (Entry)url.next();
                            if(((String)entry.getKey()).equals(this.fileURL)) {
                                this.flag_cache = 1;
                                this.response = (String)entry.getValue();
                                break;
                            }
                        }

                        URL var14 = new URL(this.fileURL);
                        this.url_for_sending = var14.getFile();
                        this.HttpVersion = flag.split(" ")[2];
                    }

                    if(lineNumber > 1) {
                        String key = flag.split(": ")[0].toLowerCase();
                        String value = flag.split(": ")[1].toLowerCase();
                        if(!key.equals("cookie") && !key.equals("accept-encoding") && !key.equals("proxy-connection") && !key.equals("user-agent")) {
                            outrequest.write(flag + "\r\n");
                        }

                        this.header.put(key, value);
                        if(this.header.containsKey("cookie")) {
                            this.header.remove("cookie");
                        }

                        if(this.header.containsKey("accept-encoding")) {
                            this.header.remove("accept-encoding");
                        }

                        if(this.header.containsKey("proxy-connection")) {
                            this.header.remove("proxy-connection");
                        }

                        if(this.header.containsKey("user-agent")) {
                            this.header.remove("user-agent");
                        }
                    }

                    request.delete(0, request.length());
                }

                if(isEndOfRequest == 3) {
                    outrequest.write("\r\n");
                    break;
                }
            }
        }

        this.header.put("connection", "close");
        if(this.requestMethod.toLowerCase().equals("post")) {
            int var13 = 0;

            while((tempReader = this.clientInput.read()) != -1) {
                if((char)tempReader != 13 && (char)tempReader != 10) {
                    var13 = 0;
                    request.append((char)tempReader);
                } else {
                    ++var13;
                    request.append((char)tempReader);
                }

                if(var13 == 4) {
                    String requestHeadline_2 = request.toString();
                    outrequest.write(requestHeadline_2);
                    break;
                }
            }
        }

        outrequest.flush();
        outrequest.close();
    }

    public void proxyToServer() {
        try {
            this.chatSocket = new Socket((String)this.header.get("host"), 80);
            PrintWriter e = new PrintWriter(this.chatSocket.getOutputStream());
            e.print(this.requestMethod + " " + this.url_for_sending + " " + this.HttpVersion + "\r\n");

            File requestAll = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\info\\request_"+header.get("host")+".txt");
            if (!requestAll.exists()) {
                PrintWriter writer = new PrintWriter(requestAll);
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    writer.write(entry.getKey()+": "+entry.getValue()+"\r\n");
                    writer.flush();
                }
                writer.close();
            }

            //System.out.print(this.requestMethod + " " + this.url_for_sending + " " + this.HttpVersion + "\r\n");
            Iterator var2 = this.header.entrySet().iterator();

            while(var2.hasNext()) {
                Entry entry = (Entry)var2.next();
                e.print((String)entry.getKey() + ":" + (String)entry.getValue() + "\r\n");
                //System.out.print((String)entry.getKey() + ":" + (String)entry.getValue() + "\r\n");
            }

            //System.out.println();
            e.print("\r\n");
            e.flush();
        } catch (IOException var4) {
            var4.printStackTrace();
        }

    }

    public void serverToProxy() throws IOException {
        try {
            if(this.chatSocket.isClosed()) {
                System.out.println("Socket is closed");
                return;
            }

            File e;
            BufferedWriter outresponse;
            if(this.response == null) {
                this.serverInput = new DataInputStream(this.chatSocket.getInputStream());
                e = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\response" + Proxy.count + ".txt");
                e.createNewFile();
                outresponse = new BufferedWriter(new FileWriter(e));

                int length;
                while((length = this.serverInput.read(this.tempByteArray)) != -1) {
                    this.bo.write(this.tempByteArray, 0, length);
                }

                this.response = new String(this.bo.toByteArray(), "UTF-8");

                File responseAll = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\info\\response_"+header.get("host")+".txt");
                if (!responseAll.exists()) {
                    PrintWriter writer = new PrintWriter(responseAll);
                    //response是否要提取头文件，过滤掉内容？
                    writer.close();
                }

                System.out.println(this.response);
                outresponse.write(this.response);
                Proxy.header_1.put(this.fileURL, this.response);
                outresponse.flush();
                outresponse.close();
            }

            if(this.response != null) {
                e = new File("C:\\Users\\Ningyu He\\Desktop\\Proxy\\src\\Proxy\\response" + Proxy.count + ".txt");
                e.createNewFile();
                outresponse = new BufferedWriter(new FileWriter(e));
                System.out.println(this.response);
                outresponse.write(this.response);
                Proxy.header_1.put(this.fileURL, this.response);
                outresponse.flush();
                outresponse.close();
            }
        } catch (IOException var4) {
            var4.printStackTrace();
        }

    }

    public void proxyToClient(byte[] temp) throws IOException {
        try {
            BufferedOutputStream e = new BufferedOutputStream(this.clientSocket.getOutputStream());
            e.write(temp);
            e.flush();
            e.close();
        } catch (IOException var3) {
            var3.printStackTrace();
        }

    }

    public void run() {
        try {
            ++Proxy.count;
            this.clientToProxy();
            if(this.flag_for_interrupt_thread == 1) {
                return;
            }

            if(this.flag_cache == 1) {
                this.proxyToClient(this.bo.toByteArray());
            } else if(this.flag_cache == 0) {
                this.proxyToServer();
                this.serverToProxy();
                this.proxyToClient(this.bo.toByteArray());
            }
        } catch (IOException var2) {
            var2.printStackTrace();
        }

    }
}
