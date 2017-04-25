package upem.jarret.http.test;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

import upem.jarret.http.HTTPRequest;

public class HTTPRequestTest {

	private static final Charset CS=Charset.forName("UTF-8");

	@Test(expected=NullPointerException.class)
	public void requireNonNullGetTaskHost(){
		HTTPRequest.getTask(null, CS);
	}

	@Test(expected=NullPointerException.class)
	public void requireNonNullgetTasskCharset(){
		HTTPRequest.getTask("localhost",null);
	}

	@Test(expected=NullPointerException.class)
	public void requireNonNullvalidGetResponseJson() throws IOException{
		HTTPRequest.validGetResponse(null);
	}

	@Test(expected=NullPointerException.class)
	public void requireNonNullBufferToString(){
		HTTPRequest.bufferToString(null, CS);
	}

	@Test(expected=NullPointerException.class)
	public void requireNonNullGetPostHeaderHost(){
		HTTPRequest.getPostHeader(null, CS, "contentType", 50);
	}

	@Test(expected=NullPointerException.class)
	public void requireNonNullGetPostHeaderCharset(){
		HTTPRequest.getPostHeader("host", null, "contentType", 50);
	}
	@Test(expected=NullPointerException.class)
	public void requireNonNullGetPostHeaderContentType(){
		HTTPRequest.getPostHeader("host", CS, null, 10);
	}
	@Test(expected=IllegalArgumentException.class)
	public void requireValidGetPostHeaderSize(){
		HTTPRequest.getPostHeader("host", CS, "contentType", -1);
	}

	@Test(expected=NullPointerException.class)
	public void requireNonNullGetTaskInfo(){
		HTTPRequest.getTaskInfo(null);
	}

	@Test(expected=NullPointerException.class)
	public void requireNonNullGetPostContentJsonTask() throws IOException{
		HTTPRequest.getPostContent(null,"error", "answer", "clientId",CS);
	}


}
