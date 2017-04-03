package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.rmi.UnexpectedException;
import java.security.cert.PKIXRevocationChecker.Option;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;

import client.http.HTTPPacket;
import client.models.Job;
import utils.Utils;
import worker.Worker;
import worker.WorkerFactory;

public class HTTPClient implements Runnable{
	
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;
    private final String host;
    private final int port;
    
    public HTTPClient(String host, int port) {
    	this.host = Objects.requireNonNull(host);
    	this.port = port;
	}
    
    /**
     * 
     * @param
     * @param 
     * @return 
     * @throws IOException
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    public Optional<Job> sendTaskRequest(InetSocketAddress server) throws IOException, IllegalArgumentException, IllegalAccessException{
    	Objects.requireNonNull(server);

    	HTTPPacket packet = new HTTPPacket("GET Task HTTP/1.1",null, HTTPPacket.TYPE_REQUEST);
    	packet.setHost("ns3001004.ip-5-196-73.eu");
    	
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(UTF8_CHARSET.encode(packet.toString()));
		sc.shutdownOutput();

		//String responseTask = "";
        ByteBuffer buffer = ByteBuffer.allocate(48);
        HTTPReader reader = new HTTPReader(sc, buffer);
        HTTPHeader header = reader.readHeader();
        System.out.println(header.toString());
        /*
         * Gerer les erreurs HTTP avec des exceptions
         */
        ByteBuffer content = reader.readBytes(header.getContentLength());
        content.flip();
       
        String json = UTF8_CHARSET.decode(content).toString();
        Job job = Job.fromJson(json);
        
        try {
			preparePost(job, " ", " ", server);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        System.out.println("[CLIENT] GET RESPONSE \n"+job.toJson());
        System.out.println("[CLIENT] ******************** [CLIENT]");
        return Optional.of(job);
    }
    
    
    private Optional<String> preparePost(Job job, String calcul, String error, InetSocketAddress server) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	String fields = mapper.writeValueAsString(job.toMap());
    	JsonNode obj = mapper.valueToTree(fields);
    	HTTPPacket packet = new HTTPPacket("POST Answer HTTP/1.1", obj,HTTPPacket.TYPE_REQUEST);
    	packet.setHost("ns3001004.ip-5-196-73.eu");
    	
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(UTF8_CHARSET.encode(packet.toString()));
		sc.shutdownOutput();
		
		//String responseTask = "";
        ByteBuffer buffer = ByteBuffer.allocate(48);
        HTTPReader reader = new HTTPReader(sc, buffer);
        HTTPHeader header = reader.readHeader();
        
        /*
         * Gerer les erreurs HTTP avec des exceptions
         */
        
        if(header.getContentLength() > 0){
        	ByteBuffer content = reader.readBytes(header.getContentLength());
        	content.flip();
	        String contentS = UTF8_CHARSET.decode(content).toString();
	        JsonNode jsonNode = mapper.valueToTree(contentS);
	        HTTPPacket packetReceive = new HTTPPacket(header.getResponse(), jsonNode, HTTPPacket.TYPE_RESPONSE);
	        System.out.println(packetReceive.toString());
	        return Optional.of(packetReceive.toString());
        }else{
	        HTTPPacket packetReceive = new HTTPPacket(header.getResponse(), null, HTTPPacket.TYPE_RESPONSE);
	        System.out.println(packetReceive.toString());
	        return Optional.of(packetReceive.toString());
        }
        
    }

    // a implementer une fois que toutes les autres fonctions seront ok
	@Override
	public void run() {
		/*
		 * On recupere les informations du worker en envoyant une requete GET
		 * au serveur
		 */
		InetSocketAddress server = new InetSocketAddress("localhost",7777);
		Optional<JsonNode> httpGetResponse;
		
		/*
		 * On lance le worker
		 */
		
		
		
	}
}
