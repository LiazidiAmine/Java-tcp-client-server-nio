/*package upem.jarret.client.test;

import java.io.IOException;
import java.net.MalformedURLException;
import org.junit.Test;

import upem.jarret.client.HTTPClient;

public class HTTPClientTest {

	@Test(expected=NullPointerException.class)
	public void requireNonNullHost(){
		new HTTPClient(null, 0, "736");
	}
	
	@Test(expected=NullPointerException.class)
	public void requireNonNullClientId(){
		new HTTPClient("localhost", 0, null);
	}
	
	/*
	@Test(expected=NullPointerException.class)
	public void requireNonNullCheckWorkersJob(){
		HTTPClient client=new HTTPClient("localhost", 0, "111");
		Optional<Worker> workerOp = client.checkWorkers(null);
	}*/
	/*
	@Test(expected=NullPointerException.class)
	public void requireNonNullRunWorker() throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException{
		HTTPClient client=new HTTPClient("localhost", 0, "111");
		client.runWorker(null);
	}
	
	@Test(expected=NullPointerException.class)
	public void requireNonNullSendAnswerAndTaskJson()throws IOException{
		HTTPClient client=new HTTPClient("localhost", 0, "111");
		client.sendAnswerTask(null, "result", "error");
	}
	@Test(expected=NullPointerException.class)
	public void requireNonNullSendAnswerAndTaskResult()throws IOException{
		HTTPClient client=new HTTPClient("localhost", 0, "111");
		client.sendAnswerTask("json", null, "error");
	}
	@Test(expected=NullPointerException.class)
	public void requireNonNullSendAnswerAndTaskError()throws IOException{
		HTTPClient client=new HTTPClient("localhost", 0, "111");
		client.sendAnswerTask("json", "result",null);
	}
}*/
