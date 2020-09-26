import java.net.*; 
import java.io.*;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;

public class PartialHTTP1Server {

	private ArrayList<WorkerThread> threads = new ArrayList<>(50); //list of threads; once full 

	private static ExecutorService pool = Executors.newFixedThreadPool(5); //thread pool of size 5

	//worker thread to handle requests
	private static class WorkerThread implements Runnable {
		private Socket clientSocket; 

		private DataInputStream inStream; 
		private PrintWriter outClient; 

		//thread class to handle requests 
		public WorkerThread(Socket clientSocket) throws IOException {
			this.clientSocket = clientSocket; 
			inStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			outClient = new PrintWriter(clientSocket.getOutputStream(), true);
		}
		public void run() {
			try { 
				//2DO: implement GET, HEAD, POST here(?)
				outClient.println("Welcome to server");
				while (true) {
					//parse request 
					String request = inStream.readUTF(); 
					//tokenize request into 
					//method, file, protocol 
					StringTokenizer tokens = new StringTokenizer(request); //tokenize by SPACE
					
					String method = tokens.nextToken(); 

					//parse file part of request 
					String file = tokens.nextToken();  //parse the folder/filename 
					StringTokenizer files = new StringTokenizer(file, "/"); //tokenize by /
					String folderName = files.nextToken(); //get folder token
					String fileName = files.nextToken(); //get file token
					
					//parse protocol to get protocol name and version
					String protocol = tokens.nextToken();

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
					
					
					//check if valid request 
					//check if parent directory of server matches folderName
					
					//NEED TO FIX LOGIC--thread needs to sleep?

					//check if valid request but no command support 
					//respond with "501 Not Implemented"
					if (((!method.equals("GET") && (!method.equals("POST") && (!method.equals("HEAD")))) 
						&& (version.equals("1.0")))) {
						System.out.println("command not implemented");
						outClient.println("501 Not Implemented");
						continue; 
					}

					//general bad requests 
					if (((!method.equals("GET") && (!method.equals("POST") && (!method.equals("HEAD")))) 
						&& (!version.equals("1.0")))) {
						outClient.println("400 Bad Request");
						continue; 
					}

					//check if version # is 1.0--if greater than 1.0, 
					//respond with "505 HTTP Version Not Supported"
					if ((method.equals("GET")) || (method.equals("POST")) || (method.equals("HEAD"))) {
						//System.out.println("we are here");
						if (!version.equals("1.0")) {
							System.out.println("version is not supported");
							//System.out.println(protocol);
							outClient.println("505 HTTP Version Not Supported");
							continue; 
							
							//clientSocket.close(); 
						}
						
					}
					
					//TO IMPLEMENT 
					//
					//200
					//
					//304
					//
					//403
					//
					//404
					//
					//408
					//
					//500 Internal Error
					//
					//
					//
					//


					//test to see if threads work--delete later
					outClient.println("Valid request");
					
				}

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

					PrintWriter outToClient = new PrintWriter(client.getOutputStream(), true);

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

	//modify to take command-line input
	public static void main(String[] args) {
		PartialHTTP1Server server = new PartialHTTP1Server(5000);
	}
}

