package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
		Map<String,String> node = Utils.toMap(task);
		if(node.containsKey("JobId") && node.containsKey("WorkerVersionNumber") && node.containsKey("JobPriority")
				&& node.containsKey("WorkerURL") && node.containsKey("WorkerClassName") 
				&& node.containsKey("JobTaskNumber") && node.containsKey("JobDescription")){
			HashMap<String,String> map = new HashMap<String,String>();
			map.put("JobId", node.get("JobId"));
			map.put("WorkerVersion", node.get("WorkerVersionNumber"));
			map.put("WorkerURL", node.get("WorkerURL"));
			map.put("WorkerClassName", node.get("WorkerClassName"));
			map.put("Task", node.get("JobTaskNumber"));
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(map);
			
			return Optional.of(json);
		}
		return Optional.empty();
	}
}
