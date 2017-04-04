package client.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import client.HTTPException;
import utils.Utils;

public class HttpRequest {

	public static ByteBuffer getTask(String host, Charset cs){
    	StringBuilder request = new StringBuilder();
    	request
    		.append("GET Task HTTP/1.1\r\n")
    		.append("Host: ")
    		.append(host)
    		.append("\r\n")
    		.append("\r\n");
    	return cs.encode(request.toString());
	}
	
	public static boolean validGetResponse(String json) throws IOException{
		Map<String,String> map = Utils.toMap(json);
		if(!map.containsKey("JobId") && !map.containsKey("WorkerVersion")
				&& !map.containsKey("WorkerURL") && !map.containsKey("WorkerClassName") 
				&& !map.containsKey("Task") && !map.containsKey("ComeBackInSeconds")){
			throw new HTTPException("Invalid GET response from server :"+json);
		}else if(map.containsKey("ComeBackInSeconds")){
			return false;
		}else{
			return true;
		}
		
	}
	
	public static String bufferToString(ByteBuffer b, Charset cs){
		b.flip();
		return cs.decode(b).toString();
	}
	
	public static ByteBuffer getPostHeader(String host, Charset cs, String contentType, int size){
    	StringBuilder request = new StringBuilder();
    	request
    		.append("GET Task HTTP/1.1\r\n")
    		.append("Host: "+host+"\r\n")
    		.append("Content-Type: "+contentType+"\r\n")
    		.append("Content-Length: "+size+"\r\n")
    		.append("\r\n");
    	return cs.encode(request.toString());
	}
	
	public static ByteBuffer getTaskInfo(String json){
		try {
			Map<String,String> map = Utils.toMap(json);
			if(!map.containsKey("JobId") || !map.containsKey("Task")){
				throw new HTTPException("Parsing error.."+map.toString());
			}
			long jobId = Long.valueOf(map.get("JobId"));
			int task = Integer.valueOf(map.get("Task"));
			ByteBuffer bbOut = ByteBuffer.allocate(Long.BYTES + Integer.BYTES);
			bbOut.putLong(jobId);
			bbOut.putInt(task);
			bbOut.flip();
			return bbOut;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new IllegalArgumentException("Invalid json : "+json);
	}
	
	public static ByteBuffer getPostContent(String jsonTask, String error, String answer, String clientId, Charset cs) throws IOException{
		Map<String,String> map = Utils.toMap(jsonTask);
		
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		JsonFactory factory = new JsonFactory();
		JsonGenerator generator = factory.createGenerator(byteArray);

		generator.writeStartObject();

		generator.writeStringField("JobId", String.valueOf(map.get("JobId")));
		generator.writeStringField("WorkerVersion", map.get("WorkerVersion"));
		generator.writeStringField("WorkerURL", map.get("WorkerURL"));
		generator.writeStringField("WorkerClassName", map.get("WorkerClassName"));
		generator.writeStringField("Task", String.valueOf(map.get("Task")));
		generator.writeStringField("ClientId", clientId);

		if (error == null) {
			generator.writeFieldName("Answer");
			generator.writeRawValue(answer);
		} else {
			generator.writeStringField("Error", error);
		}

		generator.writeEndObject();
		generator.close();
		
		String json = byteArray.toString();
		ByteBuffer jsonBuffer = cs.encode(json);
		
		return jsonBuffer;
	}
}
