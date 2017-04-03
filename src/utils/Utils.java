package utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.UnexpectedException;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import client.HTTPHeader;
import client.HTTPReader;

public class Utils {

	private static int CAPACITY = 50;
	
	public static JsonNode toJson(String jsonString) throws IOException{
		//String json = jsonString.substring(jsonString.indexOf("{"));
		ObjectMapper mapper = new ObjectMapper();
		JsonNode obj = mapper.readTree(jsonString);
		
		return obj;
	}
	
	public static Optional<JsonNode> validGetResponse(JsonNode obj) {
		if(!obj.has("JobId") || !obj.has("WorkerVersion") || !obj.has("WorkerURL") 
				|| !obj.has("WorkerClassName") || !obj.has("Task")){
			if(obj.has("ComeBackInSeconds")){
				return Optional.empty();
			}
		}
		return Optional.of(obj);
	}
	
	/**
	 * 
	 * @param sc
	 * @throws IOException
	 */
	public static void validHttpResponse(SocketChannel sc) throws IOException{
		ByteBuffer bbIn = ByteBuffer.allocate(CAPACITY);
		HTTPReader reader = new HTTPReader(sc, bbIn);
		HTTPHeader header = reader.readHeader();
		if(header.getCode() != 200){
			throw new UnexpectedException("Bad answer code : "+header.getCode());
		}
		
		System.out.println("Server response : "+header.getCode());
	}
	
	public static boolean validJsonFormat(String json){
		ObjectMapper objectMapper = new ObjectMapper();
	  	try {
	  		JsonNode jsonNode = objectMapper.readTree(json);
		return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
}
