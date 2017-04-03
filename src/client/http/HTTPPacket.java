package client.http;

import java.util.HashMap;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import client.HTTPException;
import client.HTTPHeader;

public class HTTPPacket {
	public static final int TYPE_REQUEST = 1;
	public static final int TYPE_RESPONSE = 0;
	
	private final HTTPHeader header;
	private JsonNode content;
	private final int packet_type;
	private String host;
	
	public HTTPPacket(HTTPHeader header, JsonNode content, int packet_type){
		this.header = Objects.requireNonNull(header);
		this.content = content;
		this.packet_type = packet_type;
	}
	
	public HTTPPacket(String firstLine, JsonNode content, int packet_type) throws HTTPException{
		this.header = HTTPHeader.create(firstLine, new HashMap<String,String>(), packet_type);
		this.content = content;
		this.packet_type = packet_type;
	}
	
	public HTTPHeader getHeader(){
		return header;
	}
	
	public JsonNode getContent(){
		return content;
	}
	
	public int type(){
		return packet_type;
	}
	
	public String getHost(){
		return host;
	}
	
	public void setHost(String host){
		if(this.packet_type != TYPE_REQUEST){
			throw new IllegalStateException("Not a request packet. Request type : "+this.packet_type);
		}
		this.host = host;
	}
	
    public String encodedHeader() {
		if(this.packet_type != TYPE_REQUEST){
			throw new IllegalStateException("Not a request packet. Request type : "+this.packet_type);
		}
    	StringBuilder packet = new StringBuilder();
    	packet
    		.append(header.getResponse() +"\r\n")
    		.append("Host: "+ host +"\r\n");
    	header.getFields().forEach((k,v)->{
    		System.out.println(k);
    		packet
    			.append(k+": ")
    			.append(v+"\r\n");
    	});
    	packet.append("\r\n");
    	return packet.toString();
    }
	
	@Override
	public String toString(){
		if(content != null){
			return this.encodedHeader() +""
					+ this.content.asText();
		}else{
			return this.header.toString()+
					"Host: "+host;
		}
		
	}
}
