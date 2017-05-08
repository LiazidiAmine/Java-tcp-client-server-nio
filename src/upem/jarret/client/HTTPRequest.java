package upem.jarret.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import upem.jarret.utils.Utils;

public class HTTPRequest {

	public static ByteBuffer getTask(String host, Charset cs){
		Objects.requireNonNull(host);
		Objects.requireNonNull(cs);
    	StringBuilder request = new StringBuilder();
    	request
    		.append("GET Task HTTP/1.1\r\n")
    		.append("Host: ")
    		.append(host)
    		.append("\r\n")
    		.append("\r\n");

    	return cs.encode(request.toString());
	}
	
	public static Optional<String> validGetResponse(String json) throws IOException{
		Objects.requireNonNull(json);
		Map<String,String> map = Utils.toMap(json);
		
		if(!map.containsKey("JobId") && !map.containsKey("WorkerVersion")
				&& !map.containsKey("WorkerURL") && !map.containsKey("WorkerClassName") 
				&& !map.containsKey("Task") && !map.containsKey("ComeBackInSeconds")){
		
			return Optional.empty();
		
		}else if(map.containsKey("ComeBackInSeconds")){
			System.out.println(map.get("ComeBackInSeconds"));
			return Optional.of(map.get("ComeBackInSeconds"));
		
		}else{
			return Optional.of(json);
		}
		
	}
	
	public static String bufferToString(ByteBuffer b, Charset cs){
		Objects.requireNonNull(b);
		Objects.requireNonNull(cs);
		b.flip();
		return cs.decode(b).toString();
	}
	
	public static ByteBuffer getPostHeader(String host, Charset cs, String contentType, int size){
		Objects.requireNonNull(host);
		Objects.requireNonNull(cs);
		Objects.requireNonNull(contentType);
		requirePositiveStrictValue(size);
    	StringBuilder request = new StringBuilder();
    	request
    		.append("POST Answer HTTP/1.1\r\n")
    		.append("Host: "+host+"\r\n")
    		.append("Content-Type: "+contentType+"\r\n")
    		.append("Content-Length: "+size+"\r\n")
    		.append("\r\n");
    	return cs.encode(request.toString());
	}
	
	public static ByteBuffer getTaskInfo(String json){
		Objects.requireNonNull(json);
		try {
			Map<String,String> map = Utils.toMap(json);
			if(!map.containsKey("JobId") || !map.containsKey("Task")){
				
				//throw new HTTPException("Parsing error.."+map.toString());
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
	
	public static int requirePositiveStrictValue(int a){
		if(a <= 0){
			throw new IllegalArgumentException("need to be positive");
		}
		return a;
	}
	
	public static ByteBuffer getPostContent(String jsonTask, String error, String answer, String clientId, Charset cs) throws IOException{
		Map<String,String> map = Utils.toMap(Objects.requireNonNull(jsonTask));
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		JsonFactory factory = new JsonFactory();
		JsonGenerator generator = factory.createGenerator(byteArray);

		generator.writeStartObject();

		generator.writeStringField("JobId", String.valueOf(map.get("JobId")));
		generator.writeStringField("WorkerVersion", map.get("WorkerVersion"));
		generator.writeStringField("WorkerURL", map.get("WorkerURL"));
		generator.writeStringField("WorkerClassName", map.get("WorkerClassName"));
		generator.writeStringField("Task", String.valueOf(map.get("Task")));
		generator.writeStringField("ClientId", Objects.requireNonNull(clientId));

		if (error == null) {
			generator.writeFieldName("Answer");
			generator.writeRawValue(answer);
		} else {
			generator.writeStringField("Error",error);
		}

		generator.writeEndObject();
		generator.close();
		
		String json = byteArray.toString();
		ByteBuffer jsonBuffer = cs.encode(json);
		
		return jsonBuffer;
	}
}