package upem.jarret.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

public class ReadLineCRLFServer {

	
	 public static Optional<String> readLineCRLF(ByteBuffer buff) throws IOException {
	      StringBuilder builder = new StringBuilder();
	    	  buff.flip();
	    	  while(buff.hasRemaining()){
	    		  byte currentChar = buff.get();
	    		  builder.append((char)currentChar);
	    		  if(builder.toString().endsWith("\r\n")){
	    			  builder.setLength(builder.length() - 2);
	    			  buff.compact();
	    			  
	    			  return Optional.of(builder.toString());
	    		  }
	    	  }
	    	  buff.position(0);
	    	  buff.compact();
	    	  return Optional.empty();
	      }
}
