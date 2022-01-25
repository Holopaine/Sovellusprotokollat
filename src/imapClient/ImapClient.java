package imapClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.UUID;

public class ImapClient {
    private final Scanner consoleInput = new Scanner(System.in);

    /**
     * Opens a connection to pop3 server and manages the session state from
     * authorization to transaction and update.
     */
    public static void main(String[] args) {
        var client = new ImapClient();
        try (var socket = new Socket("localhost", 143);
                var out = new PrintWriter(socket.getOutputStream(), true);
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
            client.authenticate(in, out);
            client.transact(in, out);
//            client.quit(in, out);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean serverOk(BufferedReader in, String tag) throws IOException {
        var response = "";
        do {
            response = in.readLine();
            if (response == null) {
                return false;
            }
            System.out.println(String.format("> %s", response));
        } while (!response.startsWith(tag));
        var splitted = response.split(" ");
        var status = "";
        if (splitted.length > 1) {
            status = splitted[1];
        }
        return status.equals("OK");

    }

    private String getTag() {
        return UUID.randomUUID().toString();
    }

    /**
     * Try to log in until success or unconnection from server.
     */
    private void authenticate(BufferedReader in, PrintWriter out) throws IOException {
        if (serverOk(in, "*")) {
            var logged = false;
            while (!logged) {
                String[] auth = AskCredentials();
                var username = auth[0];
                var pass = auth[1];
                var tag = getTag();
                out.println(String.format("%s LOGIN %s %s", tag, username, pass));
                if (serverOk(in, tag)) {
                    logged = true;
                }
            }
        }
    }

    private String[] AskCredentials() {
        System.out.print("Username:");
        var username = consoleInput.nextLine();
        System.out.print("Password:");
        var password = consoleInput.nextLine();
        return new String[] { username, password };
    }
    
    private void transact(BufferedReader in, PrintWriter out) throws IOException {
//        selectInbox();  
        var tag = getTag();
        out.println(String.format("%s LIST \"*\" \"*\"", tag));
        if (serverOk(in, tag)) {
            System.out.println("Select a folder.");
            var folder = consoleInput.nextLine();
            out.println(String.format("%s SELECT %s", tag, folder));
        }
        
    }

}
