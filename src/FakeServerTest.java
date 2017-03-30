import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class FakeServerTest {

	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");	
	private final ServerSocketChannel serverSocketChannel;

	public FakeServerTest(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		System.out.println(this.getClass().getName() 
				+ " bound on " + serverSocketChannel.getLocalAddress());
	}

	public void launch() throws IOException {
		while(!Thread.interrupted()) {
			SocketChannel client = serverSocketChannel.accept();
			System.out.println("Connection accepted from " + client.getRemoteAddress());
			try {
				serve(client);
			} catch (IOException ioe) {
				System.out.println("I/O Error while communicating with client... ");
				ioe.printStackTrace();
			} catch (InterruptedException ie) {
				System.out.println("Server interrupted... ");
				ie.printStackTrace();
				break;
			} finally {
				silentlyClose(client);
			}
		}
	}

	private void serve(SocketChannel sc) throws IOException, InterruptedException {
		String response = "HTTP/1.1 200 OK\r\n"
				+ "Content-type: application/json; charset=utf-8\r\n"
				+ "Content-length: 199\r\n"
				+ "\r\n"
				+ "{"
				+ "\"JobId\": \"23571113\","
				+ "\"WorkerVersion\": \"1.0\","
				+ "\"WorkerURL\": \"http://igm.univ-mlv.fr/~carayol/WorkerPrimeV1.jar\","
				+ "\"WorkerClassName\": \"upem.workerprime.WorkerPrime\","
				+ "\"Task\":\"100\""
				+ "}";
		ByteBuffer bbResponse = UTF8_CHARSET.encode(response);
		bbResponse.flip();
		sc.write(bbResponse);
	}


	private void silentlyClose(SocketChannel sc) {
		if (sc != null) {
			try {
				sc.close();
			} catch (IOException e) {
				// Do nothing
			}
		}
	}

}
