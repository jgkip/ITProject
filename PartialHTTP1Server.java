import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.ParseException;

public class PartialHTTP1Server {

  private ArrayList<WorkerThread> threads = new ArrayList<>(50); //list of threads; once full 
  private static ExecutorService pool = Executors.newFixedThreadPool(5); //thread pool of size 

  //worker thread to handle requests
  private static class WorkerThread implements Runnable {

    private Socket clientSocket; 
    private DataInputStream inStream; 
    private PrintWriter outClient;
    private DataOutputStream outHeader; 
    private PrintStream ps;

    //thread class to handle requests 
    public WorkerThread(Socket clientSocket) throws IOException {
      this.clientSocket = clientSocket; 
      inStream = new DataInputStream(clientSocket.getInputStream());
      outClient = new PrintWriter(clientSocket.getOutputStream(), true);
      outHeader = new DataOutputStream(clientSocket.getOutputStream());  
      //ps = new PrintStream(clientSocket.getOutputStream(), true);
    }
    public void run() {
      try { 
        //System.out.println("Processing client request...");
        //outClient.println("Welcome to server");
        int lines = 0; 
        //String request = inStream.readLine();
        String request = "";
        //System.out.println(request);
        File resource = null; 
        
        while (lines != 1) {
          request = inStream.readLine(); 
          lines++;
        }
        System.out.println(request);
        String resp = response(request);
        System.out.println(resp);
        outClient.println(resp);
        //ps.println(resp);
        //if valid response send payload 
        if (resp.contains("200 OK")) {
            File res = fileToReturn(request);
            byte[] payLoad = Files.readAllBytes(res.toPath());
            outHeader.write(payLoad);
            outHeader.flush(); 
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

  private static File fileToReturn (String request) {
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

  private static String response(String request) {
    
    //parse request 
    //String request = inStream.readUTF(); 
    //tokenize request into 
    //method, file, protocol 
    
    boolean ifModifiedSince = false;
    if(request.contains("If-Modified-Since")){
      ifModifiedSince = true;
    }
    StringTokenizer tokens = new StringTokenizer(request); //tokenize by SPACE        

    if (tokens.countTokens() != 3) {
      return "HTTP/1.0 400 Bad Request\r\n";
    }

    String method = tokens.nextToken(); 

    //parse file part of request e.g. /index.html
    String file = tokens.nextToken();  
    //System.out.println("file token: " + file);
    StringTokenizer files = new StringTokenizer(file, "/"); //tokenize by /
    String fileName = files.nextToken(); //get file token
    System.out.println("file name: " + fileName);
    StringTokenizer fileParse = new StringTokenizer(fileName, ".");
    String fn = fileParse.nextToken();
    String fileExt = fileParse.nextToken(); 
          
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

    //check if valid command but no implementation
    if ((!(method.equals("GET")) && !(method.equals("POST")) && !(method.equals("HEAD")) && (version.equals("1.0"))) || 
      (method.equals("DELETE") || method.equals("PUT") || method.equals("LINK") || method.equals("UNLINK"))) {
        return "HTTP/1.0 501 Not Implemented\r\n";      
    }

    //general bad requests 

    //check if version # is 1.0--if greater than 1.0, 
    //respond with "505 HTTP Version Not Supported"
    if (((method.equals("GET")) || (method.equals("POST")) || (method.equals("HEAD"))) && (versionInt > 1.0 || versionInt < 0.0))  {
      return "HTTP/1.0 505 HTTP Version Not Supported\r\n"; 
    }

    //get current directory 
    String crntDir = System.getProperty("user.dir");
    StringTokenizer dirTokens = new StringTokenizer(crntDir, "\\");

    //walk through folder and check for file 
    //boolean inDir = false; 
  
    //if file in folder inDir = true 
    //if GET command and file in folder...
    ArrayList<String> fileNames = getFileList(crntDir);
    ArrayList<String> direct = getDirList(crntDir); //list of file names
    System.out.println("file list: " + fileNames);
    System.out.println("dir list: " + direct);
    ArrayList<File> actualFiles = getFiles(crntDir); //list of files 
    boolean inDir = fileNames.contains(fileName); //check if requested file is in directory
    boolean findDir = direct.contains(fileName);

    String separator = System.getProperty("line.separator");
    String head;

    if (file.contains("secret")) {
      return "HTTP/1.0 403 Forbidden\r\n";
    }

    // Perform HEAD command
    if (method.equals("HEAD") && inDir) {
      File resource = fetchFile(fileName, actualFiles); //fetch the file
      String status = "HTTP/1.0 200 OK\r\n";
      String allow = "Allow: GET, HEAD\r\n";
      head = status + allow + getHeader(resource, fileExt);
      return head;
    }

    //Perform GET command
    //If the requst includes an "If-Modified-Since" field, Perform a Conditional GET.
    if ((method.equals("GET") && inDir)) {
      File resource = fetchFile(fileName, actualFiles); //fetch the file
      if(ifModifiedSince == true){
        //Check when the file was last modified
        SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss z");
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        String lastModDate = f.format(resource.lastModified());
        //Get If-Modified-Since date
        String temp = request.substring(request.indexOf(':')+2);
        String modDate = f.format(temp);

        //Convert Strings to Dates for Comparison
        try{
          Date date1 = f.parse(lastModDate);
          Date date2 = f.parse(modDate);
          //If the last modified date is before the necessary "If-Modified-Since" date, do not modify.
          if(date1.compareTo(date2) < 0 ){
            String date = "Sat, 21 Jul 2021 10:00 GMT";
            Date expDate = new Date(date);
            date = f.format(expDate);
            String expires = "Expires: " + date + "\r" + "\n";
            return "HTTP/1.0 304 Not Modified\r" + expires;
            }
        }
        catch(ParseException e){
          System.out.println("Error: Invalid Dates");
        }
      }
      String status = "HTTP/1.0 200 OK\r\n";
      String allow = "Allow: GET, HEAD\r\n";
      head = status + allow + getHeader(resource, fileExt);
      //System.out.println(resource.getPath().toString()); 
          
      return head;
    }
    // Perform POST command DO NOT ADD ALLOW
    if (method.equals("POST") && inDir) {
      File resource = fetchFile(fileName, actualFiles); //fetch the file
      String status = "HTTP/1.0 200 OK\r\n";
      head = status + getHeader(resource, fileExt);
      return head;
    }

    if(inDir == false) {
      return "HTTP/1.0 404 Not Found\r\n";
    }
    return "";
  }

  //helper method to fetch the file 
  private static File fetchFile(String name, ArrayList<File> files) {
    for (int i = 0; i < files.size(); i++) {
      if (files.get(i).getName().equals(name)) {
        return files.get(i);
      }
    }
    return null;
  }

  //method to walk directory 
  private static void walkDir(File[] arr,int index,int level, ArrayList<String> f) { 
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
            //f.add(arr[index].getName());
            //System.out.println("[" + arr[index].getName() + "]"); 
               
            // recursion for sub-directories 
            walkDir(arr[index].listFiles(), 0, level + 1, f); 
        } 
            
        // recursion for main directory 
        walkDir(arr,++index, level, f); 
  } 

  private static void dirN(File[] arr,int index,int level, ArrayList<String> d) { 
        // terminate condition 
        if(index == arr.length) 
            return; 
           
        // for files 
        if(arr[index].isDirectory()) { 
            d.add(arr[index].getName());
            //System.out.println("[" + arr[index].getName() + "]"); 
               
            // recursion for sub-directories 
            dirN(arr[index].listFiles(), 0, level + 1, d); 
        } 
            
        // recursion for main directory 
        dirN(arr,++index, level, d); 
  } 
  private static ArrayList<String> getDirList(String dirName) {
        File maindir = new File(dirName);
        ArrayList<String> dirs = new ArrayList<String>(); 

        if (maindir.exists() && maindir.isDirectory()) { 
              File arr[] = maindir.listFiles(); 
              dirN(arr,0,0, dirs); 
        }

        return dirs; 
  }

  private static void findFiles(File[] arr,int index,int level, ArrayList<File> f) { 
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

  private static ArrayList<File> getFiles(String dirName) {
        File maindir = new File(dirName);
        ArrayList<File> filez = new ArrayList<File>(); 

        if (maindir.exists() && maindir.isDirectory()) { 
              File arr[] = maindir.listFiles(); 
              findFiles(arr,0,0, filez); 
        }

        return filez; 
  }

  //method that gets list of files in directory
  private static ArrayList<String> getFileList(String dirName) {
        File maindir = new File(dirName);
        ArrayList<String> filez = new ArrayList<String>(); 

        if (maindir.exists() && maindir.isDirectory()) { 
              File arr[] = maindir.listFiles(); 
              walkDir(arr,0,0, filez); 
        }

        return filez; 
  }

  //return content type for a file
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
      if(port == -1){
        System.out.println("Server needs port number");
        return;
      }
    }
    catch (NumberFormatException e){
      System.out.println("Server needs port number");
      return;
    }
    PartialHTTP1Server server = new PartialHTTP1Server(port);   
  }
}


