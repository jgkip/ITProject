import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.ParseException;

public class PartialHTTP1Server implements Runnable {

	//private ArrayList<WorkerThread> threads = new ArrayList<>(50); //list of threads; once full
	//private static ExecutorService pool = Executors.newFixedThreadPool(5); //thread pool of size
	private byte[] payLoad;
	private Socket clientSocket;

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
				System.out.println("Listening on Port Number " + port);

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
	


	//from request get file to return
	//@Param request is the request from client
	private File fileToReturn (String request) {
		StringTokenizer tokens = new StringTokenizer(request); //tokenize by SPACE

		String method = tokens.nextToken();

		//parse file part of request e.g. /index.html
		String file = tokens.nextToken();
		StringTokenizer files = new StringTokenizer(file, "/"); //tokenize by /
		String fileName = files.nextToken(); //get file token
		//get current directory
		String crntDir = System.getProperty("user.dir");
		StringTokenizer dirTokens = new StringTokenizer(crntDir, "\\");
		ArrayList<String> fileNames = getFileList(crntDir); //list of file names
		ArrayList<File> actualFiles = getFiles(crntDir); //list of files
		File f = fetchFile(fileName, actualFiles);
		return f;
	}


	/* method that parses request and returns the correct response
	   @Param request is the request from client
	   @Param modify is the modifyDate
	 **/
	private String response(String request, String modifyDate) {
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
		Path path = Paths.get("." + file);
		System.out.println("file token: " + file);
		StringTokenizer files = new StringTokenizer(file, "/"); //tokenize by /
		int numTokens = files.countTokens();
		String fileName = "";
		String folder = "";
		String fn = "";
		String fileExt = "";

		//if requested resource is not in folder
		if (numTokens == 1) {
			fileName = files.nextToken(); //get file token
			System.out.println("file name: " + fileName);
	
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

		//get current directory
		String crntDir = System.getProperty("user.dir");
		StringTokenizer dirTokens = new StringTokenizer(crntDir, "\\");

		//walk through folder and check for file
		//if file in folder inDir = true
		//if GET command and file in folder...
		ArrayList<String> fileNames = getFileList(crntDir);

		ArrayList<File> actualFiles = getFiles(crntDir); //list of files
		boolean inDir = fileNames.contains(fileName); //check if requested file is in directory

		String separator = System.getProperty("line.separator");
		String head;

		if (file.contains("secret")) {
			return "HTTP/1.0 403 Forbidden\r\n";
		}

		// Perform HEAD command
		if (method.equals("HEAD") && inDir) {
			File resource = fetchFile(fileName, actualFiles); //fetch the file
			String status = "HTTP/1.0 200 OK\r\n";
			String allow = "Allow: GET, POST, HEAD\r\n";
			head = status + allow + getHeader(resource, fileExt);
			return head;
		}

		//Perform GET command
		//If the request includes an "If-Modified-Since" field, Perform a Conditional GET.
		if ((method.equals("GET") && inDir)) {
			File resource = fetchFile(fileName, actualFiles); //fetch the file
			//File resource = new File("." + file);
			//Path path = Paths.get(resource);
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
						payLoad = Files.readAllBytes(path);
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
			File resource = fetchFile(fileName, actualFiles); //fetch the file
			//File resource = new File("." + file);
			//Path path = Paths.get("." + file);
					
					try{
						payLoad = Files.readAllBytes(path);
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

		if(inDir == false) {
			return "HTTP/1.0 404 Not Found\r\n";
		}
		return "";
	}

	//helper method to fetch the file
	private File fetchFile(String name, ArrayList<File> files) {
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i).getName().equals(name)) {
				return files.get(i);
			}
		}
		return null;
	}

	//method to walk directory
	/*@Param arr is an array of files
	  @Param index is the index of current file
	  @Param level is the current level in directory
	  @Param ArrayList f collects file names
	 **/
	private void walkDir(File[] arr, int index, int level, ArrayList<String> f) {
		// terminate condition
		if(index == arr.length)
			return;

		// for files
		if(arr[index].isFile()) {
			f.add(arr[index].getName());
		}
		// for sub-directories
		else if(arr[index].isDirectory()) {
			// recursion for sub-directories
			walkDir(arr[index].listFiles(), 0, level + 1, f);
		}

		// recursion for main directory
		walkDir(arr,++index, level, f);
	}

	/*helper method used to find files
	  @Param arr array of files
	  @Param int index current index of File
	  @Param level is the level in the directory
	  @Param ArrayList f is used to collect the files
	 **/
	private void findFiles(File[] arr, int index, int level, ArrayList<File> f) {
		// terminate condition
		if(index == arr.length)
			return;

		// for files
		if(arr[index].isFile()) {
			f.add(arr[index]);
		}
		else if(arr[index].isDirectory()) {
			findFiles(arr[index].listFiles(), 0, level + 1, f);
		}

		findFiles(arr,++index, level, f);
	}

	/*method that gets actual files in directory dirName
	  @Param dirName is the name of the directory
	 **/
	private ArrayList<File> getFiles(String dirName) {
		File maindir = new File(dirName);
		ArrayList<File> filez = new ArrayList<File>();

		if (maindir.exists() && maindir.isDirectory()) {
			File arr[] = maindir.listFiles();
			findFiles(arr,0,0, filez);
		}

		return filez;
	}

	/*method that gets list of files in directory dirName
	  @Param dirName is the name of the directory
	 **/
	private ArrayList<String> getFileList(String dirName) {
		File maindir = new File(dirName);
		ArrayList<String> filez = new ArrayList<String>();

		if (maindir.exists() && maindir.isDirectory()) {
			File arr[] = maindir.listFiles();
			walkDir(arr,0,0, filez);
		}

		return filez;
	}

	/*return content type for a file
	  @Param ext extension of file to fetch
	 **/
	private static String getcontentType(String ext){
		String contentType = "";
		switch(ext){
			case "html":
			case "txt":
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


	public void run() {
			try {
				BufferedReader inStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				DataOutputStream outHeader = new DataOutputStream(clientSocket.getOutputStream());

				try {
					String request = "";
					String modifyDate = "";
					File resource = null;
					int lines = 0;
					while (lines != 2) {
						if(lines == 0){
							request = inStream.readLine();
						}
						else if (lines == 1) {
							modifyDate = inStream.readLine();
						}
						lines++;
					}
					//Parse input from client
					String resp = response(request, modifyDate);
					System.out.println(resp);
					byte[] byteResponse = resp.getBytes();
					outHeader.write(byteResponse);
					outHeader.flush();

					//if valid response send payload
					if (resp.contains("200 OK") && !(request.contains("HEAD"))) {
						//System.out.println(payLoad);
						outHeader.write(payLoad);
						//outHeader.flush();
										}
					}
				catch (Exception e) {
					System.out.println(e);
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
