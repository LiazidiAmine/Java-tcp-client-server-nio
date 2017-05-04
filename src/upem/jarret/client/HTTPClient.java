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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import upem.jarret.http.*;
import upem.jarret.utils.Utils;
import upem.jarret.worker.*;

public class HTTPClient {
	
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;
    private static int TIMEOUT = 300;
    
    private final String host;
    private InetSocketAddress server;
    private final String clientId;
    
    private final HashMap<String, Worker> workers = new HashMap<>();
    
    
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
        	return Optional.empty();
        }
		ByteBuffer content = reader.readBytes(header.getContentLength());
		String json = HTTPRequest.bufferToString(content, UTF8_CHARSET);
		Optional<String> result = HTTPRequest.validGetResponse(json);
		System.out.println(result);
		return result;
    }    
    
    
    private Optional<Worker> checkWorkers(Map<String,String> job) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	if(!job.containsKey("JobId") || !job.containsKey("WorkerVersion") || !job.containsKey("WorkerURL")
    			|| !job.containsKey("WorkerClassName") || !job.containsKey("Task")){
    		return Optional.empty();
    	}
    	
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
    	Worker worker = null;
    	String error = "null";
    	Optional<Worker> workerOp = checkWorkers(job);
    	Map<String,String> map = new HashMap<String,String>();
    	
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
    	
    	try{
    		Optional<String> result = Optional.of(worker.compute(Integer.valueOf(job.get("Task"))));
    		if(!result.isPresent()){
    			error = "Comutation error";
    			result = Optional.empty();
    		}
    		
    		JsonNode tmp = Utils.toJson(result.get());
    		if(!tmp.asText().equals(result.get())){
    			error = "Answer is not valid json";
    			//Si la réponse est imprbriqué, un champ OBJECT apparait dans le JsonNode
    		}else if(tmp.has("OBJECT")){
    			error = "Answer is nested";
    		}
    	
        	if(result.isPresent() && checkWorkerResponse(result.get())){
        		map.put("Answer", result.get());
        		System.err.println("[CLIENT] Réponse du worker recupérée, Job ID #"+job.get("JobId"));
        	}else{
        		map.put("Answer", "");
        	}
        	
        	map.put("Error",error);
    	}catch(Exception e){
    		error = "Computation error";
    	}
    	
    	return Optional.of(map);
    }
    
    private boolean checkWorkerResponse(String json) throws JsonParseException, JsonMappingException, IOException{
    	Map<String,String> map = Utils.toMap(json);
    	if(map != null){
    		return true;
    	}
    	return false;
    }
    
    public void sendAnswerTask(String json, String result, String error) throws IOException{
    	Objects.requireNonNull(json);
    	ByteBuffer content = HTTPRequest.getPostContent(json, result, error, this.clientId, UTF8_CHARSET);
    	ByteBuffer task = HTTPRequest.getTaskInfo(json);
    	int size = content.remaining() + task.remaining();
    	ByteBuffer total = ByteBuffer.allocate(size);
    	total.put(task).put(content);
    	total.flip();
    	if(size > 4096){
    		//sendErrorPacket(PACKET_TOO_BIG);
    	}
    	ByteBuffer headerPacket = HTTPRequest.getPostHeader(host, UTF8_CHARSET, "application/json", size);
    	ByteBuffer allin = ByteBuffer.allocate(headerPacket.remaining() + total.remaining());
    	allin.put(headerPacket).put(total);
    	allin.flip();
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(allin);
    	//sc.write(total);
		sc.shutdownOutput();
		
		ByteBuffer buffer = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc,buffer);
		HTTPHeader header = reader.readHeader();
		if(header.getCode() != 200){
        	System.err.println("Server response error : "+header.getCode());
        }
		System.err.println("[CLIENT] Requête traitée "+header.getResponse());
    }

    public void run() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		try {
			Optional<String> getResponse = sendTaskRequest();
			long start = System.currentTimeMillis(); 
			while(true){
				if(getResponse.isPresent()){
					break;
				}else{
					while(System.currentTimeMillis() < start + TIMEOUT){};
					getResponse = sendTaskRequest();
				}
			}
			String jsonGetResponse = getResponse.get();
			Optional<Map<String,String>> result = runWorker(Utils.toMap(jsonGetResponse));
			System.out.println("worker out");
			if(result.isPresent()){
				if(result.get().containsKey("Error") && !result.get().get("Error").equals("null")){
					sendAnswerTask(jsonGetResponse,result.get().get("Error"),null);
				}else if(result.get().containsKey("Answer") && !result.get().get("Answer").equals("")){
					System.err.println("[CLIENT] Worker answer : "+result.get().get("Answer"));
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
