import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.ParseException;

public class PartialHTTP1Server implements Runnable {

	private byte[] payload;
	private Socket clientSocket;
	private boolean returnPayload;

	PartialHTTP1Server (Socket clientSocket) {
		this.clientSocket = clientSocket;
	}


	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("Server needs port number");
			return;
		}
		int port = 0;
		String holder = "";
		try{
			holder = args[0];
			if(holder.length() < 1 ){
				System.out.println("Server needs port number");
				return;
			}
		}
		catch(NullPointerException e){
			System.out.println("Server needs port number");
			return;
		}
		try{
			port = Integer.parseInt(holder);
			ServerSocket sersock;
			if(port == -1){
				System.out.println("Server needs port number");
				return;
			}
			else{
				ExecutorService threads = new ThreadPoolExecutor(5, 50, 5000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
				try{
					sersock = new ServerSocket(port);

					while(true) {
						//When a client connects, spawn a new thread to handle it
						Socket sock = sersock.accept();
						System.out.println("Server Started");
						try {
							threads.execute(new PartialHTTP1Server(sock));
						} catch (RejectedExecutionException rej) {
							try {
								PrintWriter outToClient = new PrintWriter(sock.getOutputStream(), true);
								outToClient.println("503 Service Unavailable");
								outToClient.close();
								sersock.close();
							} catch (IOException f) {
								System.out.println("Error handling client input.");
							}
						}
						//new Thread(new SimpleHTTPServer(sock)).start();
					}
				} catch (IOException e) {
					System.out.println("Error accepting connection.");
				}
			}


		}
		catch (NumberFormatException e){
			System.out.println("Server needs port number");
			return;
		}
	}




	/* method that parses request and returns the correct response
	   @Param request is the request from client
	   @Param modify is the modifyDate
	 **/
	private String response(String request, String modifyDate, String from, String userAgent, String contentType, String contentLength, String param) {
		if (request == null) {
			return "HTTP/1.0 400 Bad Request\r\n";
		}

		StringTokenizer tokens = new StringTokenizer(request); //tokenize by SPACE

		if (tokens.countTokens() != 3) {
			return "HTTP/1.0 400 Bad Request\r\n";
		}

		boolean ifModifiedSince = false;
		if(modifyDate.contains("If-Modified-Since")){
			ifModifiedSince = true;
		}
		String method = tokens.nextToken();

		//parse file part of request e.g. /index.html
		String file = tokens.nextToken();
		String filePath = "." + file;
		Path path = Paths.get(filePath);

		StringTokenizer files = new StringTokenizer(file, "/"); //tokenize by /
		int numTokens = files.countTokens();
		String fileName = "";
		String folder = "";
		String folder2 = "";
		String fn = "";
		String fileExt = "";

		//if requested resource is not in folder
		if (numTokens == 1) {
			fileName = files.nextToken(); //get file token
			if(fileName.indexOf(".")!=-1){
				StringTokenizer fileParse = new StringTokenizer(fileName, ".");
				fn = fileParse.nextToken();
				fileExt = fileParse.nextToken();
			}
			else{
				fn = fileName;
			}
		}

		//if requested resource is located in folder
		if (numTokens == 2) {
			folder = files.nextToken();
			fileName = files.nextToken(); //get file token

			if(fileName.indexOf(".")!=-1){
				StringTokenizer fileParse = new StringTokenizer(fileName, ".");
				fn = fileParse.nextToken();
				fileExt = fileParse.nextToken();
			}
			else{
				fn = fileName;
			}

		}


		//if requested resource is located in resources folder
		if (numTokens == 3) {

			folder = files.nextToken();
			folder2 = files.nextToken();
			fileName = files.nextToken(); //get file token

			if(fileName.indexOf(".")!=-1){
				StringTokenizer fileParse = new StringTokenizer(fileName, ".");
				fn = fileParse.nextToken();
				fileExt = fileParse.nextToken();
			}
			else{
				fn = fileName;
			}

		}
		//parse protocol to get protocol name and version
		String protocol = tokens.nextToken();
		StringTokenizer protocolData = new StringTokenizer(protocol, "/");
		String http = protocolData.nextToken();
		String version = protocolData.nextToken();
		double versionInt = Double.parseDouble(version);

		//400 Bad Request
		//if no version is specified...
		if (protocol.length() != 8) {
			//System.out.println("yes");
			return "HTTP/1.0 400 Bad Request\r\n";

		}

		if ((method.equals("get") || method.equals("head") || method.equals("post")) && version.equals("1.0")) {
			return "HTTP/1.0 400 Bad Request\r\n";
		}

		if (method.equals("KICK") && version.equals("1.0")) {
			return "HTTP/1.0 400 Bad Request\r\n";
		}

		if (((!method.equals("GET") && (!method.equals("POST") && (!method.equals("HEAD"))))
					&& (!version.equals("1.0")))) {
			return "HTTP/1.0 400 Bad Request\r\n";
		}

		//check if valid request but no implementation
		if (!(method.equals("GET")) && !(method.equals("POST")) && !(method.equals("HEAD")) && (version.equals("1.0"))) {
			if(method.equals("DELETE") || method.equals("PUT") || method.equals("LINK") || method.equals("UNLINK")){
				return "HTTP/1.0 501 Not Implemented\r\n";
			}
			return "HTTP/1.0 400 Bad Request\r\n";
		}


		//general bad requests

		//check if version # is 1.0--if greater than 1.0,
		//respond with "505 HTTP Version Not Supported"
		if (((method.equals("GET")) || (method.equals("POST")) || (method.equals("HEAD"))) ){
			if (versionInt > 1.0 ) {
				return "HTTP/1.0 505 HTTP Version Not Supported\r\n";
			}
			if(versionInt < 1.0){
				return "HTTP/1.0 400 Bad Request\r\n";
			}
		}

		boolean inDir =  false;
		File resource = new File("."+ file);
		System.out.println("IN DIR FILE NAME " + resource.getName());

		if(resource.isFile()){
			inDir = true;
		}
		//check if the file is in the directory as a cgi for POST commands
		if (method.equals("POST")){
			System.out.println("file value " + file);
			String tmp = file.substring(0,file.indexOf("."));
			
			String file2 = tmp + ".cgi";
			System.out.println("file2 value " + file2);
			File resource2 = new File("./" + file2);
			if(resource2.isFile()){
				inDir = true;
			}
		}

		String head;

		if (file.contains("secret") || file.contains("forbidden")) {
			return "HTTP/1.0 403 Forbidden\r\n";
		}

		// Perform HEAD command
		if (method.equals("HEAD") && inDir) {
			String status = "HTTP/1.0 200 OK\r\n";
			String allow = "Allow: GET, POST, HEAD\r\n";
			head = status + allow + getHeader(resource, fileExt);
			return head;
		}

		//Perform GET command
		//If the request includes an "If-Modified-Since" field, Perform a Conditional GET.
		if ((method.equals("GET") && inDir)) {
			SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
			f.setTimeZone(TimeZone.getTimeZone("GMT"));

			if(ifModifiedSince == true){
				//Check when the file was last modified
				String lastModDate = f.format(resource.lastModified());
				//Get If-Modified-Since date
				String ifMod = modifyDate.substring(modifyDate.indexOf(':')+2);
				System.out.println("\n\nModified Since: " + ifMod + "\n\n");

				//Convert Strings to Dates for Comparison
				try{
					Date date1 = f.parse(lastModDate);
					Date date2 = f.parse(ifMod);
					//If the last modified date is before the necessary "If-Modified-Since" date, do not modify.
					if (date2.after(date1)) {
						Calendar time = Calendar.getInstance();
						time.add(Calendar.HOUR, 24);
						return "HTTP/1.0 304 Not Modified" + '\r' + '\n' + "Expires: " + f.format(time.getTime()) + '\r' + '\n' ;
					}
				}
				catch(ParseException e){
					System.out.println("Invalid modified date");
				}
			}

			try{
				payload = readFile(resource);
			}
			catch (AccessDeniedException e) {
				return "HTTP/1.0 403 Forbidden";
			}
			catch (IOException io) {
				return "HTTP/1.0 404 Not Found";
			}

			String status = "HTTP/1.0 200 OK\r\n";
			String allow = "Allow: GET, POST, HEAD\r\n";
			head = status + allow + getHeader(resource, fileExt);

			return head;
		}

		// Perform POST command DO NOT ADD ALLOW
		if (method.equals("POST") && inDir) {
			System.out.println("YESSSSS");
			
			if (contentLength == "") {
				return "HTTP/1.0 411 Length Required";
			}
			if(contentType == "") {
				return "HTTP/1.0 500 Internal Server Error";
			}
			if(!(fileExt.equals("cgi"))){
				return "HTTP/1.0 405 Method Not Allowed";
			}
			try{
				int i = Integer.parseInt(contentLength);
				if(i==0) {
					return "HTTP/1.0 204 No Content";
				}
			}
			catch(NumberFormatException nfe){
				return "HTTP/1.0 411 Length Required";
			}

			 try{
			   byte[] temporary = readFile(resource);
			   }
			   catch (AccessDeniedException e) {
			   return "HTTP/1.0 403 Forbidden";
			   }
			   catch (IOException io) {
			   return "HTTP/1.0 405 Method Not Allowed";
			   }
			  
			   String status = "HTTP/1.0 200 OK\r\n";
			   String allow = "Allow: GET, POST, HEAD\r\n";
			   head = status + allow + getHeader(resource, fileExt);  
			   return head;

		}

		if(inDir == false) {
			return "HTTP/1.0 404 Not Found";
		}

		
		return "";
	}




	/*return content type for a file
	  @Param ext extension of file to fetch
	 **/
	private static String getcontentType(String ext){
		String contentType = "";
		switch(ext){
			case "html":
			case "txt":
			case "cgi":
				contentType = "text/html";
				break;
			case "gif":
				contentType = "image/gif";
				break;
			case "jpeg":
				contentType = "image/jpeg";
				break;
			case "png":
				contentType = "image/png";
				break;
			case "pdf":
				contentType = "application/pdf";
				break;
			case "gzip":
				contentType = "application/x-gzip";
				break;
			case "zip":
				contentType = "application/zip";
				break;
			default:
				contentType = "application/octet-stream";
		}
		return contentType;
	}

	/*Method to create the Header
	  @param File to extract information from
	  @return The header information
	 **/
	private static String getHeader(File file, String ext){

		String header = "";
		String contentType = "Content-Type: " + getcontentType(ext) + "\r" + "\n";
		String contentLength = "Content-Length: " + file.length() + "\r" + "\n";
		String contentEncoding = "Content-Encoding: identity\r" + "\n";

		String date = "Sat, 21 Jul 2021 10:00 GMT";
		Date expDate = new Date(date);
		SimpleDateFormat s = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
		s.setTimeZone(TimeZone.getTimeZone("GMT"));

		String lastModified = "Last-Modified: " + s.format(file.lastModified()) + "\r" + "\n";

		date = s.format(expDate);
		String expires = "Expires: " + date + "\r" + "\n";
		header =  contentEncoding + contentLength + contentType + expires + lastModified;
		return header;

	}

	/*Method to read the file into a byte array
	  @param File to extract information from
	  @return Byte array containing file
	 **/
	private byte[] readFile(File file) throws IOException{

		int fileLength = 0;
		if(file != null){
			fileLength = (int) file.length();
		}
		byte[] fileToReturn = new byte[fileLength];
		FileInputStream fis = null;

		try{
			fis = new FileInputStream(file);
			fis.read(fileToReturn);
		}
		finally{
			if(fis != null){
				fis.close();
			}
		}
		return fileToReturn;

	}

	private String cgiExecute(String request, String param){
		try{
			String request1 = param.substring(0, param.indexOf("&"));
			String request2 = param.substring(param.indexOf("&")+1);
			String name1 = request1.substring(0, request1.indexOf("="));
			String value1 = = request1.substring(request1.indexOf("=")+1);
			String name2 = request2.substring(0, request2.indexOf("="));
			String value2 = = request2.substring(request2.indexOf("=")+1);

		//check if the values are any reserved symbol under HTTP
			if(value1.contains("!") || value1.contains("*") || value1.contains("'") || value1.contains("(") || value1.contains(")") || value1.contains(";") || 	value1.contains(":") || value1.contains("@") || value1.contains("$") || value1.contains("+") || value1.contains(",") || value1.contains("/") || value1.contains("?") || value1.contains("#") || value1.contains("[") || value1.contains("]") || value1.contains(" ")){
				//encode the character with an ! before that character

			}
		}
		catch(Exception ex){
			return "HTTP/1.0 500 Internal Server Error";
		}
		return "";
	}


	public void run() {
		try {
			BufferedReader inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			DataOutputStream outHeader = new DataOutputStream(clientSocket.getOutputStream());


			try {
				clientSocket.setSoTimeout(3000);
				String request = "";
				String modifyDate = "";
				String from = "";
				String userAgent = "";
				String contentType = "";
				String contentLength = "";
				String param = "";
				String temp = "";
				File resource = null;
				int i = 0;

				try{
					while((temp = inStream.readLine()) != null && temp.length()!=0){
						if(request == ""){
							request = temp;
							continue;
						}

						if(request.contains("GET")){
							modifyDate = temp;
						}

						if(request.contains("POST")){
							if(temp.contains("From")){
								from = temp.substring(temp.indexOf(":")+2);
							}
							else if(temp.contains("User-Agent")){
								userAgent = temp.substring(temp.indexOf(":")+2);
							}
							else if(temp.contains("Content-Type")){
								contentType = temp.substring(temp.indexOf(":")+2);
							}
							else if(temp.contains("Content-Length")){
								contentLength = temp.substring(temp.indexOf(":")+2);
							}
							else if(temp.contains("&")){
								param = temp;
							}
						}

					}

					if(param == ""){
						param = inStream.readLine();
					}

				}
				catch(IOException eih){
					System.out.println("bad");
				}

				System.out.println("REQUEST: " + request);


				//String postResp = postCheck(request, from, userAgent, contentType, contentLength, param);
				/*
				   if(request.contains("POST")){
				   System.out.println("REQUEST: " + request);
				   System.out.println("MODIFY DATE: " + modifyDate);
				   System.out.println("FROM: " + from);
				   System.out.println("USER AGENT: " + userAgent);
				   System.out.println("CONTENT TYPE: " + contentType);
				   System.out.println("CONTENT LENGTH: " + contentLength);
				   System.out.println("PARAM: " + param);
				   byte[] poststuff = postResp.getBytes();
				   outHeader.write(poststuff, 0, poststuff.length);
				   outHeader.flush();
				   }
				 */

				//Parse input from client
				String resp = response(request, modifyDate, from, userAgent, contentType, contentLength, param);
				System.out.println("RESPONSE: " + resp);
				byte[] byteResponse = resp.getBytes();
				outHeader.write(byteResponse, 0, byteResponse.length);
				System.out.println(byteResponse.length);
				outHeader.flush();
				//if valid response send payload

				if (resp.contains("200 OK") && (request.contains("POST")) ) {
					String cgi = cgiExecute(request, param);
					//outHeader.write(payload, 0, payload.length);
				}
				if (resp.contains("200 OK") && (request.contains("GET")) ) {
					//System.out.println("payload length is " + payload.length);
					outHeader.write(payload, 0, payload.length);
				}

				/*
				//if valid response send payload
				if (resp.contains("200 OK") && !(request.contains("HEAD")) ) {
				//System.out.println("payload length is " + payload.length);
				outHeader.write(payload, 0, payload.length);
				}
				 */
			}
			catch (SocketTimeoutException e){
				byte[] byteResponse = "HTTP/1.0 408 Request Timeout".getBytes();
				outHeader.write(byteResponse, 0, byteResponse.length);
			}
			inStream.close();
			outHeader.close();
			clientSocket.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}

	}
}



