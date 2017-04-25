package upem.jarret.http;



import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;


public class HTTPReader {

    private final SocketChannel sc;
    private final ByteBuffer buff;

    public HTTPReader(SocketChannel sc, ByteBuffer buff) {
        this.sc = sc;
        this.buff = buff;
    }

    /**
     * @return The ASCII string terminated by CRLF
     * <p>
     * The method assume that buff is in write mode and leave it in write-mode
     * The method never reads from the socket as long as the buffer is not empty
     * @throws IOException HTTPException if the connection is closed before a line could be read
     */
    public String readLineCRLF() throws IOException {
      StringBuilder builder = new StringBuilder();
      while(true){
    	  buff.flip();
    	  while(buff.hasRemaining()){
    		  byte currentChar = buff.get();
    		  builder.append((char)currentChar);
    		  if(builder.toString().endsWith("\r\n")){
    			  builder.setLength(builder.length() - 2);
    			  buff.compact();
    			  
    			  return builder.toString();
    		  }
    	  }
    	  buff.clear();
    	  if(-1 == sc.read(buff)){
    		  throw new HTTPException();
    	  }
      }
    }

    /**
     * @return The HTTPHeader object corresponding to the header read
     * @throws IOException HTTPException if the connection is closed before a header could be read
     *                     if the header is ill-formed
     */
    /**
     * @return
     * @throws IOException
     */
    public HTTPHeader readHeader() throws IOException {
		HashMap<String, String> map= new HashMap<>();
		String firstLine=readLineCRLF();
		String nextLine=readLineCRLF();
		while(!nextLine.equals("")){
			String[] token=nextLine.split(":",2);
			if(map.containsKey(token[0])){
				String contains = map.get(token[0]);
				String concat = contains + ";" + token[1];
				map.replace(token[0], concat);
			}
			map.put(token[0], token[1]);
			nextLine=readLineCRLF();
		}
		return HTTPHeader.create(firstLine, map);
    }

    /**
     * @param size
     * @return a ByteBuffer in write-mode containing size bytes read on the socket
     * @throws IOException HTTPException is the connection is closed before all bytes could be read
     */
    public ByteBuffer readBytes(int size) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(size);
		buff.flip();
		int oldLimitBuff = buff.limit();
		if(buff.hasRemaining()){
			if(buff.remaining() > bb.remaining())
				buff.limit(size);
			bb.put(buff);
			buff.limit(oldLimitBuff);
			buff.compact();
		}
		if(bb.hasRemaining())
			if(!readFully(sc, bb))
				throw new HTTPException();
		return bb;
    }

    /**
     * @return a ByteBuffer in write-mode containing a content read in chunks mode
     * @throws IOException HTTPException if the connection is closed before the end of the chunks
     *                     if chunks are ill-formed
     */

    public ByteBuffer readChunks() throws IOException {
    	return readBytes(Integer.parseInt(readLineCRLF(), 16)+2);
    }
    
	static boolean readFully(SocketChannel su,ByteBuffer bb) throws IOException{
		while(-1 != su.read(bb)){
			if(!bb.hasRemaining()){
				return true;
			}
		}
		return false;
	}


    public static void main(String[] args) throws IOException {
        Charset charsetASCII = Charset.forName("ASCII");
        String request = "GET / HTTP/1.1\r\n"
                + "Host: www.google.com\r\n"
                + "\r\n";
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("www.google.com", 80));
        sc.write(charsetASCII.encode(request));
        ByteBuffer bb = ByteBuffer.allocate(50);
        HTTPReader reader = new HTTPReader(sc, bb);
        System.out.println(reader.readLineCRLF());
        System.out.println(reader.readLineCRLF());
        System.out.println(reader.readLineCRLF());
        sc.close();

        bb = ByteBuffer.allocate(50);
        sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("www.google.com", 80));
        reader = new HTTPReader(sc, bb);
        sc.write(charsetASCII.encode(request));
        System.out.println(reader.readHeader());
        sc.close();

        bb = ByteBuffer.allocate(50);
        sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("www.google.com", 80));
        reader = new HTTPReader(sc, bb);
        sc.write(charsetASCII.encode(request));
        HTTPHeader header = reader.readHeader();
        System.out.println(header);
        ByteBuffer content = reader.readBytes(header.getContentLength());
        content.flip();
        System.out.println(header.getCharset().decode(content));
        sc.close();

        bb = ByteBuffer.allocate(50);
        request = "GET / HTTP/1.1\r\n"
                + "Host: www.google.com\r\n"
                + "\r\n";
        sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("www.google.com", 80));
        reader = new HTTPReader(sc, bb);
        sc.write(charsetASCII.encode(request));
        header = reader.readHeader();
        System.out.println(header);
        content = reader.readChunks();
        content.flip();
        System.out.println(header.getCharset().decode(content));
        sc.close();
    }
}
