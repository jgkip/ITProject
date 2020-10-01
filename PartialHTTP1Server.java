import java.net.*; 
import java.io.*;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

public class PartialHTTP1Server implements Runnable{

	private ArrayList<WorkerThread> threads = new ArrayList<>(50); //list of threads; once full 
	private static ExecutorService pool = Executors.newFixedThreadPool(5); //thread pool of size 5

	//worker thread class to handle requests
	private static class WorkerThread implements Runnable {
		
		private Socket clientSocket; 
		private DataInputStream inStream; 
		private PrintWriter outClient;
		private OutputStream outHeader;

		//worker thread to handle requests 
		public WorkerThread(Socket clientSocket) throws IOException {
			this.clientSocket = clientSocket; 
			inStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			outClient = new PrintWriter(clientSocket.getOutputStream(), true);
			outHeader = clientSocket.getOutputStream();  
		}

		@Override
		public void run() {
			try { 
				//2DO: implement GET, HEAD, POST here(?)
				outClient.println("Welcome to server");

				while (true) {
					String request = inStream.readUTF(); 
 
					StringTokenizer tokens = new StringTokenizer(request); //tokenize by SPACE

					//Parse Request into method, file, protocol
					String method = tokens.nextToken(); 
					String file = tokens.nextToken();  
					String protocol = tokens.nextToken();

					//Get file information
					StringTokenizer files = new StringTokenizer(file, "/"); 
					String fileName = files.nextToken(); 
					System.out.println(fileName);
					StringTokenizer fileParse = new StringTokenizer(fileName, ".");
					String fn = fileParse.nextToken();
					String fileExt = fileParse.nextToken(); 
					
			

					//400 Bad Request
					//if no version is specified...
					if (protocol.length() != 8) {
						//System.out.println("yes");
						outClient.println("400 Bad Request");
						continue;
					} 	
					StringTokenizer protocolData = new StringTokenizer(protocol, "/");
					String http = protocolData.nextToken();
					//System.out.println("version: " + protocolData.nextToken());
					String version = protocolData.nextToken();
					
					
					//check if parent directory of server matches folderName
					
					//NEED TO FIX LOGIC--thread needs to sleep?

					 //check if valid request but no implementation
                   			 if (!(method.equals("GET")) && !(method.equals("POST")) && !(method.equals("HEAD")) && (version.equals("1.0"))) {

                      				  if(method.equals("DELETE") || method.equals("PUT") || method.equals("LINK") || method.equals("UNLINK")){
                      					  System.out.println("command not implemented");
                      					  outClient.println("501 Not Implemented");
                      				  }
                       				 else{
                       					 outClient.println("400 Bad Request");
                       				 }        
                        			continue;
                 			   }

					//check if both version and request are invalid
					if (!(method.equals("GET")) && !(method.equals("POST")) && !(method.equals("HEAD")) && !(version.equals("1.0"))) {
						outClient.println("400 Bad Request");
						continue; 
					}

					//check if request is valid but version is invalid
					if (method.equals("GET") || method.equals("POST")|| method.equals("HEAD")) {
						//System.out.println("we are here");
						if (!version.equals("1.0")) {
							System.out.println("version is not supported");
							//System.out.println(protocol);
							outClient.println("505 HTTP Version Not Supported");
							continue; 
							//clientSocket.close(); 
						}
	
					}

					//get current directory 
					String crntDir = System.getProperty("user.dir");
					StringTokenizer dirTokens = new StringTokenizer(crntDir, "\\");

					//walk through folder and check for file 
					//boolean inDir = false; 
	
					//if file in folder inDir = true 
					//if GET command and file in folder...
					ArrayList<String> fileNames = getFileList(crntDir);
					ArrayList<File> actualFiles = getFiles(crntDir);
					boolean inDir = fileNames.contains(fileName);

					String separator = System.getProperty("line.separator");
					//DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					//System.out.println(inDir);
					//String status;
					//String allow = "GET, HEAD, POST";
					//String contentEncoding;
					//String contentLength;
					//String contentType;
					//String expires; 
					//String lastMod;
					String head;

				
				// Perform HEAD command
				if (method.equals("HEAD") && inDir) {
					File resource = fetchFile(fileName, actualFiles); //fetch the file
					head = getHeader(resource, fileExt) + "Allow: GET, HEAD, POST";
					//outClient.println(head);
					
					try{
						byte[] byteResponse = head.getBytes();
						outHeader.write(byteResponse);
						outHeader.flush();
					}
					
					catch(SocketTimeoutException e){
						byte[] byteResponse = "HTTP/1.0 408 Request Timeout".getBytes();
						outHeader.write(byteResponse);
						outHeader.flush();
					}
					inStream.close();
					outClient.close();
					outHeader.close();
					clientSocket.close();
				continue;

				}
				// Perform GET command
				if (method.equals("GET") && inDir) {
					File resource = fetchFile(fileName, actualFiles); //fetch the file
					head = getHeader(resource, fileExt) + "Allow: GET, HEAD, POST";
					//outClient.println(head);
					
					try{
						byte[] byteResponse = head.getBytes();
						outHeader.write(byteResponse);
						outHeader.flush();
					}
					
					catch(SocketTimeoutException e){
						byte[] byteResponse = "HTTP/1.0 408 Request Timeout".getBytes();
						outHeader.write(byteResponse);
						outHeader.flush();
					}
					inStream.close();
					outClient.close();
					outHeader.close();
					clientSocket.close();
				continue;
				}
			


				
					/**
					if (method.equals("GET") && inDir) {
						File resource = fetchFile(fileName, actualFiles); //fetch the file
						if (fileExt.equals("html") || fileExt.equals("txt")) {
							status = "HTTP/1.0 200 OK\r";
							contentType = "Content-Type: text/html\r";
							contentLength = "Content-Length: " + Long.toString(resource.length()) + "\r";
							//change format--currently in seconds 
							lastMod = "Last-Modified: " + Long.toString(resource.lastModified());
							String header = status + contentType + contentLength + lastMod; 
							String[] headers = {status, contentType};
							//outClient.println(header);
							outClient.println(header);
							//outClient.println(contentType);
							//outHeader.write(header.getBytes());
							//outHeader.write(status.getBytes());
							//outHeader.write(contentType.getBytes());
							//outHeader.flush();
							continue;
							
						}
						continue;
						

					}
				*/

					
					//test to see if threads work--delete later
					outClient.println("Valid request");
					
				}

			}
			catch (Exception e) {
				outClient.println("HTTP/1.0 500 Internal Server Error");
				//System.exit(0);
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
					contentType = "text/html";
					break;
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
					String status = "HTTP/1.0 200 OK\r";
					String contentType = "Content-Type: " + getcontentType(ext) + "\r" + "\n";
					String contentLength = "Content-Length: " + file.length() + "\r" + "\n";
					String contentEncoding = "identity\r" + "\n";
					
					String date = "Sat, 21 Jul 2021 10:00 GMT";
					Date expDate = new Date(date);
					SimpleDateFormat s = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
				

					String lastModified = "Last-Modified: " + s.format(file.lastModified()) + "\r" + "\n";

					date = s.format(expDate);
					String expires = "Expires: " + date + "\r" + "\n";
					header = status + contentType + contentLength + contentEncoding + lastModified + expires;
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

