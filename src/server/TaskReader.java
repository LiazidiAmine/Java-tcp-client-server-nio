package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import utils.Utils;

public class TaskReader {

	public static TaskReader instance = null;
	private final String url;
	private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(100);
	
	private TaskReader(String url) throws IOException{
		this.url = url;
		init();
	}
	
	public static TaskReader getInstance(String url) throws IOException{
		if(instance == null){
			instance = new TaskReader(url);
		}
		return instance;
	}
	
	private void init() throws IOException{
		String content = new String(Files.readAllBytes(Paths.get(url)));
		String[] jsons = content.split("\n\n");
		Arrays.asList(jsons).forEach(x->{
			try {
				queue.put(x);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}
	
	public Optional<String> getTask() throws InterruptedException, JsonParseException, JsonMappingException, IOException{
		synchronized(queue){
			String task = null;
			if((task = queue.poll(300, TimeUnit.MILLISECONDS)) == null){
				return Optional.empty();
			}
			return parseTask(task);
				
		}
	}
	
	public boolean checkTask(String content){
		
		return true;
	}
	
	private Optional<String> parseTask(String task) throws JsonParseException, JsonMappingException, IOException{
		JsonNode node = Utils.toJson(task);
		if(node.has("JobId") && node.has("WorkerVersion") 
				&& node.has("WorkerURL") && node.has("WorkerClassName") && node.has("Task")){
			StringBuilder json = new StringBuilder();
			json.append("{")
				.append("JobId:"+node.get("JobId")+",")
				.append("WorkerVersion:"+node.get("WorkerVersionNumber")+",")
				.append("WorkerURL:"+node.get("WorkerURL")+",")
				.append("WorkerClassName:"+node.get("WorkerClassName")+",")
				.append("Task:"+node.get("JobTaskNumber"))
				.append("}");
			
			return Optional.of(json.toString());
		}
		return Optional.empty();
	}
}
