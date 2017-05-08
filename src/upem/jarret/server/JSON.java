package upem.jarret.server;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JSON {
	
	
	/**
	 * load the config file jarRetConfig.json for a server
	 * @return an instance of a server with config contains in te file
	 * @throws IOException
	 */
	public static Server FactoryServer() throws IOException{
		Path path;
		try {
			path=Paths.get("./config/JarRetConfig.json");
			JsonParser jParser =createJsonParser(path);
			return parsingJsonConfig(jParser);

		} catch (FileSystemNotFoundException fsnfe) {
			Server.logger.debug("[Job] Loading default settings : {}", Server.getLocalCurrentDate());
		}	catch(JsonParseException jpe) {
			Server.logger.debug("[Job] Loading default settings : {}", Server.getLocalCurrentDate());
		} catch (IOException ioe) {
			Server.logger.debug("[Job] Loading default settings : {}", Server.getLocalCurrentDate());
		}
		int port = 8080;
		String logPath = "log/";
		String answersPath = "answers/";
		int maxFileSize = 0;
		int comeBackInSeconds = 300;
		return new Server(port, logPath, answersPath, maxFileSize, comeBackInSeconds);

	}
	/*
	 * We initialize with default value, in case we have wrong json format
	 */
	private static Server parsingJsonConfig(JsonParser jParser) throws IOException {

		int port = 8080;
		String logDirectoryPath = "log/";
		String answersDirectoryPath = "answers/";
		int maxFileSize = 0;
		int comeBackInSeconds = 300;
		jParser.nextToken();
		while (jParser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = jParser.getCurrentName();
			jParser.nextToken();
			switch (fieldName) {
			case "Port":
				port = jParser.getIntValue();
				break;
			case "LogDirectory":
				logDirectoryPath = jParser.getText();
				break;
			case "AnswersDirectory":
				answersDirectoryPath = jParser.getText();
				break;
			case "MaxFileSize":
				maxFileSize = jParser.getIntValue();
				break;
			case "ComeBackInSeconds":
				comeBackInSeconds = jParser.getIntValue();
				break;
			default:
				Server.logger.debug("[JSON] Unrecognized field : {}", Server.getLocalCurrentDate());
				
			}
		}
		return new Server(port, logDirectoryPath, answersDirectoryPath, maxFileSize, comeBackInSeconds);
	}


	private static JsonParser createJsonParser(Path path) throws JsonParseException, IOException {
		JsonFactory jf = new JsonFactory();
		return jf.createParser(Files.newBufferedReader(path));
	}
}
