package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import http.*;
import utils.Utils;
import client.worker.*;

public class HTTPClient {
	
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;
    private final String host;
    private InetSocketAddress server;
    private final HashMap<String, Worker> workers = new HashMap<>();
    private final String clientId;
    
    public HTTPClient(String host, int port, String clientId) {
    	this.host = Objects.requireNonNull(host);
    	this.server = new InetSocketAddress(host,port);
    	this.clientId = clientId;
	}
    
    public Optional<String> sendTaskRequest() throws IOException{

    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(HTTPRequest.getTask(host, UTF8_CHARSET));
		sc.shutdownOutput();

		ByteBuffer buffer = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc,buffer);
		HTTPHeader header = reader.readHeader();

        if(header.getCode() != 200){
        	System.err.println("Getting task connection error : "+header.getCode());
        }
        
		ByteBuffer content = reader.readBytes(header.getContentLength());
		String json = HTTPRequest.bufferToString(content, UTF8_CHARSET);
		String result = HTTPRequest.validGetResponse(json);
		if(!HTTPRequest.validGetResponse(json).equals("Ok")){
			return Optional.of(String.valueOf(result));
		}else{
			System.out.println("Job received :\n"+json+"\n\n");
			return Optional.of(json);
		}
    }    
    
    public String runWorker(Map<String,String> job) {
    	System.out.println("WorkerURL :"+job.get("WorkerURL"));
    	System.out.println("WorkerClassName :"+job.get("WorkerClassName"));
    	
    	Worker worker = null;
    	try {
			worker = WorkerFactory.getWorker(job.get("WorkerURL"), job.get("WorkerClassName"));
		} catch (MalformedURLException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		return worker.compute(Integer.valueOf(job.get("Task")));
		
    }
    
    public void sendAnswerTask(String json, String result, String error) throws IOException{
    	Objects.requireNonNull(json);
    	ByteBuffer fields1 = HTTPRequest.getPostContent(json, result, error, this.clientId, UTF8_CHARSET);
    	ByteBuffer fields2 = HTTPRequest.getTaskInfo(json);
    	fields1.flip();
    	fields2.flip();
    	int size = fields1.remaining() + fields2.remaining();
    	if(size > 4096){
    		throw new HTTPException("Packet too big : "+size);
    	}
    	ByteBuffer headerPacket = HTTPRequest.getPostHeader(json, UTF8_CHARSET, "application/json", size);

    	
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(headerPacket);
    	sc.write(fields1);
    	sc.write(fields2);
		sc.shutdownOutput();
		
		ByteBuffer buffer = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc,buffer);
		HTTPHeader header = reader.readHeader();
		
		if(header.getCode() != 200){
        	System.err.println("Server response error : "+header.getCode());
        }
    }

    public void run() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		try {
			Optional<String> getResponse = Optional.empty();
			 
			while(!(getResponse = sendTaskRequest()).isPresent()){
				long start = System.currentTimeMillis();
				long end = start + Integer.valueOf(getResponse.get());
				while(System.currentTimeMillis() < end){};
			}
			String jsonGetResponse = getResponse.get();
			/*
			 * Jusque là tout est ok
			 */
			String work = runWorker(Utils.toMap(jsonGetResponse));
			System.out.println("WORKER RESPONSE ===> "+work);
			sendAnswerTask(jsonGetResponse,null,work);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
