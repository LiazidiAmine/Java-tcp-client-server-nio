package upem.jarret.client;

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

import com.fasterxml.jackson.databind.JsonNode;

import upem.jarret.http.*;
import upem.jarret.utils.Utils;
import upem.jarret.worker.*;

public class HTTPClient {
	
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;
    private final String host;
    private InetSocketAddress server;
    private final HashMap<String, Worker> workers = new HashMap<>();
    private final String clientId;
    private static int TIMEOUT = 0;
    
    public HTTPClient(String host, int port, String clientId) {
    	this.host = Objects.requireNonNull(host);
    	this.server = new InetSocketAddress(host,port);
    	this.clientId = Objects.requireNonNull(clientId);
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
		Optional<String> result = HTTPRequest.validGetResponse(json);
		
		if(result.isPresent()){
			String res = result.get();
			if(res.equals("Ok")){
				return Optional.of(json);
			}else{
				TIMEOUT = Integer.valueOf(res);
				return Optional.empty();
			}
		}else{
			return Optional.empty();
		}
		
    }    
    
    /*pas de test methode private require implicit via runworker*/
    private Optional<Worker> checkWorkers(Map<String,String> job) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	//Objects.requireNonNull(job);
    	String id = job.get("WorkerClassName")+job.get("WorkerVersion");
    	if(workers.containsKey(id)){
    		Worker tmp = workers.get(id);
    		if(tmp.getJobId() == Integer.valueOf(job.get("JobId"))){
    			return Optional.of(tmp);
    		}
    	}
    	return Optional.empty();
    }
    
    public Optional<Map<String,String>> runWorker(Map<String,String> job) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    	Objects.requireNonNull(job);
    	Worker worker = null;
    	String error = "null";
    	Optional<Worker> workerOp = checkWorkers(job);
    	
    	if(workerOp.isPresent()){
    		worker = workerOp.get();
    	}else{
        	try {
    			worker = (Worker) WorkerFactory.getWorker(
    					String.valueOf(job.get("WorkerURL")),
    					String.valueOf(job.get("WorkerClassName")));
    			workers.put(worker.getJobId()+""+worker.getClass(), worker);
    		} catch (MalformedURLException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
    			
    			e.printStackTrace();
    		}
    	}
    	
    	Optional<String> result = Optional.empty();
    	try{
    		result = Optional.of(worker.compute(Integer.valueOf(job.get("Task"))));
    		if(!result.isPresent()){
    			error = "Comutation error";
    			result = Optional.of(" ");
    		}
    		JsonNode tmp = Utils.toJson(result.get());
    		if(!tmp.asText().equals(result.get())){
    			error = "Answer is not valid json";
    		}else if(tmp.has("OBJECT")){
    			error = "Answer is nested";
    		}
    	}catch(Exception e){
    		error = "Computation error";
    	}
    	Map<String,String> map = new HashMap<String,String>();
    	map.put("Answer", result.get());
    	map.put("Error",error);
    	return Optional.of(map);
    }
    
    public void sendAnswerTask(String json, String result, String error) throws IOException{
    	Objects.requireNonNull(json);
    	Objects.requireNonNull(result);
    	Objects.requireNonNull(error);
    	ByteBuffer content = HTTPRequest.getPostContent(json, result, error, this.clientId, UTF8_CHARSET);
    	ByteBuffer task = HTTPRequest.getTaskInfo(json);
    	int size = content.remaining() + task.remaining();
    	ByteBuffer total = ByteBuffer.allocate(size);
    	total.put(task).put(content);
    	total.flip();
    	if(size > 4096){
    		throw new HTTPException("Packet too big : "+size);
    	}
    	ByteBuffer headerPacket = HTTPRequest.getPostHeader(host, UTF8_CHARSET, "application/json", size);
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(headerPacket);
    	sc.write(total);
		sc.shutdownOutput();
		
		ByteBuffer buffer = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc,buffer);
		HTTPHeader header = reader.readHeader();
		if(header.getCode() != 200){
        	System.err.println("Server response error : "+header.getCode());
        }
		System.out.println("[CLIENT]\n");
		System.out.println(header.toString());
		System.out.println("[CLIENT]\n");
    }

    public void run() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		try {
			Optional<String> getResponse = Optional.empty();
			 
			while(!(getResponse = sendTaskRequest()).isPresent()){
				long start = System.currentTimeMillis();
				long end = start + Integer.valueOf(TIMEOUT);
				while(System.currentTimeMillis() < end){};
			}
			String jsonGetResponse = getResponse.get();
			Optional<Map<String,String>> result = runWorker(Utils.toMap(jsonGetResponse));
			
			if(result.isPresent()){
				if(result.get().containsKey("Error") && !result.get().get("Error").equals("null")){
					sendAnswerTask(jsonGetResponse,result.get().get("Error"),null);
				}else if(result.get().containsKey("Answer") && !result.get().get("Answer").equals("")){
					sendAnswerTask(jsonGetResponse,null,result.get().get("Answer"));
				}
			}else{
				System.err.println("Worker error");
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
