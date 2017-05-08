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

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {
	
	private static final Logger logger = LoggerFactory.getLogger(Client.class);
	
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;
    private static int TIMEOUT = 300;
    
    private final String host;
    private InetSocketAddress server;
    private final String clientId;
    
    private SocketChannel sc;
    
    private final HashMap<String, Worker> workers = new HashMap<>();
    
    
    public Client(String host, int port, String clientId) throws IOException {
    	this.host = Objects.requireNonNull(host);
    	this.server = new InetSocketAddress(host,port);
    	this.clientId = Objects.requireNonNull(clientId);
    	this.sc = SocketChannel.open();
    	this.sc.connect(server);
	}
    
	private static String getLocalCurrentDate() {
		LocalDate date = new LocalDate();
		return date.toString();
	}
    
    public Optional<String> sendTaskRequest() throws IOException{
    	
    	sc.write(HTTPRequest.getTask(host, UTF8_CHARSET));
    	System.out.println("GET Task request sended");
		ByteBuffer buffer = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc,buffer);
		HTTPHeader header = reader.readHeader();

        if(header.getCode() != 200){
        	sendError();
        	return Optional.empty();
        }
		ByteBuffer content = reader.readBytes(header.getContentLength());
		String json = HTTPRequest.bufferToString(content, UTF8_CHARSET);
		Optional<String> result = HTTPRequest.validGetResponse(json);
		System.out.println("Job received");
		return result;
    }    
    
    
    private Optional<Worker> checkWorkers(Map<String,String> job) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	Worker worker = null;
    	if(!job.containsKey("JobId") || !job.containsKey("WorkerVersion") || !job.containsKey("WorkerURL")
    			|| !job.containsKey("WorkerClassName") || !job.containsKey("Task")){
    		return Optional.empty();
    	}
    	
    	String id = job.get("WorkerClassName")+job.get("WorkerVersion");
    	if(workers.containsKey(id)){
    		worker = workers.get(id);
    		if(worker.getJobId() == Integer.valueOf(job.get("JobId"))){
    			System.out.println("Retrieving worker");
    			return Optional.of(worker);
    		}
    	} else{
        	try {
    			worker = (Worker) WorkerFactory.getWorker(
    					String.valueOf(job.get("WorkerURL")),
    					String.valueOf(job.get("WorkerClassName")));
    			workers.put(worker.getJobId()+""+worker.getClass(), worker);
    			System.out.println("Retrieving worker");
    			return Optional.of(worker);
    		} catch (MalformedURLException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
    			e.printStackTrace();
    		}
    	}
    	
    	return Optional.empty();
    }
    
    public Map<String,String> runWorker(Map<String,String> job) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
    	Worker worker = null;
    	Optional<Worker> workerOp = checkWorkers(job);
    	Map<String,String> map = new HashMap<String,String>();
    	
    	if(!workerOp.isPresent()){
    		logger.debug("[RunWorker] worker error : {}", getLocalCurrentDate());
    		sendError();
    	}else{
        	try{
        		worker = workerOp.get();
        		System.out.println("Computing...");
        		Optional<String> result = Optional.of(worker.compute(Integer.valueOf(job.get("Task"))));
        		if(!result.isPresent() || result == null){
        			map.put("Error", "Comutation error");
        		}else{
        			JsonNode tmp = Utils.toJson(result.get());
            		if(!tmp.asText().equals(result.get())){
            			map.put("Error", "Answer is not valid json");
            		}else if(tmp.has("OBJECT")){
            			map.put("Error", "Answer is nested");
            		}
            	
                	if(checkWorkerResponse(result.get())){
                		map.put("Answer", result.get());
                		logger.debug("[RunWorker] Worker json response valid : {}", getLocalCurrentDate());
                	}else{
                		map.put("Error", "Json format error");
                		logger.debug("[RunWorker] Worker json response error : {}", getLocalCurrentDate());
                	}
        		}
        	}catch(Exception e){
        		e.printStackTrace();
        	}
    	}
    	System.out.println("Compute successful");
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
    		sendError();
    	}
    	ByteBuffer headerPacket = HTTPRequest.getPostHeader(host, UTF8_CHARSET, "application/json", size);
    	ByteBuffer allin = ByteBuffer.allocate(headerPacket.remaining() + total.remaining());
    	allin.put(headerPacket).put(total);
    	allin.flip();

    	sc.write(allin);
		System.out.println("POST Answer sended");
		ByteBuffer buffer = ByteBuffer.allocate(50);
		HTTPReader reader = new HTTPReader(sc,buffer);
		HTTPHeader header = reader.readHeader();
		if(header.getCode() != 200){
        	logger.debug("[SendAnswerTask] Server response error : {}", getLocalCurrentDate());
        	sendError();
        }
		logger.debug("[SendAnswerTask] Requête traitée : {}", getLocalCurrentDate());
    }
    
    private void sendError() throws IOException{
    	String packet = "HTTP 1.1 400 Error";
    	sc.write(UTF8_CHARSET.encode(packet));
    }

	public Client run() throws IOException {
    	try {
    		logger.debug("[RUN] Start running : {}", getLocalCurrentDate());
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
        			while(System.currentTimeMillis() - start <= TIMEOUT);
        			logger.debug("[RUN] Timeout : {}", getLocalCurrentDate());
        			System.out.println("TimeOut");
        			return this.run();
        		}
        		
				Map<String,String> jobMap = Utils.toMap(job.get());
				Map<String, String> workerResponse = runWorker(jobMap);

        		if(workerResponse.size() > 0){
        			if(workerResponse.containsKey("Answer")){
        				sendAnswerTask(job.get(), workerResponse.get("Answer"), null);
        			}else if(workerResponse.containsKey("Error")){
        				sendAnswerTask(job.get(), null, workerResponse.get("Error"));
        			}
        		}
        	}
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | IOException e) {
			e.printStackTrace();
			
		} finally{
			this.sc.close();
		}
    	return null;
	}
	
	public static void main(String[] args){
		logger.debug("[MAIN] Current Date : {}", getLocalCurrentDate());
		Objects.requireNonNull(args);
		if(args.length != 3){
			System.out.println("Arguments non valid. Usage HOST PORT ID");
		}
		String host = args[0];
		int port = Integer.valueOf(args[1]);
		String id = args[2];
		
		try {
			Client client = new Client(host, port, id);
			
			while(!Thread.interrupted()){
				client.run();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}