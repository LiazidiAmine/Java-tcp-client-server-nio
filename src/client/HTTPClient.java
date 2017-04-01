package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import worker.Worker;
import worker.WorkerFactory;

public class HTTPClient {
	
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;
    private final String host;
    private final int port;
    
    public HTTPClient(String host, int port) {
    	this.host = Objects.requireNonNull(host);
    	this.port = port;
	}
    
    public void sendTaskRequest(String res, InetSocketAddress server) throws IOException{
    	Objects.requireNonNull(res);
    	StringBuilder request = new StringBuilder();
    	request
    		.append("GET ")
    		.append(res)
    		.append(" HTTP/1.1\r\n")
    		.append("Host: ")
    		.append(host)
    		.append("\r\n")
    		.append("\r\n");
    	
    	
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(UTF8_CHARSET.encode(request.toString()));
		sc.shutdownOutput();

		String responseTask = "";
        ByteBuffer buffer = ByteBuffer.allocate(48);

        while (sc.read(buffer) > 0) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                responseTask += (char) buffer.get();
            }
            buffer.clear();
        }

		System.out.println(responseTask);

    }    
    
    public void runWorker() throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
    	String workerUrl = "http://igm.univ-mlv.fr/~carayol/WorkerPrimeV1.jar";
    	String workerClassName = "upem.workerprime.WorkerPrime";
    	
    	Worker worker = WorkerFactory.getWorker(workerUrl, workerClassName);
    	
    	System.out.println(worker.getJobDescription());
    }
}
