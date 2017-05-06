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
    
    private SocketChannel sc;
    
    private final HashMap<String, Worker> workers = new HashMap<>();
    
    
    public HTTPClient(String host, int port, String clientId) throws IOException {
    	this.host = Objects.requireNonNull(host);
    	this.server = new InetSocketAddress(host,port);
    	this.clientId = Objects.requireNonNull(clientId);
    	this.sc = SocketChannel.open();
    	this.sc.connect(server);
	}
    
    public Optional<String> sendTaskRequest() throws IOException{
    	
    	sc.write(HTTPRequest.getTask(host, UTF8_CHARSET));
		//sc.shutdownOutput();
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
    
    public Map<String,String> runWorker(Map<String,String> job) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    	Worker worker = null;
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
    		if(!result.isPresent() || result == null){
    			map.put("Error", "Comutation error");
    		}else{
    			JsonNode tmp = Utils.toJson(result.get());
        		if(!tmp.asText().equals(result.get())){
        			map.put("Error", "Answer is not valid json");
        			//Si la réponse est imprbriqué, un champ OBJECT apparait dans le JsonNode
        		}else if(tmp.has("OBJECT")){
        			map.put("Error", "Answer is nested");
        		}
        	
            	if(checkWorkerResponse(result.get())){
            		map.put("Answer", result.get());
            		System.err.println("[CLIENT] Réponse du worker recupérée, Job ID #"+job.get("JobId"));
            	}else{
            		map.put("Error", "Json format error");
            		System.err.println("[CLIENT] JSON FORMAT ERROR");
            	}
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	
    	return map;
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

    	sc.write(allin);
    	//sc.write(total);
		//sc.shutdownOutput();
		
		ByteBuffer buffer = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc,buffer);
		HTTPHeader header = reader.readHeader();
		if(header.getCode() != 200){
        	System.err.println("Server response error : "+header.getCode());
        }
		System.err.println("[CLIENT] Requête traitée "+header.getResponse());
    }

	public HTTPClient run() throws IOException {
    	try {  		
    		long start = System.currentTimeMillis();
    		
    		if(!sc.isOpen()){
        		this.sc = SocketChannel.open();
        	}
    		if(!sc.isConnected()){
        		this.sc.connect(server);
        	}
    		
    		Optional<String> job = sendTaskRequest();

        	if(job.isPresent()){
        		if(job.get().equals("ComeBackInSeconds")){
        			System.err.println("[CLIENT] Come back");
        			while(System.currentTimeMillis() - start <= TIMEOUT);
        			return this.run();
        		}
        		
				Map<String,String> jobMap = Utils.toMap(job.get());
				System.err.println("[CLIENT] JobId recupéré : "+jobMap.get("JobId"));
				Map<String, String> workerResponse = runWorker(jobMap);

        		if(workerResponse.size() > 0){
        			System.err.println("\n[CLIENT] Worker response valid \n"+workerResponse.toString());
        			if(workerResponse.containsKey("Answer")){
        				sendAnswerTask(job.get(), workerResponse.get("Answer"), null);
        			}else if(workerResponse.containsKey("Error")){
        				sendAnswerTask(job.get(), null, workerResponse.get("Error"));
        			}
        		}
        	}
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} finally{
			this.sc.close();
		}
    	return null;
	}
}
