package mailServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Pop3Listener {
    public MailServer mailServer;

    public Pop3Listener(MailServer server) {
        mailServer = server;
    }

    public void listenPop3() throws IOException {
        int smtpPortNumber = 11000;
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(smtpPortNumber);
            serverSocket.setSoTimeout(1000);
            clientSocket = serverSocket.accept();
            var out = new PrintWriter(clientSocket.getOutputStream(), true);
            var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String user = "";
            // String pass = "";
            var auth = false;
            var quit = false;

            out.println(String.format("+OK TIES323 Server ready for requests from, %s",
                    clientSocket.getInetAddress().toString()));
            while (!quit) {

                var command = in.readLine().split(" ");
                switch (command[0].toUpperCase()) {
                case "USER":
                    if ((command.length > 1) && (command[1].length() > 0)) {
                        out.println("+OK send PASS");
                        user = command[1];
                    } else {
                        out.println("-ERR USER _who_?");
                    }
                    break;
                case "PASS":
                    if ((command.length > 1) && (command[1].length() > 0) && (user.length() > 0)) {
                        out.println("+OK Welcome.");
                        // pass = command[1];
                        auth = true;
                    } else {
                        out.println(String.format("-ERR bad command"));
                    }
                    break;
                case "STAT":
                    if (auth) {
                        out.println(String.format("+OK %s %s", mailServer.getMailbox().size(), mailServer.countOctets()));
                    } else {
                        out.println(String.format("-ERR bad command"));
                    }
                    break;
                case "LIST":
                    if (auth) {
                        if (command.length == 1) {
                            out.println(String.format("+OK %s %s", mailServer.getMailbox().size(), mailServer.countOctets()));
                            for (int i = 0; i < mailServer.getMailbox().size(); i++) {
                                out.println(String.format("%s %s", i, mailServer.getMailbox().get(i).data.getBytes().length));
                            }
                            out.println(".");
                        } else if (command.length == 2) {
                            try {
                                var msg = Integer.parseInt(command[1]);
                                if (msg >= 0 && msg < mailServer.getMailbox().size()) {
                                    out.println(
                                            String.format("+OK %s %s", msg, mailServer.getMailbox().get(msg).data.getBytes().length));
                                } else {
                                    out.println(String.format("-ERR Message number out of range."));
                                }
                            } catch (NumberFormatException e) {
                                out.println(String.format("-ERR That is not a number."));
                            }
                        } else {
                            out.println(String.format("-ERR Exactly zero or one argument permitted"));
                        }
                    } else {
                        out.println(String.format("-ERR bad command"));
                    }
                    break;
                case "RETR":
                    if (auth) {
                        if (command.length == 2) {
                            try {
                                var msg = Integer.parseInt(command[1]);
                                if (msg >= 0 && msg < mailServer.getMailbox().size()) {
                                    out.println(
                                            String.format("+OK %s %s", msg, mailServer.getMailbox().get(msg).data.getBytes().length));
//                                    out.println(mailbox.get(msg).data+"\r\n.");
                                    mailServer.getMailbox().get(msg).data.lines().forEach(line -> out.println(line));
                                    out.println(".");
                                } else {
                                    out.println(String.format("-ERR Message number out of range."));
                                }
                            } catch (NumberFormatException e) {
                                out.println(String.format("-ERR That is not a number."));
                            }
                        } else {
                            out.println(String.format("-ERR Exactly one argument permitted"));
                        }
                    } else {
                        out.println(String.format("-ERR bad command"));
                    }
                    break;
                case "QUIT":
                    out.println("+OK Farewell.");
                    quit = true;
                    break;
                default:
                    out.println(String.format("-ERR bad command"));
                    break;
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

}
