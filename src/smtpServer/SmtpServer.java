package smtpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lauri Holopainen
 *
 */
public class SmtpServer {

    /**
     * @param args
     */
    public static void main(String[] args) {
        listenSmtp();
    }

    private static void listenSmtp() {
        // Recources are closed when exiting try block.
        int smtpPortNumber = 25000;
        try (var serverSocket = new ServerSocket(smtpPortNumber);
                var clientSocket = serverSocket.accept();
                var out = new PrintWriter(clientSocket.getOutputStream(), true);
                var in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
            String inputLine;
            String mailFrom;
            String data = "";
            List<String> rcpts = new ArrayList<>();

            out.println("220 TIES323 SMTP Server");
            String state = "220";

            while (!state.equals("221")) {
                inputLine = in.readLine();
                System.out.println(inputLine);
                String expectedCommand;
                switch (state) {
                case "220":
                    expectedCommand = "HELO";
                    if (inputLine.toUpperCase().startsWith(expectedCommand)) {
                        out.println("250 OK");
                        state = "250";
                    } else
                        out.println("550 Syntax error. Expected " + expectedCommand);
                    break;
                case "250":
                    expectedCommand = "MAIL FROM:";
                    if (inputLine.toUpperCase().startsWith(expectedCommand)) {
                        mailFrom = inputLine.substring(expectedCommand.length());
                        out.println("250 2.1.0 OK");
                        state = "2.1.0";
                    } else
                        out.println("550 Syntax error. Expected " + expectedCommand);
                    break;
                case "2.1.0":
                    expectedCommand = "RCPT TO:";
                    if (inputLine.toUpperCase().startsWith(expectedCommand)) {
                        rcpts.add(inputLine.substring(expectedCommand.length()));
                        out.println("250 2.1.5 OK");
                        state = "2.1.5";
                    } else
                        out.println("550 Syntax error. Expected " + expectedCommand);
                    break;
                case "2.1.5":
                    expectedCommand = "DATA";
                    if (inputLine.toUpperCase().startsWith(expectedCommand)) {
                        out.println("354 Start mail input; end with <CRLF>.<CRLF>");
                        state = "354";
                    } else
                        out.println("550 Syntax error. Expected " + expectedCommand);
                    break;
                case "354":
                    if (inputLine.equals(".")) {
                        out.println("250 2.0.0 OK");
                        state = "2.0.0";
                    } else
                        data += inputLine;
                    break;
                case "2.0.0":
                    expectedCommand = "QUIT";
                    if (inputLine.toUpperCase().startsWith(expectedCommand)) {
                        out.println("221 TIES323 SMTP Server closing transmission channel");
                        state = "221";
                    } else
                        out.println("550 Syntax error. Expected " + expectedCommand);
                    break;
                default:
                    out.println("502 Server error.");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
