package pop3client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * @author Lauri Holopainen
 */
public class Pop3Client {
    private final Scanner consoleInput = new Scanner(System.in);

    /**
     * Opens a connection to pop3 server and manages the session 
     * state from authorization to transaction and update.
     */
    public static void main(String[] args) {
        var client = new Pop3Client(); 
        try (var socket = new Socket("localhost", 110);
                var out = new PrintWriter(socket.getOutputStream(), true);
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
            client.Authorize(in, out);
            client.Transact(in, out);
            client.Quit(in, out);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean serverOk(BufferedReader in) throws IOException {
        var response = in.readLine();
        System.out.println(String.format("> %s", response));
        return response.startsWith("+OK");
    }

    /**
     * Try to log in until success or unconnection from server.
     */
    private void Authorize(BufferedReader in, PrintWriter out) throws IOException {
        if (serverOk(in)) {
            var logged = false;
            while (!logged) {
                String[] auth = AskCredentials();
                var username = auth[0];
                var pass = auth[1];
                out.println(String.format("USER %s", username));
                if (serverOk(in)) {
                    out.println(String.format("PASS %s", pass));
                    if (serverOk(in)) {
                        logged = true;
                    }
                }
            }
        }
    }

    private  String[] AskCredentials() {
        System.out.print("Username:");
        var username = consoleInput.nextLine();
        System.out.print("Password:");
        var password = consoleInput.nextLine();
        return new String[] { username, password };
    }

    /**
     * Send stat, list or retr commands until user wants to quit.
     */
    private void Transact(BufferedReader in, PrintWriter out) throws IOException {
        out.println("STAT");
        var response = in.readLine().split(" ");
        System.out.println(String.format("%s unread email(s).", response[1]));
        
        var quit = false;
        while (!quit) {
            System.out.print("Available actions:\n" + "s: Status\n" + "l [msg]: List\n" + "r msg: Retrieve a message.\n"
                    + "q: Quit\n");
            var action = consoleInput.nextLine().split(" ");
            var msg = (action.length > 1) ? action[1] : "";
            switch (action[0]) {
            case "s":
                out.println("STAT");
                serverOk(in);
                break;
            case "l":
                out.println(String.format("LIST %s", msg));
                if (action.length == 1) {
                    ReadMultiLine(in);
                } else {
                    serverOk(in);
                }
                break;
            case "r":
                out.println(String.format("RETR %s", msg));
                ReadMultiLine(in);
                break;
            case "q":
                quit = true;
                break;
            default:
                break;
            }
        }

    }

    /**
     * After successfull command, read response until "CRLF.CRLF".
     */
    private static void ReadMultiLine(BufferedReader in) throws IOException {
        var finished = false;
        var status = in.readLine();
        System.out.println(status);
        if (status.startsWith("+OK")) {
            while (!finished) {
                var line = in.readLine();
                if (line.equals(".")) {
                    finished = true;
                } else {
                    System.out.println(line);
                }
            }
        }
    }

    /**
     * End the session.
     */
    private void Quit(BufferedReader in, PrintWriter out) throws IOException {
        out.println("QUIT");
        System.out.println(in.readLine());
    }

}
