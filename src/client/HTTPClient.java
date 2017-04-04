package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import client.http.HttpRequest;
import utils.Utils;
import worker.Worker;
import worker.WorkerFactory;

public class HTTPClient implements Runnable{
	
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;
    private final String host;
    private InetSocketAddress server;
    
    public HTTPClient(String host, int port) {
    	this.host = Objects.requireNonNull(host);
    	this.server = new InetSocketAddress(host,port);
	}
    
    public Optional<String> sendTaskRequest() throws IOException{

    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(HttpRequest.getTask(host, UTF8_CHARSET));
		sc.shutdownOutput();

		ByteBuffer buffer = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc,buffer);
		HTTPHeader header = reader.readHeader();

        if(header.getCode() != 200){
        	throw new HTTPException("Getting task connection error : "+header.getCode());
        }
        
		ByteBuffer content = reader.readBytes(header.getContentLength());
		String json = HttpRequest.bufferToString(content, UTF8_CHARSET);
		if(!HttpRequest.validGetResponse(json)){
			return Optional.empty();
		}else{
			return Optional.of(json);
		}
    }    
    
    public void runWorker() throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	String workerUrl = "http://igm.univ-mlv.fr/~carayol/WorkerPrimeV1.jar";
    	String workerClassName = "upem.workerprime.WorkerPrime";
    	
    	//Worker worker = WorkerFactory.getWorker(workerUrl, workerClassName);
    	
    	//System.out.println(worker.getJobDescription());
    }
    
    public String sendAnswerTask(String json) throws IOException{
    	Objects.requireNonNull(json);
    	ByteBuffer fields1 = HttpRequest.getPostContent(json, "", "{ \"Prime\" : \"false\", \"Facteur\" : 2}", "Amine", UTF8_CHARSET);
    	ByteBuffer fields2 = HttpRequest.getTaskInfo(json);
    	fields1.flip();
    	fields2.flip();
    	int size = fields1.remaining() + fields2.remaining();
    	if(size > 4096){
    		throw new HTTPException("Packet too big : "+size);
    	}
    	ByteBuffer headerPacket = HttpRequest.getPostHeader(json, UTF8_CHARSET, "application/json", size);

    	
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(headerPacket);
    	sc.write(fields1);
    	sc.write(fields2);
		sc.shutdownOutput();
		
		ByteBuffer buffer = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc,buffer);
		HTTPHeader header = reader.readHeader();
		System.out.println(header.toString());
		if(header.getCode() != 200){
        	throw new HTTPException("Server response error : "+header.getCode());
        }
		
		ByteBuffer content = reader.readBytes(header.getContentLength());
		return HttpRequest.bufferToString(content,UTF8_CHARSET);
    }

    // a implementer une fois que toutes les autres fonctions seront ok
	@Override
	public void run() {
		
	}
}
