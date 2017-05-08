package upem.jarret.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import upem.jarret.utils.Utils;

public class TaskReader {

	public static TaskReader instance = null;
	private final String url;
	private final Map<Job,Integer> map=new HashMap<>();
	private TaskReader(String url) throws IOException{
		this.url = url;
		init();
	}
	
	/**
	 * Singleton: need only one instance.
	 * @param url
	 * @return one instance of TaskReader.
	 * @throws IOException
	 */
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
				Map<String,String> node = Utils.toMap(x);
				if(node.containsKey("JobId") 
						&& node.containsKey("WorkerVersionNumber") && node.containsKey("JobPriority")
						&& node.containsKey("WorkerURL") && node.containsKey("WorkerClassName") 
						&& node.containsKey("JobTaskNumber") && node.containsKey("JobDescription")
						&& !node.get("JobPriority").equals("0")){
					map.put(new Job(Long.parseLong(node.get("JobId")),
							Integer.parseInt(node.get("JobTaskNumber")),
							node.get("JobDescription"),
							Integer.parseInt(node.get("JobPriority")),
							node.get("WorkerVersionNumber"),
							node.get("WorkerURL"),
							node.get("WorkerClassName")),
							Integer.parseInt(node.get("JobPriority")));
					System.err.println("-------------\n"+Integer.parseInt(node.get("JobPriority")));
					System.err.println("-------\n"+node.get("JobPriority"));

				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	/**
	 * Take à task from the Set of Job and sennd it to a client
	 * @return if a job is available return Optional of this one, else that return an empty Optional.
	 * @throws InterruptedException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public Optional<String> getTask() throws InterruptedException, JsonParseException, JsonMappingException, IOException{
		Set<Job> keys=map.keySet();
		System.err.println("get task -TaskReader-");
		for(Job b:keys){
			System.err.println("get task -TaskReader-"+b);
			if(map.get(b)>0){
				System.err.println("get task -TaskReader----------------"+map.get(b));
				map.put(b,map.get(b)-1);
				return b.getTask();
			}
		}
		for(Job b:keys){
			map.put(b, b.getJobPriority());
		}
		for(Job b:keys){
			System.err.println("get task -TaskReader-"+b);
			if(map.get(b)>0){
				System.err.println("get task -TaskReader----------------"+map.get(b));
				map.put(b,map.get(b)-1);
				return b.getTask();
			}
		}

		//renvoyer job a faire


		return Optional.empty();
	
	}
	/**
	 * 
	 * @param content
	 * @return
	 */
	public boolean checkTask(String content){

		return true;
	}
	/***
	 * 
	 * @param jobId
	 * @param task
	 * @param msg
	 */
	public void taskFinish(long jobId, int task, String msg) {
		Set<Job> keys = map.keySet();
		for(Job b:keys){
			if(b.getJobId() != jobId)
				continue;
			if(b.finishTask(task)){
				//write answer contenu dans messaeg
			}
		}
	}

}
