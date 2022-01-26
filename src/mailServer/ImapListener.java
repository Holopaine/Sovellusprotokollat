package mailServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

public class ImapListener {
    public MailServer mailServer;
    private ImapSessionState sessionState = ImapSessionState.NOT_AUTHENTICATED;
    MailBox selectedBox = null;

    public ImapListener(MailServer server) {
        mailServer = server;
    }

    public void listenImap() throws IOException {
        int portNumber = 14300;
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(portNumber);
            serverSocket.setSoTimeout(1000);
            clientSocket = serverSocket.accept();
            var out = new PrintWriter(clientSocket.getOutputStream(), true);
            var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Greetings to client.
            out.println(String.format("* OK TIES323 Imap Server ready for requests from %s",
                    clientSocket.getInetAddress().toString()));

            // Set up session
            sessionState = ImapSessionState.NOT_AUTHENTICATED;
            selectedBox = null;

            while (sessionState != ImapSessionState.LOGOUT) {
                // Wait for client command
                var message = in.readLine();
                var input = message.split(" ");

                // Ensure that tag and command exist.
                if (input.length < 2) {
                    out.println("* BAD Invalid tag or command");
                } else {
                    var tag = input[0];
                    var command = input[1];
                    switch (command.toUpperCase()) {
                    case "LOGIN":
                        loginRoutine(in, out, input, tag);
                        break;
                    case "LIST":
                        listRoutine(in, out, input, tag);
                        break;
                    case "SELECT":
                        selectRoutine(in, out, input, tag);
                        break;
                    case "FETCH":
                        fetchRoutine(in, out, input, tag);
                        break;
                    case "SEARCH":
                        searchRoutine(in, out, input, tag);
                        break;
                    case "LOGOUT":
                        logoutRoutine(in, out, input, tag);
                        break;
                    default:
                        out.println(String.format("%s  BAD Unknown command", tag));
                        break;
                    }
                }
            }
            serverSocket.close();
            clientSocket.close();
        } catch (SocketTimeoutException e) {
            // Expected, no need to print
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
    }

    private void loginRoutine(BufferedReader in, PrintWriter out, String[] input, String tag) {
        if (sessionState != ImapSessionState.NOT_AUTHENTICATED) {
            out.println(String.format("%s BAD LOGIN not allowed now.", tag));
        } else {
            if ((input.length != 4)) {
                out.println(String.format("%s BAD Provide exactly 2 arguments.", tag));
            } else {
                // var user = input[2];
                // pass = input[3];
                out.println(String.format("%s OK Success", tag));
                sessionState = ImapSessionState.AUTHENTICATED;
            }
        }
    }

    private void listRoutine(BufferedReader in, PrintWriter out, String[] input, String tag) {
        if (sessionState == ImapSessionState.NOT_AUTHENTICATED) {
            out.println(String.format("%s BAD LIST not allowed now.", tag));
        } else {
            if ((input.length != 4)) {
                out.println(String.format("%s BAD Provide exactly 2 arguments.", tag));
            } else {
                List<MailBox> mailboxes = mailServer.getMailBoxes();
                for (MailBox mailBox : mailboxes) {
                    out.println(String.format("* (%s) NIL %s", mailBox.getAttributesString(), mailBox.getName()));
                }
                out.println(String.format("%s OK Success", tag));
            }
        }
    }

    private void selectRoutine(BufferedReader in, PrintWriter out, String[] input, String tag) {
        if (sessionState == ImapSessionState.NOT_AUTHENTICATED) {
            out.println(String.format("%s BAD SELECT not allowed now.", tag));
        } else {
            if ((input.length != 3)) {
                out.println(String.format("%s BAD Provide exactly 1 argument.", tag));
            } else {
                var toSelect = input[2];
                var maybeBox = mailServer.getMailBoxes().stream()
                        .filter(box -> box.getName().toUpperCase().equals(toSelect.toUpperCase())).findAny();
                if (maybeBox.isPresent()) {
                    selectedBox = maybeBox.get();
                    out.println(String.format("%s OK Success", tag));
                    sessionState = ImapSessionState.SELECTED;
                } else {
                    out.println(String.format("%s NO [NONEXISTENT] Unknown Mailbox: %s (Failure)", tag, toSelect));
                }
            }
        }
    }

    private void searchRoutine(BufferedReader in, PrintWriter out, String[] input, String tag) {
        if (sessionState != ImapSessionState.SELECTED) {
            out.println(String.format("%s BAD SEARCH not allowed now.", tag));
        } else {
            if ((input.length < 3)) {
                out.println(String.format("%s BAD Not enough arguments.", tag));
            } else {
//                var searchCriteria = message.trim()
//                        .substring(String.format("%s SEARCH ", tag).length());
                var result = "";
                var mails = selectedBox.getMails();
                for (int i = 0; i < mails.size(); i++) {
                    // Could apply search criteria here.
                    result += i + " ";
                }
                result = result.trim();
                out.println(String.format("* SEARCH %s", result));
                out.println(String.format("%s OK SEARCH completed", tag));
            }
        }

    }

    private void fetchRoutine(BufferedReader in, PrintWriter out, String[] input, String tag) {
        if (sessionState != ImapSessionState.SELECTED) {
            out.println(String.format("%s BAD FETCH not allowed now.", tag));
        } else {
            if ((input.length < 3)) {
                out.println(String.format("%s BAD Provide 1 argument.", tag));
            } else {
                try {
                    var mailNumber = Integer.parseInt(input[2]);
                    var dataItems = "";
                    for (int i = 2; i < input.length; i++) {
                        dataItems += String.format(" %s", input[i]);
                    }
                    if (mailNumber >= 0 && mailNumber < selectedBox.getMails().size()) {
                        var mail = selectedBox.getMails().get(mailNumber);
                        out.println(String.format("* %s FETCH %s", mailNumber, dataItems));
                        mail.data.lines().forEach(line -> out.println(line));
                        out.println(String.format("%s OK Success", tag));
                    } else {
                        out.println(String.format("%s BAD Could not parse command.", tag));
                    }
                } catch (NumberFormatException e) {
                    out.println(String.format("%s BAD Could not parse command.", tag));
                }
            }
        }
    }

    private void logoutRoutine(BufferedReader in, PrintWriter out, String[] input, String tag) {
        out.println("* BYE LOGOUT Requested");
        selectedBox = null;
        sessionState = ImapSessionState.LOGOUT;
        out.println(String.format("%s OK Good day!", tag));
    }
}
