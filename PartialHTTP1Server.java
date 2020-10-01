import java.net.*; 
import java.io.*;
import java.lang.*;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.nio.file.Files;
import java.text.SimpleDateFormat;

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
				//2DO: implement GET, HEAD, POST here(?)
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

		//check if valid request but no command support 
		//respond with "501 Not Implemented"
		if (((!method.equals("GET") && (!method.equals("POST") && (!method.equals("HEAD")))) 
			&& (version.equals("1.0")))) {
			System.out.println("command not implemented");
			return "HTTP/1.0 501 Not Implemented";
						
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
		SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
		//date.setTimeZone(TimeZone.getTimeZone("GMT"));
		//System.out.println(inDir);
		String status;
		String allow = "Allow: GET, HEAD, POST\r\n";
		String contentEncoding = "Content-Encoding: identity"; //x-gzip, zip 
		String contentLength;
		String contentType;
		String expires; 
		String lastMod;
		if ((method.equals("GET") || method.equals("POST")) && inDir) {
			File resource = fetchFile(fileName, actualFiles); //fetch the file
			if (fileName.contains("html") || fileName.contains("txt")) {
				status = "HTTP/1.0 200 OK \r\n";
				contentType = "Content-Type: text/html \r\n";
				contentLength = "Content-Length: " + Long.toString(resource.length()) + " \r\n";
				//change format--currently in seconds  
				lastMod = "Last-Modified: " + date.format(resource.lastModified()) + " \r\n";
				String header = status + allow + contentLength + contentType + lastMod + contentEncoding; 
				System.out.println(resource.getPath().toString()); 
				return header;  	
			}
						
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

	//modify to take command-line input
	public static void main(String[] args) {
		PartialHTTP1Server server = new PartialHTTP1Server(5000);
	}
}

