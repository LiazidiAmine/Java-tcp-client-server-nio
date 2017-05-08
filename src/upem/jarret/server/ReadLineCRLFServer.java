package upem.jarret.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

public class ReadLineCRLFServer {

	
	/**
	 * read the header 
	 * @param buff
	 * @return
	 * @throws IOException
	 */
	public static Optional<String> readHeader(ByteBuffer buff) throws IOException {
		StringBuilder builder = new StringBuilder();
		buff.flip();
		while(buff.hasRemaining()){
			byte currentChar = buff.get();
			builder.append((char)currentChar);
			if(builder.toString().endsWith("\r\n\r\n")){
				//builder.setLength(builder.length() - 4);
				buff.compact();

				return Optional.of(builder.toString());
			}
		}
		buff.position(0);
		buff.compact();
		return Optional.empty();
	}
	
	/***
	 * 
	 * @param size
	 * @param buff
	 * @return
	 * @throws IOException
	 */
	  public static Optional<ByteBuffer> readBytes(int size, ByteBuffer buff) throws IOException {
			ByteBuffer bb = ByteBuffer.allocate(size);
			buff.flip();
			int oldLimitBuff = buff.limit();
			if(buff.hasRemaining()){
				if(buff.remaining() > bb.remaining())
					buff.limit(size);
				bb.put(buff);
				buff.limit(oldLimitBuff);
				buff.compact();
				return Optional.of(bb);
			}
			return Optional.empty();
	    }
}
