import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.ParseException;

public class PartialHTTP1Server implements Runnable {

	private Socket clientSocket;
	private byte[] payload;
	private String command;
	private String modified;
	private static int port;

	PartialHTTP1Server (Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("Server needs port number");
			return;
		}
		port = 0;
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


	/*return content type for a file
	  @Param ext extension of file to fetch
	 **/
	private static String getContentType(String ext){
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

	//Generate header based on filename and file
	private String buildHeader (File file, String fileName) {
		String header = "";

		String extension = "";

		if (fileName.indexOf('.') != -1)
			extension = fileName.substring(fileName.indexOf('.')+1);
		header += "Content-Type: " + getContentType(extension) + '\r' + '\n';
		header += "Content-Length: " + file.length() + '\r' + '\n';

		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		header += "Last-Modified: " + sdf.format(file.lastModified()) + '\r' + '\n';
		header += "Content-Encoding: identity" + '\r' + '\n';
		header += "Allow: GET, POST, HEAD" + '\r' + '\n';

		Calendar now = Calendar.getInstance();
		now.add(Calendar.HOUR, 24);
		header += "Expires: " + sdf.format(now.getTime()) + '\r' + '\n';



		return header;


	}

	/* method that parses request and returns the correct response
	   @Param request is the request from client
	   @Param modify is the modifyDate
	 **/
	private int checkCommand(String command) {
		if (command.equals("GET") || command.equals("POST") || command.equals("HEAD"))
			return 0;
		else if (command.equals("PUT") || command.equals("DELETE") || command.equals("LINK") || command.equals("UNLINK"))
			return 1;
		else
			return 2;
	}

	private String parseClientInput(String clientInput, String from, String userAgent, String contentType, String contentLength, String param) {
		if (clientInput == null)
			return "HTTP/1.0 400 Bad Request";

		String[] tokens = clientInput.split("\\s+");

		float versionNum;

		//1. Parse format. Only three tokens, and first token is capitalized? Does the second token begin with /? Does the third token begin with HTTP/?
		if(tokens.length != 3 || !tokens[0].toUpperCase().equals(tokens[0]) || tokens[1].charAt(0) != '/' || !tokens[2].substring(0,5).equals("HTTP/") || tokens[2].substring(5) == null)
			return "HTTP/1.0 400 Bad Request";

		//Try to grab version number and see if it's valid
		try {
			versionNum = Float.parseFloat(tokens[2].substring(5));
		} catch (NumberFormatException num) {
			return "HTTP/1.0 400 Bad Request";
		}

		//See if version number is supported
		if (versionNum > 1.0 || versionNum < 0.0)
			return "HTTP/1.0 505 HTTP Version Not Supported";

		//2. Parse command. Is the first token GET, POST or HEAD?
		if(checkCommand(tokens[0]) == 1)
			return "HTTP/1.0 501 Not Implemented";
		else if (checkCommand(tokens[0]) == 2)
			return "HTTP/1.0 400 Bad Request";


		StringTokenizer token = new StringTokenizer(command); //tokenize by SPACE
		String method = token.nextToken();

		//parse file part of request e.g. /index.html
		String file = token.nextToken();

		if (file.contains("secret") || file.contains("forbidden")) {
			return "HTTP/1.0 403 Forbidden\r\n";
		}


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

		//Does the file exist on the server?
		String filePath = "." + tokens[1];
		boolean inDir = false;
		File f = new File(filePath);
		if (f.isFile()){
			inDir = true;
		}
		

		//check if the file is in the directory as a cgi for POST commands
		if (tokens[0].equals("POST")){
			System.out.println("file value " + file);
			String tmp = file.substring(0,file.indexOf("."));
			String file2 = tmp + ".cgi";
			System.out.println("file2 value " + file2);
			File resource2 = new File("./" + file2);
			if(resource2.isFile()){
				inDir = true;
			}
		}

		//Perform HEAD Command
		if (tokens[0].equals("HEAD")){
			String response = "HTTP/1.0 200 OK" + '\r' + '\n';
			response += buildHeader(f, tokens[1]);
			response+="\r\n";
			return response;

		}
		
		//Perform GET Command
		if (tokens[0].equals("GET")){
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

			//If If-Modified-Since header exists on a GET request, see if file has been modified since the requested date
			if (modified.contains("If-Modified-Since") && tokens[0].equals("GET")) {
				String date = modified.substring(modified.indexOf(':') + 2);
				System.out.println("\n\nModified Since String: " + date + "\n\n");
				String fileDate = sdf.format(f.lastModified());
				try {
					Date fileModifiedDate = sdf.parse(fileDate);
					Date ifModifiedSinceDate = sdf.parse(date);
					if (ifModifiedSinceDate.after(fileModifiedDate)) {
						Calendar now = Calendar.getInstance();
						now.add(Calendar.HOUR, 24);
						return "HTTP/1.0 304 Not Modified" + '\r' + '\n' + "Expires: " + sdf.format(now.getTime()) + '\r' + '\n' ;
					}
				} 	
				catch (ParseException e) {
				System.out.println("Error parsing date");
				}

			}

			Path path = Paths.get(filePath);

			//4. Otherwise, try to open the file and send response.
			try {

				payload = Files.readAllBytes(path);
				String response = "HTTP/1.0 200 OK" + '\r' + '\n';
				response += buildHeader(f, tokens[1]);
				response+="\r\n";
				return response;

			} 
			catch (AccessDeniedException e) {
				return "HTTP/1.0 403 Forbidden";
			} 
			catch (IOException io) {
				return "HTTP/1.0 404 Not Found";
			}
		}
	
		// Perform POST command DO NOT ADD ALLOW
		if (tokens[0].equals("POST") && inDir) {
			System.out.println("YESSSSS");
			Path path = Paths.get(filePath);

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
				byte[] temporary = Files.readAllBytes(path);
			}
			catch (AccessDeniedException e) {
				return "HTTP/1.0 403 Forbidden";
			}
			catch (IOException io) {
				return "HTTP/1.0 405 Method Not Allowed";
			}

			String response = "HTTP/1.0 200 OK" + '\r' + '\n';

			//Attach header to the 200 OK Request
			response += buildHeader(f, tokens[1]);


			response+="\r\n";

			return response;  

		}
			if(inDir == false) {
			return "HTTP/1.0 404 Not Found";
			}


			return "";



	}

	//helper method to decode parameters 
	private String decode(String param) {
		if (param.contains("!!")) {
			param = param.replace("!!", "!");
		}
		else if (param.contains("!")) {
			param = param.replace("!", "");
		}


		return param;
	}


	//helper method that parses parameters 
	private String parseParams(String param) {
		ArrayList<String> p = new ArrayList<String>(); 
		StringTokenizer parameters = new StringTokenizer(param, "&");
		p.add(decode(parameters.nextToken()));
		while (parameters.hasMoreTokens()) {
			p.add(decode(parameters.nextToken()));
		}
		String parsed = "";
		for (int i = 0; i < p.size(); i++) {
			parsed += p.get(i) + " ";
		}
		return parsed;
	}

	private String[] envVars(String script, String param, String from, String userAgent) {
		String[] vars = new String[6];
		vars[0] = "CONTENT LENGTH=" + param.getBytes().length;
		vars[1] = "SCRIPT_NAME=" + script; 
		try { 
			vars[2] = "SERVER_NAME=" + InetAddress.getLocalHost();
		}
		catch(UnknownHostException e) {
			System.out.println(e);
		}
		vars[3] = "SERVER_PORT=" + port;
		vars[4] = "HTTP_FROM=" + from;
		vars[5] = "HTTP_USER_AGENT=" + userAgent; 
		return vars;  
	}

	private String cgiExecute(String param, String from, String userAgent){
		String request = command;
		try{

			StringTokenizer parse_request = new StringTokenizer(request);
			String ft = parse_request.nextToken(); 
			String script_name = parse_request.nextToken(); 
			String[] variables = envVars(script_name, param, from, userAgent);

			//create command to execute CGI script
			param = parseParams(param);
			String command = "." + script_name + " " + param; 

			//run command	
			Runtime run = Runtime.getRuntime(); 
			Process proc = run.exec(command, variables);

			if (!param.equals("")) {
				proc.getOutputStream().write(param.getBytes());
				proc.getOutputStream().close(); 
			}

			InputStream stdIN = proc.getInputStream(); 
			BufferedReader b = new BufferedReader(new InputStreamReader(stdIN));

			StringBuilder out = new StringBuilder();

			String line;
			
			while ((line = b.readLine()) != null) {
				out.append(line + "\n");
			}

			b.close();
			stdIN.close(); 

			String string_payload_cgi = out.toString(); 

			if (string_payload_cgi.endsWith("\n\n")) {
				return string_payload_cgi.substring(0, string_payload_cgi.length()-1);
			}

			return string_payload_cgi;
					
		}
		catch(IOException ex){
			System.out.println(ex);
			return "HTTP/1.0 500 Internal Server Error";
		}

	}



	public void run() {

		try {
			BufferedReader inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			DataOutputStream outHeader = new DataOutputStream(clientSocket.getOutputStream());


			try {
				clientSocket.setSoTimeout(3000);
		
				String from = "";
				String userAgent = "";
				String contentType = "";
				String contentLength = "";
				String param = "";
				String temp = "";
				File resource = null;
				int i = 0;
				command = "";
				modified = "";

				try{
					while((temp = inStream.readLine()) != null && temp.length()!=0){
						if(command == ""){
							command = temp;
							continue;
						}

						if(command.contains("GET")){
							modified = temp;
						}

						if(command.contains("POST")){
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

				System.out.println("REQUEST: " + command);

				//Parse input from client
				String resp = parseClientInput(command, from, userAgent, contentType, contentLength, param);
				System.out.println("RESPONSE: " + resp);
				byte[] byteResponse = resp.getBytes();
				outHeader.write(byteResponse, 0, byteResponse.length);
				System.out.println(byteResponse.length);
				outHeader.flush();

				//if valid response send payload
				if (resp.contains("200 OK") && (command.contains("POST")) ) {
					String cgi = cgiExecute(param, from, userAgent);
					System.out.println("PAYLOAD STRING REP: " + cgi);
					
					byte[] cgi_payload = cgi.getBytes();
					outHeader.write(cgi_payload, 0, cgi_payload.length);
					outHeader.flush();
					
				}
				if (resp.contains("200 OK") && (command.contains("GET")) ) {
					outHeader.write(payload, 0, payload.length);
					outHeader.flush(); 
				}

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
