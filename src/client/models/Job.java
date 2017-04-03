package client.models;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

public class Job {
	private String JobId;
	private String WorkerVersion;
	private String WorkerURL;
	private String WorkerClassName;
	private String Task;
	@JsonIgnore
	private String clientId;
	
	@JsonIgnore
	public Job(String jobId, String workerVersion, String workerUrl, String workerClassName, String task) {
		this.JobId = jobId;
		this.WorkerVersion = workerVersion;
		this.WorkerURL = workerUrl;
		this.WorkerClassName = workerClassName;
		this.Task = task;
	}
	
	public Job(){
		
	}
	
	@JsonIgnore
	public static Job fromJson(String json) throws JsonParseException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setVisibilityChecker(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        
        return mapper.readValue(json, Job.class);
	}

	@JsonProperty
	public String getJobId() {
		return JobId;
	}

	@JsonProperty
	public String getWorkerVersion() {
		return WorkerVersion;
	}

	@JsonProperty
	public String getWorkerUrl() {
		return WorkerURL;
	}

	@JsonProperty
	public String getWorkerClassName() {
		return WorkerClassName;
	}

	@JsonProperty
	public String getTask() {
		return Task;
	}
	
	@JsonProperty
	public String getWorkerURL() {
		return WorkerURL;
	}

	@JsonProperty
	public void setWorkerURL(String workerURL) {
		WorkerURL = workerURL;
	}

	@JsonProperty
	public void setJobId(String jobId) {
		JobId = jobId;
	}

	@JsonProperty
	public void setWorkerVersion(String workerVersion) {
		WorkerVersion = workerVersion;
	}

	@JsonProperty
	public void setWorkerClassName(String workerClassName) {
		WorkerClassName = workerClassName;
	}

	@JsonProperty
	public void setTask(String task) {
		Task = task;
	}

	@JsonIgnore
	public String toJson() throws JsonProcessingException{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
	
	@JsonIgnore
	public void setClientId(String clientId){
		this.clientId = clientId;
	}
	
	@JsonIgnore
	public Map<String,String> toMap() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
		/*
		 * trouver un moyen de parser un objet en Map
		 */
		Map<String,String> map = new HashMap<String,String>();
		map.put("JobId", JobId);
		map.put("WorkerVersion", WorkerVersion);
		map.put("WorkerURL", WorkerURL);
		map.put("WorkerClassName", WorkerClassName);
		map.put("Task", Task);
		map.put("ClientId", clientId);
			
		return map; 
	}
}
