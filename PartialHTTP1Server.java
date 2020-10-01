import java.net.*; 
import java.io.*;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

public class PartialHTTP1Server {

	private ArrayList<WorkerThread> threads = new ArrayList<>(50); //list of threads; once full 

	private static ExecutorService pool = Executors.newFixedThreadPool(5); //thread pool of size 5

	//worker thread to handle requests
	private static class WorkerThread implements Runnable {
		private Socket clientSocket; 

		private DataInputStream inStream; 
		private PrintWriter outClient;
		private DataOutputStream outHeader; 

		//thread class to handle requests 
		public WorkerThread(Socket clientSocket) throws IOException {
			this.clientSocket = clientSocket; 
			inStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			outClient = new PrintWriter(clientSocket.getOutputStream(), true);
			outHeader = new DataOutputStream(clientSocket.getOutputStream());  
		}
		public void run() {
			try { 
				outClient.println("Welcome to server");
				int lines = 0; 
				String request = "";
				File resource = null; 
				while (lines != 1) {
					request = inStream.readUTF(); 
					lines++;
				}
				String resp = response(request);
				//System.out.println(resp);
				outClient.println(resp);
				/*
				File res = fileToReturn(request);
				byte[] payLoad = Files.readAllBytes(res.toPath());
				outHeader.write(payLoad);
				*/
			}
			catch (Exception e) {
				System.exit(0);
			}
		}
	}

	public PartialHTTP1Server(int port) {
		try {

			//create server socket 
			ServerSocket serverSocket = new ServerSocket(port); 
			System.out.println("Server started");
			//main server loop 
			while(true) {
				//create threads here
				//do other fancy stuff 
				try {
					//create client socket 
					Socket client = serverSocket.accept(); 

					//PrintWriter outToClient = new PrintWriter(client.getOutputStream(), true);

					//for each client connection create thread and 
					//add to thread list 
					WorkerThread worker = new WorkerThread(client);
					threads.add(worker);
					//System.out.println("Thread size: " + threads.size());
					pool.execute(worker);
					//worker.run(); 
					System.out.println("Thread size: " + threads.size());
					//worker.run(); 
	
					//if there are no more threads 
					//respond with 503 
					/*
					if (threads.size() == 50) {
						outToClient.println("503 Service Unavailable");
						client.close()
						
					}
					*/
				}
				catch(IOException i) { 
					System.out.println(i); 
				}  
			} 
			//dos.println("Server closing...");
			//System.out.println("Server closing...");
			//client.close();
		}
		catch(IOException i) { 
					System.out.println(i); 
		}  
	}

	static File fileToReturn (String request) {
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

	static String response(String request) {
		//System.out.println(request);
		//parse request 
		//String request = inStream.readUTF(); 
		//tokenize request into 
		//method, file, protocol 
		StringTokenizer tokens = new StringTokenizer(request); //tokenize by SPACE
						
		String method = tokens.nextToken(); 

		//parse file part of request e.g. /index.html
		String file = tokens.nextToken();  
		StringTokenizer files = new StringTokenizer(file, "/"); //tokenize by /
		String fileName = files.nextToken(); //get file token
		StringTokenizer fileParse = new StringTokenizer(fileName, ".");
		String fn = fileParse.nextToken();
		String fileExt = fileParse.nextToken(); 
					
		//parse protocol to get protocol name and version
		String protocol = tokens.nextToken();

		//400 Bad Request
		//if no version is specified...
		if (protocol.length() != 8) {
			//System.out.println("yes");
			return "HTTP/1.0 400 Bad Request";
						
		} 	
		StringTokenizer protocolData = new StringTokenizer(protocol, "/");
		String http = protocolData.nextToken();

		//System.out.println("version: " + protocolData.nextToken());
					
		String version = protocolData.nextToken();
					
					
		//check if valid request 
		//check if parent directory of server matches folderName
					
		//NEED TO FIX LOGIC--thread needs to sleep?

		//check if valid request but no implementation
                 if (!(method.equals("GET")) && !(method.equals("POST")) && !(method.equals("HEAD")) && (version.equals("1.0"))) {
				if(method.equals("DELETE") || method.equals("PUT") || method.equals("LINK") || method.equals("UNLINK")){
                      			return "501 Not Implemented";
                      		}
                       		 else{
                       			return "400 Bad Request";
                       		 }       
                  }

		//general bad requests 
		if (((!method.equals("GET") && (!method.equals("POST") && (!method.equals("HEAD")))) 
			&& (!version.equals("1.0")))) {
			return "HTTP/1.0 400 Bad Request";
						
		}

		//check if version # is 1.0--if greater than 1.0, 
		//respond with "505 HTTP Version Not Supported"
		if ((method.equals("GET")) || (method.equals("POST")) || (method.equals("HEAD"))) {
			//System.out.println("we are here");
			if (!version.equals("1.0")) {
				System.out.println("version is not supported");
				//System.out.println(protocol);
				return "HTTP/1.0 505 HTTP Version Not Supported";	
			}
						
		}

		//get current directory 
		String crntDir = System.getProperty("user.dir");
		StringTokenizer dirTokens = new StringTokenizer(crntDir, "\\");

		//walk through folder and check for file 
		//boolean inDir = false; 
	
		//if file in folder inDir = true 
		//if GET command and file in folder...
		ArrayList<String> fileNames = getFileList(crntDir); //list of file names
		ArrayList<File> actualFiles = getFiles(crntDir); //list of files 
		boolean inDir = fileNames.contains(fileName); //check if requested file is in directory

		String separator = System.getProperty("line.separator");
		String head;

				
				// Perform HEAD command
				if (method.equals("HEAD") && inDir) {
					File resource = fetchFile(fileName, actualFiles); //fetch the file
					String status = "HTTP/1.0 200 OK\r";
					String allow = "Allow: GET, HEAD\r";
					head = status + allow + getHeader(resource, fileExt);
					return head;
				}
				// Perform GET command
				if (method.equals("GET") && inDir) {
					File resource = fetchFile(fileName, actualFiles); //fetch the file
					String status = "HTTP/1.0 200 OK\r";
					String allow = "Allow: GET, HEAD\r";
					head = status + allow + getHeader(resource, fileExt);
					System.out.println(resource.getPath().toString()); 
					return head;
				}
				// Perform POST command DO NOT ADD ALLOW
				if (method.equals("POST") && inDir) {
					File resource = fetchFile(fileName, actualFiles); //fetch the file
					String status = "HTTP/1.0 200 OK\r";
					head = status + getHeader(resource, fileExt);
					return head;
				}

		if(inDir == false) {
			return "HTTP/1.0 404 Not Found";
		}
		return "";
	}

	static File fetchFile(String name, ArrayList<File> files) {
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i).getName().equals(name)) {
				return files.get(i);
			}
		}
		return null;
	}

	//method to walk directory 
	static void walkDir(File[] arr,int index,int level, ArrayList<String> f) { 
        // terminate condition 
        if(index == arr.length) 
            return; 
           
        // for files 
        if(arr[index].isFile()) {
            //System.out.println(arr[index].getName()); 
   	        f.add(arr[index].getName());
        }
        // for sub-directories 
        else if(arr[index].isDirectory()) { 
            //System.out.println("[" + arr[index].getName() + "]"); 
               
            // recursion for sub-directories 
            walkDir(arr[index].listFiles(), 0, level + 1, f); 
        } 
            
        // recursion for main directory 
        walkDir(arr,++index, level, f); 
    } 

    static void findFiles(File[] arr,int index,int level, ArrayList<File> f) { 
        // terminate condition 
        if(index == arr.length) 
            return; 
           
        // for files 
        if(arr[index].isFile()) {
            //System.out.println(arr[index].getName()); 
   	        f.add(arr[index]);
        }
        // for sub-directories 
        else if(arr[index].isDirectory()) { 
            //System.out.println("[" + arr[index].getName() + "]"); 
               
            // recursion for sub-directories 
            findFiles(arr[index].listFiles(), 0, level + 1, f); 
        } 
            
        // recursion for main directory 
        findFiles(arr,++index, level, f); 
    } 

    static ArrayList<File> getFiles(String dirName) {
    	File maindir = new File(dirName);
		ArrayList<File> filez = new ArrayList<File>(); 

		if (maindir.exists() && maindir.isDirectory()) { 
            
            File arr[] = maindir.listFiles(); 
                       
            findFiles(arr,0,0, filez); 
          
        }

        return filez; 
    }

    //method that gets list of files in directory
    static ArrayList<String> getFileList(String dirName) {
    	File maindir = new File(dirName);
		ArrayList<String> filez = new ArrayList<String>(); 

		if (maindir.exists() && maindir.isDirectory()) { 
            
            File arr[] = maindir.listFiles(); 
                       
            walkDir(arr,0,0, filez); 
          
        }

        return filez; 
    }

				/*Method to return the content type of the file
				@param Extension on the file 
				@return Content Type
				**/
				private static String getcontentType(String ext){
					String contentType = "";
					switch(ext){
					case "html":
					case "txt":
					case "text":
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
				

					String lastModified = "Last-Modified: " + s.format(file.lastModified()) + "\r" + "\n";

					date = s.format(expDate);
					String expires = "Expires: " + date + "\r" + "\n";
					header = contentType + contentLength + contentEncoding + lastModified + expires;
					return header;

				}


	//modify to take command-line input
	public static void main(String[] args) {
		//try{
		//int port = Integer.parseInt(args[0]);
		//}
		//catch (NumberFormatException e){
		//System.out.println(e + "Please enter a valid port number");
		//}
		//PartialHTTP1Server server = new PartialHTTP1Server(port);
		PartialHTTP1Server server = new PartialHTTP1Server(5000);		
	}
}
