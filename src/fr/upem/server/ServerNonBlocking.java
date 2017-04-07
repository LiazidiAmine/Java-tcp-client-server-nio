package fr.upem.server;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class ServerNonBlocking {

	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;

	/*
	 * Need to be synchronized due to be shared by scannerThread and 
	 * updated in selectedKeys.
	 */
	private int nbConnection=0;
	private final Object connectionToken=new Object();

	private final SelectionKey selectionKey;
	private final String logDirectoryPath;
	private final String answerDirectoryPath;
	private final int maxFileSize;
	private final int comeBackInSeconds;


	public ServerNonBlocking(int port, String logPath, String answersPath, int maxFileSize, int comeBackInSeconds) throws IOException {
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
	}

	/**********************************************************************/
	/**********************************************************************/
	/**********************************************************************/
	/*
	 * Manage Console Option
	 */
	private final Thread console = new Thread(() -> {
		try (Scanner scanner = new Scanner(System.in)) {
			while (scanner.hasNextLine()) {
				switch (scanner.nextLine()) {
				case "SHUTDOWN":
					shutDown();
					break;
				case "SHUTDOWN NOW":
					shutDownNow();
					break;
				case "INFO":
					//info();
					break;
				default:
					System.out.println("unkown instruction");
					break;
				}
			}
		}
	});
	
	private void shutDown() {
		try {
			selectionKey.channel().close();
			selectionKey.cancel();
		} catch (IOException e) {
			System.err.println("error in trying to close server's port");
		}

	}


	private void shutDownNow() {
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
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("error in trying to close each customer's connection");
			}
		}
	}

	private void info(){
		synchronized (connectionToken) {
			System.out.println("there are "+nbConnection+" connected");
		}
	}
	/**********************************************************************/
	/**********************************************************************/
	/**********************************************************************/



	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		while (!Thread.interrupted()) {
			printKeys();
			System.out.println("Starting select");
			selector.select();
			System.out.println("Select finished");
			printSelectedKey();
			processSelectedKeys();
			selectedKeys.clear();
		}
	}

	
	/**********************************************************************/
	/**********************************************************************/
	/**********************************************************************/
	/*One thread to manage all connection*/
	
	private void processSelectedKeys() {
		for (SelectionKey key : selectedKeys) {
			if (key.isValid() && key.isAcceptable()) {
				try {
					doAccept(key);
				} catch (IOException e){
					//Connection refused
					e.printStackTrace();
				}
			}
			if (key.isValid() && key.isWritable()) {
				doWrite(key);

			}
			if (key.isValid() && key.isReadable()) {
				doRead(key);
			}
		}
	}



	private void doAccept(SelectionKey key) throws IOException {
		SocketChannel sc=serverSocketChannel.accept();
		if(sc == null){
			//doNothing
			/*may return null if someone try to connect the serverSocketChannel*/ 
			return;
		}
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_READ, new Attachment(sc));
		synchronized(connectionToken){
			nbConnection++;
		}
	}

	/*
	 * No readfully in nonblocking
	 */
	private void doRead(SelectionKey key) {
		SocketChannel sc = (SocketChannel) key.channel();
		
		//HTTPReaderServer reader = attachment.getReader();
		key.interestOps(SelectionKey.OP_WRITE);
	}

	private void doWrite(SelectionKey key) {
		// TODO
		key.interestOps(SelectionKey.OP_READ);
	}
	/**********************************************************************/
	/**********************************************************************/
	/**********************************************************************/

	
	private static ServerNonBlocking FactoryServer() throws IOException{
		Path path;
		try {
			path=Paths.get("config/JarRetConfig.json");
			JsonParser jParser =createJsonParser(path);
			return parsingJson(jParser);
			
		} catch (FileSystemNotFoundException fsnfe) {
			System.err.println("can't load JarRetConfig.json");
			System.err.println("Load default Settings");
		}	catch(JsonParseException jpe) {
			System.err.println("can't read JarRetConfig.json");
			System.err.println("Load default Settings");
		} catch (IOException ioe) {
			System.err.println("can't load JarRetConfig.json");
			System.err.println("Load default Settings");
		}
		int port = 8080;
		String logPath = "log/";
		String answersPath = "answers/";
		int maxFileSize = 0;
		int comeBackInSeconds = 300;
		return new ServerNonBlocking(port, logPath, answersPath, maxFileSize, comeBackInSeconds);
		
	}
	/*
	 * We initialize with default value, in case we have wrong json format
	 */
	private static ServerNonBlocking parsingJson(JsonParser jParser) throws IOException {
		
		int port = 8080;
		String logDirectoryPath = "log/";
		String answersDirectoryPath = "answers/";
		int maxFileSize = 0;
		int comeBackInSeconds = 300;
		jParser.nextToken();
		while (jParser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = jParser.getCurrentName();
			jParser.nextToken();
			switch (fieldName) {
			case "Port":
				port = jParser.getIntValue();
				break;
			case "LogDirectory":
				logDirectoryPath = jParser.getText();
				break;
			case "AnswersDirectory":
				answersDirectoryPath = jParser.getText();
				break;
			case "MaxFileSize":
				maxFileSize = jParser.getIntValue();
				break;
			case "ComeBackInSeconds":
				comeBackInSeconds = jParser.getIntValue();
				break;
			default:
				System.err.println("Unrecognize field");
			}
		}
		return new ServerNonBlocking(port, logDirectoryPath, answersDirectoryPath, maxFileSize, comeBackInSeconds);
	}


	private static JsonParser createJsonParser(Path path) throws JsonParseException, IOException {
		JsonFactory jf = new JsonFactory();
		return jf.createParser(Files.newBufferedReader(path));
	}


	public static void main(String[] args) throws NumberFormatException, IOException {
		ServerNonBlocking server=FactoryServer();
		server.launch();
	}


	/***
	 *  Theses methods are here to help understanding the behavior of the selector
	 ***/

	private String interestOpsToString(SelectionKey key){
		if (!key.isValid()) {
			return "CANCELLED";
		}
		int interestOps = key.interestOps();
		ArrayList<String> list = new ArrayList<>();
		if ((interestOps&SelectionKey.OP_ACCEPT)!=0) list.add("OP_ACCEPT");
		if ((interestOps&SelectionKey.OP_READ)!=0) list.add("OP_READ");
		if ((interestOps&SelectionKey.OP_WRITE)!=0) list.add("OP_WRITE");
		return String.join("|",list);
	}

	public void printKeys() {
		Set<SelectionKey> selectionKeySet = selector.keys();
		if (selectionKeySet.isEmpty()) {
			System.out.println("The selector contains no key : this should not happen!");
			return;
		}
		System.out.println("The selector contains:");
		for (SelectionKey key : selectionKeySet){
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				System.out.println("\tKey for ServerSocketChannel : "+ interestOpsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				System.out.println("\tKey for Client "+ remoteAddressToString(sc) +" : "+ interestOpsToString(key));
			}


		}
	}

	private String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (IOException e){
			return "???";
		}
	}

	private void printSelectedKey() {
		if (selectedKeys.isEmpty()) {
			System.out.println("There were not selected keys.");
			return;
		}
		System.out.println("The selected keys are :");
		for (SelectionKey key : selectedKeys) {
			SelectableChannel channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
			} else {
				SocketChannel sc = (SocketChannel) channel;
				System.out.println("\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
			}

		}
	}

	private String possibleActionsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		ArrayList<String> list = new ArrayList<>();
		if (key.isAcceptable()) list.add("ACCEPT");
		if (key.isReadable()) list.add("READ");
		if (key.isWritable()) list.add("WRITE");
		return String.join(" and ",list);
	}
}