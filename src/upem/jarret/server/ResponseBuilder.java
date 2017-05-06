package upem.jarret.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import upem.jarret.utils.Utils;

public class ResponseBuilder {
	
	public static final String BAD_REQUEST = "HTTP/1.1 400 Bad Request\r\n"
												+ "\r\n";
	public static final String OK_REQUEST = "HTTP/1.1 200 OK\r\n"
												+ "\r\n";
	
	private static ResponseBuilder builder = null;
	private final Object lock = new Object();
	
	
	private ResponseBuilder(String url) throws IOException{
		
	}
	
	public static ResponseBuilder getInstance(String url) throws IOException{
		if(builder == null){
			builder = new ResponseBuilder(url);
		}
		return builder;
	}
	
	private String initComeBack() throws JsonProcessingException{
		HashMap<String,String> comeBack = new HashMap<String,String>();
		comeBack.put("ComeBackInSeconds","300");
		ObjectMapper mapper = new ObjectMapper();
		String content = mapper.writeValueAsString(comeBack);
		return content;
	}
	
	public String get(Optional<String> json) throws InterruptedException, JsonParseException, JsonMappingException, IOException{
		synchronized(lock){
			StringBuilder header = new StringBuilder();
			header
				.append("HTTP/1.1 200 OK\r\n")
				.append("Content-type: application/json; charset=utf-8\r\n");
			
			String content = initComeBack();
			if(json.isPresent()){	
				header.append("Content-Length: "+ json.get().length()+"\r\n");
			}else{
				header
					.append("Content-Length: "+content.length()+"\r\n");
			}
			header.append("\r\n");

			return header.toString();
		}
	}
	
	public String getContent(Optional<String> json) throws JsonProcessingException{
		if(json.isPresent()){
			return json.get();
		}else{
			return initComeBack();
		}
	}
	
	public String post(String content) throws JsonParseException, JsonMappingException, IOException{
		/*Map<String,String> mapContent = Utils.toMap(content);
		
		if(mapContent.containsKey("Content-Length") && Integer.valueOf(mapContent.get("Content-Length")) > 0){
			if((!mapContent.containsKey("JobId") || !mapContent.containsKey("WorkerVersion") ||
					!mapContent.containsKey("WorkerURL") || !mapContent.containsKey("WorkerClassName") ||
					!mapContent.containsKey("Task") || !mapContent.containsKey("ClientId"))&& 
					!mapContent.containsKey("ClientId") && !mapContent.containsKey("Error")){
				return BAD_REQUEST;
			}
			
		}*/
		return OK_REQUEST;
	}
	
}
