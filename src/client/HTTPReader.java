package tcp.tp8.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


public class HTTPReader {

    private final Charset ASCII_CHARSET = Charset.forName("ASCII");
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
        Map<String,String> fields = new HashMap<String,String>();
        String firstLine = "";
        String line;
        while(!(line = readLineCRLF()).equals("")){
        	if(!line.contains(":")){
        		firstLine = line;
        	}else{
        		String[] array = line.split(": ");
        		if(fields.containsKey(array[0])){
        			String value = fields.get(array[0]);
        			StringBuilder builder = new StringBuilder();
        			builder.append(value).append(array[1]);
        		}
        		fields.put(array[0], array[1]);
        	}
        }
        for(Map.Entry<String, String> e:fields.entrySet()){
        	System.out.println(e.getKey()+" "+e.getValue());
        }
        return HTTPHeader.create(firstLine, fields);
    }

    /**
     * @param size
     * @return a ByteBuffer in write-mode containing size bytes read on the socket
     * @throws IOException HTTPException is the connection is closed before all bytes could be read
     */
    public ByteBuffer readBytes(int size) throws IOException {
        buff.flip();
        int i = 0;
        ByteBuffer bbOut = ByteBuffer.allocate(size);
        while(buff.hasRemaining() && i<size){
        	byte b = buff.get();
        	bbOut.put(b);
        	i++;
        }

        bbOut.flip();
        return bbOut;
    }

    /**
     * @return a ByteBuffer in write-mode containing a content read in chunks mode
     * @throws IOException HTTPException if the connection is closed before the end of the chunks
     *                     if chunks are ill-formed
     */

    public ByteBuffer readChunks() throws IOException {
        buff.flip();
        int size = buff.remaining();
        ByteBuffer bbResponse = readBytes(size);
        bbResponse.flip();
        
        return bbResponse;
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
