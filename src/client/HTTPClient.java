package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

public class HTTPClient {
	
	SocketAddress socketAd;
	HTTPReader reader;
	HTTPHeader header;
	
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;
    
    public HTTPClient(String host, int port) {
		this.socketAd = new InetSocketAddress(host, port);
	}
    
    public static void sendTaskRequest(String host, String res, SocketAddress server) throws IOException{
    	Objects.requireNonNull(host);
    	Objects.requireNonNull(res);
    	StringBuilder request = new StringBuilder();
    	request
    		.append("GET ")
    		.append(res)
    		.append(" HTTP/1.1\r\n ")
    		.append("Host: ")
    		.append(host)
    		.append("\r\n")
    		.append("\r\n");
    	
    	
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(UTF8_CHARSET.encode(request.toString()));
    	ByteBuffer bb = ByteBuffer.allocate(50);
    	HTTPReader reader = new HTTPReader(sc, bb);
    	HTTPHeader header = reader.readHeader();
    	Map<String,String> headerFields = header.getFields();
    	
    	if(headerFields.containsKey("Content-Type") 
    			&& headerFields.get("Content-Type").contains("application/json") 
    			&& headerFields.containsKey("Content-Length")){
    		System.out.println(headerFields.get("Content-Length"));
    	}else{
    		System.out.println("no data");
    	}
    }
    
    public static void main(String[] args) throws IOException{
    	HTTPClient client = new HTTPClient("localhost", 7777);
    	SocketAddress server = new InetSocketAddress("localhost",7777);
    	client.sendTaskRequest("localhost", "/", server);
    }
    
    
}
