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
        } while (!response.toUpperCase().startsWith(tag.toUpperCase()));
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
        if (selectFolder(in, out)) {
            String[] unseen = getUnseen(in, out);
            System.out.println(String.format("%d unseen mail(s).", unseen.length));
            if ((unseen.length > 0) && userConfirm("Look at subjects?")) {
                listSubjects(in, out, unseen);
            }
            commandLoop(in, out);
        }
    }

    private boolean selectFolder(BufferedReader in, PrintWriter out) throws IOException {
        var tag = getTag();
        out.println(String.format("%s LIST \"*\" \"*\"", tag));
        if (serverOk(in, tag)) {
            while (true) {
                System.out.println("Select a folder:");
                var folder = consoleInput.nextLine();
                tag = getTag();
                out.println(String.format("%s SELECT %s", tag, folder));
                if (serverOk(in, tag)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void commandLoop(BufferedReader in, PrintWriter out) throws IOException {
        System.out.println(
                "Commands:\n" + "l: List subject from unseen mails.\n" + "f msg: Fetch a mail.\n" + "q: End session.");
        var quit = false;
        while (!quit) {
            var command = consoleInput.nextLine().split(" ");
            if (command.length > 0) {
                switch (command[0]) {
                case "l":
                    listSubjects(in, out, getUnseen(in, out));
                    break;
                case "f":
                    if (command.length == 2) {
                        var tag = getTag();
                        out.println(String.format("%S FETCH %S (BODY[HEADER.FIELDS (SUBJECT)] BODY[TEXT])", tag,
                                command[1]));
                        serverOk(in, tag);
                    } else {
                        System.out.println("Sorry, did not understand.");
                    }
                    break;
                case "q":
                    var tag = getTag();
                    out.println(String.format("%s LOGOUT", tag));
                    serverOk(in, tag);
                    quit = true;
                    break;
                default:
                    System.out.println("Sorry, did not understand.");
                    break;
                }
            }
        }
    }

    private boolean userConfirm(String question) {
        while (true) {
            System.out.println(String.format("%s [y/n]:", question));
            var response = consoleInput.nextLine();
            if (response.equals("y")) {
                return true;
            }
            if (response.equals("n")) {
                return false;
            }
        }
    }

    /**
     * The SEARCH response occurs as a result of a SEARCH or UID SEARCH command. The
     * number(s) refer to those messages that match the search criteria. For SEARCH,
     * these are message sequence numbers; for UID SEARCH, these are unique
     * identifiers. Each number is delimited by a space. Example: S: * SEARCH 2 3 6
     */
    private String[] getUnseen(BufferedReader in, PrintWriter out) throws IOException {
        var tag = getTag();
        out.println(String.format("%s SEARCH UNSEEN", tag));
        var response = in.readLine();
        var unseen = new String[0];
        if (response != null && response.startsWith("* SEARCH")) {
            unseen = response.substring("* SEARCH".length()).trim().split(" ");
            serverOk(in, tag);
        }
        return unseen;
    }

    private void listSubjects(BufferedReader in, PrintWriter out, String[] unseen) throws IOException {
        for (var mailNumber : unseen) {
            var tag = getTag();
            out.println(String.format("%S FETCH %S BODY.PEEK[HEADER.FIELDS (SUBJECT FROM)]", tag, mailNumber));
            serverOk(in, tag);
        }
    }

}
