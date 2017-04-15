package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseBuilder {
	
	
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
		comeBack.put("ComeBackInSeconds","30000");
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
			if(json.isPresent()){	
				header.append("Content-Length: "+ json.get().length()+"\r\n");
			}else{
				header
					.append("Content-Length: "+content.length()+"\r\n");
			}
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
	
	public String post(String content){
		synchronized(lock){		
			String response = "HTTP/1.1 200 OK\r\n"
							+ "\r\n";

			return response;
		}
	}
}
