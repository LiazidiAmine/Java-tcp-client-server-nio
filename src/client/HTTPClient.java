package client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import com.fasterxml.jackson.*;

public class HTTPClient {
	
	SocketAddress socketAd;
	HTTPReader reader;
	HTTPHeader header;
	
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;
    
    public static final String REQUEST_TASK = "GET Task HTTP/1.1\r\n "
    											+ "Host: ***server address***\r\n "
    											+ "\r\n";
    
    public HTTPClient(String host, int port) {
		this.socketAd = new InetSocketAddress(host, port);
	}
    
    public static String sendTaskRequest(String request, SocketAddress server){
    	
    }
    
    
    
}
