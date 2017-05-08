package upem.jarret.server;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Job {
	private final long jobId;
	private final int jobTaskNumber;
	private final String jobDescription;
	private final int jobPriority;
	private final String workerVersionNumber;
	private final String workerURL;
	private final String  workerClassName;
	private final BitSet bitSet;

	/**
	 * contructor
	 * @param jobId
	 * @param jobTaskNumber
	 * @param jobDescription
	 * @param jobPriority
	 * @param workerVersionNumber
	 * @param workerURL
	 * @param workerClassName
	 */
	public Job(long jobId, int jobTaskNumber, String jobDescription, int jobPriority, String workerVersionNumber,
			String workerURL, String workerClassName) {
		this.jobId = jobId;
		this.jobTaskNumber = jobTaskNumber;
		this.jobDescription = jobDescription;
		this.jobPriority = jobPriority;
		this.workerVersionNumber = workerVersionNumber;
		this.workerURL = workerURL;
		this.workerClassName = workerClassName;
		bitSet=new BitSet(jobTaskNumber);
	}

	/**
	 * check who is the first boolean set at false.
	 * @return an boolean
	 */
	public int getIndexOfFalseBitSet(){
		for(int i=0;i<bitSet.size();i++){//bitSet.length();i++){
			if(false==bitSet.get(i)){
				return i;
			}
		}
		return -1;

	}
	
	/**
	 * getter of field jobId
	 * @return long 
	 */
	public long getJobId() {
		return jobId;
	}
	
	/**
	 * getter of field jobid
	 * @return int
	 */
	public int getJobPriority() {
		return jobPriority;
	}
	
	/**
	 * getter of field workerVersionNumber
	 * @return String
	 */
	public String getWorkerVersionNumber() {
		return workerVersionNumber;
	}
	
	/**
	 * getter of field workerURL
	 * @return String
	 */
	public String getWorkerURL() {
		return workerURL;
	}
	/**
	 * 
	 * @return
	 */
	public String getWorkerClassName() {
		return workerClassName;
	}
	
	/**
	 * 
	 * @return
	 * @throws JsonProcessingException
	 */
	Optional<String> getTask() throws JsonProcessingException{
		if(jobIsFinished()){
			System.err.println("job finished -> empty");
			return Optional.empty();
		}
		System.err.println("get taskoptional");
		int task =getIndexOfFalseBitSet();
		if (task<0){
			return Optional.empty();
		}
		HashMap<String,String> map = new HashMap<String,String>();
		map.put("JobId",jobId+"");
		map.put("WorkerVersion", workerVersionNumber+"");
		map.put("WorkerURL", workerURL);
		map.put("WorkerClassName", workerClassName);
		map.put("Task", task+"");
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(map);
		System.err.println("get task" + json +"optional");
		return Optional.of(json);
	}

	/**
	 * check if all task of an job have been done and receive an ack to confirm all run with success
	 * @return boolean
	 */
	public boolean jobIsFinished() {
		return bitSet.cardinality()==bitSet.size();//bitSet.cardinality()==bitSet.length();
	}

	/**
	 * update task
	 * @param task
	 * @return boolean
	 */
	public boolean finishTask(int task) {

		if(task <= bitSet.size() && bitSet.get(task))
			return false;
		bitSet.set(task);
		return true;
	}






}
