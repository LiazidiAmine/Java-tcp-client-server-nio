package upem.jarret.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	
	public ByteBuffer get(Optional<String> json) throws InterruptedException, JsonParseException, JsonMappingException, IOException{
		synchronized(lock){
			StringBuilder header = new StringBuilder();
			header
				.append("HTTP/1.1 200 OK\r\n")
				.append("Content-type: application/json; charset=utf-8\r\n");
			
			String content = initComeBack();
			int size = 0;
			if(json.isPresent()){	
				size = Charset.forName("utf-8").encode(json.get()).remaining()+Long.BYTES+Integer.BYTES;
			}else{
				size = Charset.forName("utf-8").encode(content).remaining();
			}
			header.append("Content-Length: "+ size +"\r\n");
			header.append("\r\n");
			ByteBuffer headerBuff = Server.UTF8_CHARSET.encode(header.toString());
			
			return headerBuff;
		}
	}
	
	public ByteBuffer getContent(Optional<String> json) throws JsonProcessingException{
		String content = initComeBack();
		ByteBuffer contentBuff;
		if(json.isPresent()){
			contentBuff = Server.UTF8_CHARSET.encode(json.get());
		}else{
			contentBuff = Server.UTF8_CHARSET.encode(content);
		}
		return contentBuff;
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
		System.out.println(content);
		return OK_REQUEST;
	}
	
}
