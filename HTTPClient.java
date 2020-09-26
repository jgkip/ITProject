import java.net.*; 
import java.io.*;

public class HTTPClient {
	Socket socket = null; 
	DataInputStream input = null;
	DataOutputStream output = null;
	BufferedReader inFromServer = null; 
	public HTTPClient(String address, int port) {
		try {
			socket = new Socket(address, port);
			System.out.println("Connected");
			input = new DataInputStream(System.in);
			output = new DataOutputStream(socket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String response = inFromServer.readLine();
			System.out.println(response);
		}

		catch(IOException i) { 
			System.out.println(i); 
		} 

		String line = "";
		String ifs = "";

		while(!line.equals("Over")) {
			try {
				line = input.readLine();
				output.writeUTF(line);
				ifs = inFromServer.readLine(); 
				System.out.println(ifs);

			}
			catch(IOException i) { 
				System.out.println(i); 
			} 
		}
		try { 
			input.close(); 
			output.close(); 
			socket.close(); 
		} 
		catch(IOException i) { 
			System.out.println(i); 
		} 
	}

	public static void main(String args[]) { 
		HTTPClient client = new HTTPClient("127.0.0.1", 5000); 
	} 
}