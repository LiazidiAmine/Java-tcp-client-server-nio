import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class FakeServerTest {

	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");	
	private final ServerSocketChannel serverSocketChannel;
	private Selector selector;
	static String responseGetTask = "HTTP/1.1 200 OK\r\n"
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

	public FakeServerTest(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		serverSocketChannel.configureBlocking(false);
		System.err.println(this.getClass().getName() 
				+ " bound on " + serverSocketChannel.getLocalAddress());
		selector = Selector.open();
		serverSocketChannel.register(selector,  SelectionKey.OP_ACCEPT);
	}

	public void launch() throws IOException {
		while(!Thread.interrupted()) {
			int select = selector.select();
			if(select == 0) continue;
			
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator =
		            selectedKeys.iterator(); while (keyIterator.hasNext()) {

		                SelectionKey selectionKey = keyIterator.next();

		                if (selectionKey.isAcceptable()) {
		                    ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();

		                    SocketChannel socketChannel = channel.accept();

		                    socketChannel.configureBlocking(false);
		                    socketChannel.register(selector,
		                    SelectionKey.OP_READ);

		                } else if (selectionKey.isReadable()) {
		                    SocketChannel channel = (SocketChannel) selectionKey.channel();

		                    String request = "";
		                    ByteBuffer buffer = ByteBuffer.allocate(48);

		                    while (channel.read(buffer) > 0) {
		                        buffer.flip();
		                        while (buffer.hasRemaining()) {
		                            request += (char) buffer.get();
		                        }
		                        buffer.clear();
		                    }
		                    System.err.println("Request : " + request);

		                    channel.register(selector, SelectionKey.OP_WRITE, request);

		                } else if (selectionKey.isWritable()) {
		                    SocketChannel channel = (SocketChannel) selectionKey.channel();

		                    String request = (String) selectionKey.attachment();
		                    String[] split = request.split(" ");
		                    String path = split[1];
		                    System.err.println("Path : " + path);

		                    int length = path.getBytes().length;

		                    System.err.println("Response : " + responseGetTask);

		                    ByteBuffer buffer = ByteBuffer.wrap(responseGetTask.getBytes());
		                    channel.write(buffer);
		                    channel.finishConnect();
		                    channel.close();
		                }

		                keyIterator.remove();
		            }
		}
	}

}
