package upem.jarret.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import upem.jarret.client.Client;
import upem.jarret.utils.Utils;



public class Server {

	public static final Logger logger = LoggerFactory.getLogger(Server.class);
	
	private static class Context {
		private boolean inputClosed = false;

		private final ByteBuffer in = ByteBuffer.allocate(BUF_SIZE);
		private final ByteBuffer out = ByteBuffer.allocate(BUF_SIZE);
		private final SelectionKey key;
		private final SocketChannel sc;
		private long time;
		private HTTPHeaderServer head = null;

		public Context(SelectionKey key) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.time = 0;

		}

		/**
		 * @throws IOException
		 * @throws InterruptedException
		 */
		public void doRead() throws IOException, InterruptedException {
			if (sc.read(in) == -1) {
				inputClosed = true;
			}
			process();
			updateInterestOps();
		}

		/**
		 * 
		 * @throws IOException
		 * @throws InterruptedException
		 */
		public void doWrite() throws IOException, InterruptedException {
			out.flip();
			sc.write(out);
			out.compact();
			process();
			updateInterestOps();
		}

		private void process() throws IOException, InterruptedException {

			if(head != null){
				processRequest(null);
				return;
			}

			Optional<String> opt=ReadLineCRLFServer.readHeader(in);
			if(!opt.isPresent()){
				return;
			}
			boolean response = processRequest(opt.get());
			if(!response){
				logger.debug("[Process] worker error : {}", getLocalCurrentDate());
				ByteBuffer bb=Charset.forName("ASCII").encode(ResponseBuilder.BAD_REQUEST);
				out.put(bb);
			}

		}

		private boolean processRequest(String request) throws InterruptedException, JsonParseException, JsonMappingException, IOException{
			if(head == null){
				String[] requestFields = request.split("\r\n");
				String field = requestFields[0];
				HashMap<String, String> map= new HashMap<>();
				for(int i =1 ; i < requestFields.length;i++ ){
					String[] token=requestFields[i].split(":",2);
					if(map.containsKey(token[0])){
						String contains = map.get(token[0]);
						String concat = contains + ";" + token[1];
						map.replace(token[0], concat);
					}
					map.put(token[0], token[1]);
				}
				head = HTTPHeaderServer.create(field, map);
			}
			if(head.getCode().equals("GET")){
				logger.debug("[process request] GET Request : {}", getLocalCurrentDate());
				Optional<String> json =  taskReader.getTask();
				if(!json.isPresent()){// a transformer en if else avec en else un ajout du paquet ComeBackInSeconds
					json =  taskReader.getTask();
					Thread.sleep(1000);
				}

				ByteBuffer bbHeader = responseBuilder.get(json);
				ByteBuffer bbContent = responseBuilder.getContent(json);
				ByteBuffer bbOut = ByteBuffer.allocate(bbContent.remaining() + bbHeader.remaining());
				bbOut.put(bbHeader).put(bbContent);
				bbOut.flip();
				out.put(bbOut);
				head = null;
				return true;
			}
			else
				if(head.getCode().equals("POST")){
					
					logger.debug("[process request] POST Request : {}", getLocalCurrentDate());
					int size = head.getContentLength();
					
					Optional<ByteBuffer> bb = ReadLineCRLFServer.readBytes(size, in);
					if(!bb.isPresent()){
						System.out.println("no present");
						logger.debug("[process request] need more data : {}", getLocalCurrentDate());
						Thread.sleep(3000);
						return false;
					}
					ByteBuffer b = bb.get();
					b.flip();
					long jobId = b.getLong();
					int task = b.getInt();
					String msg = UTF8_CHARSET.decode(b).toString();
					taskReader.taskFinish(jobId, task, msg);
					String response = responseBuilder.post(msg);
					ByteBuffer bbOut = UTF8_CHARSET.encode(response);
					out.put(bbOut);
					head = null;
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
				StringBuilder sb = new StringBuilder();
				key.interestOps(ops);
				if ((ops & SelectionKey.OP_ACCEPT) != 0)
					sb.append("OP_ACCEPT");
				if ((ops & SelectionKey.OP_READ) != 0)
					sb.append("OP_READ");
				if ((ops & SelectionKey.OP_WRITE) != 0)
					sb.append("OP_WRITE");
				
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

	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	public static final String URL_JOB = "./jobs.txt";
	private static ResponseBuilder responseBuilder;
	private static TaskReader taskReader;

	private static final int BUF_SIZE = 4096;;
	private static final int TIMEOUT = 1000 * 1000;
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;
	private final Set<SelectionKey> keys;

	private SelectionKey selectionKey;
	private String logDirectoryPath;
	private String answerDirectoryPath;
	private int maxFileSize;
	private int comeBackInSeconds;
	private static final LinkedBlockingDeque<String> command = new LinkedBlockingDeque<>();
	/**
	 * old constructor depreciated
	 * @param port
	 * @throws IOException
	 */
	public Server(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
		keys = selector.keys();
		responseBuilder = ResponseBuilder.getInstance(URL_JOB);
		taskReader = TaskReader.getInstance(URL_JOB);
	}
	/**
	 * contructor
	 * @param port
	 * @param logPath
	 * @param answersPath
	 * @param maxFileSize
	 * @param comeBackInSeconds
	 * @throws IOException
	 */
	Server(int port, String logPath, String answersPath, int maxFileSize, int comeBackInSeconds) throws IOException {
		this.logDirectoryPath = logPath;
		this.answerDirectoryPath = answersPath;
		this.maxFileSize = maxFileSize;
		this.comeBackInSeconds = comeBackInSeconds;
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
		serverSocketChannel.configureBlocking(false);
		selectionKey=serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		keys = selector.keys();
		responseBuilder = ResponseBuilder.getInstance(URL_JOB);
		taskReader = TaskReader.getInstance(URL_JOB);
	}
	
	/**
	 * run the shell to interact with the server, and run the server
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void launch() throws IOException, InterruptedException {
		serverSocketChannel.configureBlocking(false);
		//selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		Thread console = new Thread(()->{
			Scanner scan = new Scanner(System.in);
			while(!Thread.interrupted()){
				while (scan.hasNextLine()) {
					switch (scan.nextLine()) {
					case "SHUTDOWN NOW":
						command.add("SHUTDOWN NOW");
						scan.close();
						Thread.currentThread().interrupt();
						break;
					case "SHUTDOWN":
						command.add("SHUTDOWN");
						break;
					case "INFO":
						command.add("INFO");
						break;
					default:
						break;
					}
				}
			}
			scan.close();

		});

		console.start();

		while (!Thread.interrupted()) {
			long startLoop = System.currentTimeMillis();
			selector.select(TIMEOUT / 1000);
			while (!command.isEmpty()) {
				switch (command.removeFirst()) {
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

			processSelectedKeys();
			long endLoop = System.currentTimeMillis();
			long timeSpent = endLoop - startLoop;
			updateInactivityKeys(timeSpent);
			selectedKeys.clear();
		}

		console.interrupt();
		serverSocketChannel.close();
	}

	private void shutdownNow(){

		shutdown();
		for (SelectionKey key : selector.keys()) {
			silentlyClose(key.channel());
			System.out.println("channels closed");
		}
	}

	private void shutdown(){
		silentlyClose(serverSocketChannel);
		for(SelectionKey key : keys)
			if(key.channel() instanceof ServerSocketChannel)
				silentlyClose(key.channel());
	}

	private void info(){
		System.out.println("Nombre de connexions : "+keys.size());
	}

	private void updateInactivityKeys(long timeSpent) {
		for (SelectionKey k : keys) {
			if (!(k.channel() instanceof ServerSocketChannel)) {
				Context cntxt = (Context) k.attachment();
				cntxt.addInactiveTime(timeSpent, TIMEOUT);
			}
		}
	}

	private void processSelectedKeys() throws IOException, InterruptedException {
		for (SelectionKey key : selectedKeys) {

			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
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
	
	public static String getLocalCurrentDate() {
		LocalDate date = new LocalDate();
		return date.toString();
	}
	
	public static void main(String[] args){
		Server s;
		try {
			s = JSON.FactoryServer();
			s.launch();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
