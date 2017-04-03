package client;




import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.*;

import static client.HTTPException.ensure;


/**
 * @author carayol
 *         Class representing a HTTP header
 */

public class HTTPHeader {

    /**
     * Supported versions of the HTTP Protocol
     */

    private static final String[] LIST_SUPPORTED_VERSIONS = new String[]{"HTTP/1.0", "HTTP/1.1", "HTTP/1.2"};
    private static final String[] LIST_METHODS = new String[]{"GET", "POST"};
    public static final Set<String> SUPPORTED_VERSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(LIST_SUPPORTED_VERSIONS)));
    public static final Set<String> SUPPORTED_METHODS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(LIST_METHODS)));
    
    public static final int HEADER_REQUEST = 1;
    public static final int HEADER_RESPONSE = 0;

    private final String firstLine;
    private final String version;
    private int code;
    private final Map<String, String> fields;
    private String path;
    private String method;


    private HTTPHeader(String response,String version,Map<String, String> fields) throws HTTPException {
        this.firstLine = response;
        this.version = version;
        this.fields = Collections.unmodifiableMap(fields);
    }
    
    public static HTTPHeader create(String firstLine, Map<String,String> fields, int header_type) throws HTTPException {
    	Objects.requireNonNull(firstLine);
        switch (header_type){
        	case HEADER_RESPONSE:{
            	String[] tokens = firstLine.split(" ");
                // Treatment of the response line
                ensure(tokens.length >= 2, "Badly formed response:\n" + firstLine);
                Optional<String> version = checkHeader(tokens, SUPPORTED_VERSIONS, firstLine.split(" ")[0]);
                ensure(version.isPresent(), "Unsupported version in response:\n" + firstLine);
                
                Map<String,String> fieldsCopied = new HashMap<>();
                for (String s : fields.keySet())
                    fieldsCopied.put(s,fields.get(s).trim());
                HTTPHeader header = new HTTPHeader(firstLine,version.get(),fieldsCopied);
                
                int code = 0;
                try {
                    code = Integer.valueOf(tokens[1]);
                    ensure(code >= 100 && code < 600, "Invalid code in response:\n" + firstLine);
                    header.code = code;
                } catch (NumberFormatException e) {
                    ensure(false, "Invalid response:\n" + firstLine);
                }
                
                return header;
        	}
        	case HEADER_REQUEST:{
        		String[] tokens = firstLine.split(" ");
        		ensure(tokens.length >= 3, "Badly formed response:\n" + firstLine);
        		Optional<String> method = checkHeader(tokens, SUPPORTED_METHODS, firstLine.split(" ")[0]);
        		Optional<String> version = checkHeader(tokens, SUPPORTED_VERSIONS, firstLine.split(" ")[2]);
        		ensure(method.isPresent(), "Unsupported method in response:\n" + firstLine);
        		ensure(version.isPresent(), "Unsupported version in response:\n" + firstLine);
        		Map<String,String> fieldsCopied = new HashMap<>();
        		for (String s: fields.keySet()){
        			fieldsCopied.put(s, fields.get(s).trim());
        		}
        		HTTPHeader header = new HTTPHeader(firstLine,version.get(),fieldsCopied);
        		header.method = method.get();
        		header.path = tokens[1];
        		return header;
        	}
        	default:{
        		throw new IllegalArgumentException("Bad header type : "+header_type);
        	}
        }
    }
    
    public static Optional<String> checkHeader(String[] line, Set<String> set, String word){
    	for(String s : line){
    		if((s.contains(word)||s.equals(word)) && set.contains(s)){
    			return Optional.of(s);
    		}
    	}
    	return Optional.empty();
    }
    
    public String getResponse() {
        return firstLine;
    }

    public String getVersion() {
        return version;
    }

    public int getCode() {
        return code;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public String getPath(){
    	return path;
    }
    
    public String getMethod(){
    	return method;
    }
    
    /**
     * @return the value of the Content-Length field in the header
     *         -1 if the field does not exists
     * @throws HTTPError when the value of Content-Length is not a number
     */
    public int getContentLength() throws HTTPException {
        String s = fields.get("Content-Length");
        if (s == null) return -1;
        else {
            try {
                return Integer.valueOf(s.trim());
            } catch (NumberFormatException e) {
                throw new HTTPException("Invalid Content-Length field value :\n" + s);
            }
        }
    }

    /**
     * @return the Content-Type
     *         null if there is no Content-Type field
     */
    public String getContentType() {
        String s = fields.get("Content-Type");
        if (s != null) {
            return s.split(";")[0].trim();
        } else
            return null;
    }

    /**
     * @return the charset corresponding to the Content-Type field
     *         null if charset is unknown or unavailable on the JVM
     */
    public Charset getCharset() {
        Charset cs = null;
        String s = fields.get("Content-Type");
        if (s == null) return cs;
        for (String t : s.split(";")) {
            if (t.contains("charset=")) {
                try {
                    cs= Charset.forName(t.split("=")[1].trim());
                } catch (Exception e) {
                   // If the Charset is unknown or unavailable we turn null
                }
                return cs;
            }
        }
        return cs;
    }

    /**
     * @return true if the header correspond to a chunked response
     */
    public boolean isChunkedTransfer() {
        return fields.containsKey("Transfer-Encoding") && fields.get("Transfer-Encoding").trim().equals("chunked");
    }

    public String toString() {
        return firstLine + "\n"
                + fields.toString();
    }
    

}