package utils;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils {

	public static JsonNode toJson(String jsonString) throws IOException{
		String json = jsonString.substring(jsonString.indexOf("{"));
		ObjectMapper mapper = new ObjectMapper();
		JsonNode obj = mapper.readTree(json);
		
		return obj;
	}
}
