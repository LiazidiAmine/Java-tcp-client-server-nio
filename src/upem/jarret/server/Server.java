package upem.jarret.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import upem.jarret.http.HTTPReader;

public class Server {

	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	public static final String URL_JOB = "/home/amine/Dev/oasis/src/upem/jarret/server/jobs.txt";
	private static ResponseBuilder responseBuilder;
	private static TaskReader taskReader;
	
	private static class Context {
		private boolean inputClosed = false;
		private final ByteBuffer in = ByteBuffer.allocate(BUF_SIZE);
		private final ByteBuffer out = ByteBuffer.allocate(BUF_SIZE);
		private final SelectionKey key;
		private final SocketChannel sc;
		private long time;

		public Context(SelectionKey key) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.time = 0;
		}

		public void doRead() throws IOException, InterruptedException {
			if (sc.read(in) == -1) {
				inputClosed = true;
			}
			process();
			updateInterestOps();
		}

		public void doWrite() throws IOException, InterruptedException {
			out.flip();
			sc.write(out);
			out.compact();
			process();
			updateInterestOps();
		}

		private void process() throws IOException, InterruptedException {
			in.flip();
			String request = UTF8_CHARSET.decode(in).toString();
			boolean response = processRequest(request);
			if(!response){
				sc.write(UTF8_CHARSET.encode(ResponseBuilder.BAD_REQUEST));
			}
			in.compact();
		}
		
		private boolean processRequest(String request) throws InterruptedException, JsonParseException, JsonMappingException, IOException{
			String[] requestFields = request.split("\r\n");
			String field = requestFields[0];
			if(!field.equals("POST Answer HTTP/1.1") && !field.equals("GET Task HTTP/1.1")){
				return false;
			}
			if(field.equals("POST Answer HTTP/1.1")){
				System.out.println(request);
				ByteBuffer bbOut = UTF8_CHARSET.encode(responseBuilder.post(field));
				out.put(bbOut);
				return true;
			}else if(field.equals("GET Task HTTP/1.1")){
				Optional<String> json = taskReader.getTask();
				ByteBuffer bbHeader = responseBuilder.get(json);
				ByteBuffer bbContent = responseBuilder.getContent(json);
				ByteBuffer bbOut = ByteBuffer.allocate(bbContent.remaining() + bbHeader.remaining());
				bbOut.put(bbHeader).put(bbContent);
				bbOut.flip();
				out.put(bbOut);
				return true;
			}
			return false;
		}
		
		private void updateInterestOps() {
			int ops = 0;
			if (in.hasRemaining() && !inputClosed) {
				ops |= SelectionKey.OP_READ;
			}
			if (out.position() != 0) {
				ops |= SelectionKey.OP_WRITE;
			}
			if (ops == 0) {
				silentlyClose(sc);
			} else {
				key.interestOps(ops);
			}
		}

		private void resetInactiveTime() {
			time = 0;
		}

		private void addInactiveTime(long time, long timeout) {
			this.time += time;
			if (timeout <= this.time) {
				silentlyClose(key.channel());
			}
		}
	}

	private static final int BUF_SIZE = 512;
	private static final int TIMEOUT = 300;
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;
	private final Set<SelectionKey> keys;
	
	private int nbConnection = 0;
	private final Object connectionToken = new Object();
	private SelectionKey selectionKey;

	public Server(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
		keys = selector.keys();
		responseBuilder = ResponseBuilder.getInstance(URL_JOB);
		taskReader = TaskReader.getInstance(URL_JOB);
		
	}

	public void launch() throws IOException, InterruptedException {
		serverSocketChannel.configureBlocking(false);
		selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		 
		while (!Thread.interrupted()) {
			long startLoop = System.currentTimeMillis();
			//System.out.println("Starting select");
			selector.select(TIMEOUT / 10);
			//System.out.println("Select finished");
			printKeys();
			printSelectedKey();
			processSelectedKeys();
			long endLoop = System.currentTimeMillis();
			long timeSpent = endLoop - startLoop;
			updateInactivityKeys(timeSpent);
			selectedKeys.clear();
		}
		

        Thread console = new Thread(()->{
        	Scanner scan = new Scanner(System.in);
        	while(!Thread.interrupted()){
            	while (scan.hasNextLine()) {
                    switch (scan.nextLine()) {
                        case "SHUTDOWN NOW":
                            shutdownNow();
                            break;

                        case "SHUTDOWN":
                            shutdown();
                            break;

                        case "INFO":
                        	info();
                        	break;
                        	
                        default:
                            break;
                    }
                }
        	}
            scan.close();

        });
                    
        console.start();
       
        serverSocketChannel.close();
	}

	private void shutdownNow(){
		try {
			selectionKey.channel().close();
			selectionKey.cancel();
		} catch (IOException e) {
			System.err.println("error in trying to close server's port");
		}
		for (SelectionKey key : selector.keys()) {
			try {
				key.channel().close();
				key.cancel();
				System.out.println("channels closed");
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("error in trying to close each customer's connection");
			}
		}
	}
	
	private void shutdown(){
		try {
			selectionKey.channel().close();
			selectionKey.cancel();
		} catch (IOException e) {
			System.err.println("error in trying to close server's port");
		}
	}
	
	private void info(){
		System.out.println("Nombre de connexions : "+nbConnection);
	}
	
	private void updateInactivityKeys(long timeSpent) {
		// TODO Auto-generated method stub
		for (SelectionKey k : keys) {
			if (!(k.channel() instanceof ServerSocketChannel)) {
				Context cntxt = (Context) k.attachment();
				cntxt.addInactiveTime(timeSpent, TIMEOUT);
			}
		}
	}

	private void printSelectedKey() {
		if (selectedKeys.isEmpty()) {
			//System.out.println("There were not selected keys.");
			return;
		}
		//System.out.println("The selected keys are :");
		for (SelectionKey key : selectedKeys) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				//System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				//System.out.println(
					//	"\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
			}

		}
	}
	
	private void processSelectedKeys() throws IOException, InterruptedException {
		for (SelectionKey key : selectedKeys) {
			
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
				//si acceptable alors il n'est pas en read ou write et le context n'est pas encore attaché
				continue;
				
			}

			Context cntxt = (Context) key.attachment();
			try {

				cntxt.resetInactiveTime();
				if (key.isValid() && key.isWritable()) {
					cntxt.doWrite();
				}
				if (key.isValid() && key.isReadable()) {
					cntxt.doRead();
				}
			} catch (IOException e) {
				silentlyClose(key.channel());
			}

		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		SocketChannel sc = serverSocketChannel.accept();
		if(sc == null){
			return;
		}
		sc.configureBlocking(false);
		SelectionKey clientKey = sc.register(selector, SelectionKey.OP_READ);
		clientKey.attach(new Context(clientKey));
		
		nbConnection++;
	}

	private static void silentlyClose(SelectableChannel sc) {
		if (sc == null)
			return;
		try {
			sc.close();
		} catch (IOException e) {
			// silently ignore
		}
	}

	/***
	 * Theses methods are here to help understanding the behavior of the
	 * selector
	 ***/

	private String interestOpsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		int interestOps = key.interestOps();
		ArrayList<String> list = new ArrayList<>();
		if ((interestOps & SelectionKey.OP_ACCEPT) != 0)
			list.add("OP_ACCEPT");
		if ((interestOps & SelectionKey.OP_READ) != 0)
			list.add("OP_READ");
		if ((interestOps & SelectionKey.OP_WRITE) != 0)
			list.add("OP_WRITE");
		return String.join("|", list);
	}

	public void printKeys() {
		Set<SelectionKey> selectionKeySet = selector.keys();
		if (selectionKeySet.isEmpty()) {
			//System.out.println("The selector contains no key : this should not happen!");
			return;
		}
		//System.out.println("The selector contains:");
		for (SelectionKey key : selectionKeySet) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
			//	System.out.println("\tKey for ServerSocketChannel : " + interestOpsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				//System.out.println("\tKey for Client " + remoteAddressToString(sc) + " : " + interestOpsToString(key));
			}
		}
	}

	private String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (IOException e) {
			return "???";
		}
	}

	private String possibleActionsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		ArrayList<String> list = new ArrayList<>();
		if (key.isAcceptable())
			list.add("ACCEPT");
		if (key.isReadable())
			list.add("READ");
		if (key.isWritable())
			list.add("WRITE");
		return String.join(" and ", list);
	}

}
