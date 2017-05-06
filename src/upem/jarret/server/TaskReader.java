package upem.jarret.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import upem.jarret.utils.Utils;

public class TaskReader {

	public static TaskReader instance = null;
	private final String url;
	private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(4);
	
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
		System.out.println(MessageFormat.format(Server.MSG_TEMPLATE, "Loading all tasks from config"));
	}
	
	public Optional<String> getTask() throws InterruptedException, JsonParseException, JsonMappingException, IOException{
		synchronized(queue){
			String task = null;
			if((task = queue.poll()) == null){
				return Optional.empty();
			}
			System.out.println(MessageFormat.format(Server.MSG_TEMPLATE, "Getting a new task"));
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
			
			System.out.println(MessageFormat.format(Server.MSG_TEMPLATE, "Parsing task"));
			return Optional.of(json);
		}
		return Optional.empty();
	}
}
