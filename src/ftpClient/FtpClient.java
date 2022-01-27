package ftpClient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.regex.Pattern;

public class FtpClient {
    private final Scanner consoleInput = new Scanner(System.in);
    private static String rootServer = "ftp.funet.fi";
    private String rootPath = "/pub/sci/geo/carto/vanhatkartat/maakirjakartat/";

    public static void main(String[] args) {
        var client = new FtpClient();
        try (var socket = new Socket(rootServer, 21);
                var out = new PrintWriter(socket.getOutputStream(), true);
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
            client.login(in, out);
            client.transactionLoop(in, out);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Log in as anonymous user.
     */
    private void login(BufferedReader in, PrintWriter out) throws IOException {
        readResponse(in);
        out.println("USER anonymous");
        readResponse(in);
        out.println("PASS anonymous");
        readResponse(in);
    }

    /**
     * List and retrieve files on user commands.
     */
    private void transactionLoop(BufferedReader in, PrintWriter out) throws IOException {
        var endSession = false;
        while (!endSession) {
            System.out.println("Available commands:\n" + "LIST [dir]: List directory content.\n"
                    + "RETR <filepath>: Download a file from the server to the working directory\n"
                    + "QUIT: End session.\n");
            var command = consoleInput.nextLine().split(" ");
            switch (command[0].toUpperCase()) {
            case "LIST":
                var dir = (command.length > 1) ? command[1] : "";
                listDir(in, out, dir);
                break;
            case "RETR":
                if (command.length > 1) {
                    retrieveFile(in, out, command[1]);
                } else {
                    System.out.println("Give a filepath");
                }
                break;
            case "QUIT":
                endSession = true;
                out.println("QUIT");
                readResponse(in);
                break;
            }
        }
    }

    /**
     * Send an EPSV command to the server, and if the responses with a success,
     * parse and return the data channel port number.
     */
    private int requestEpsv(BufferedReader in, PrintWriter out) throws IOException {
        out.println("EPSV");
        var response = in.readLine();
        System.out.println(response);
        var portFound = Pattern.matches(".*\\(\\|\\|\\|[0-9]*\\|\\)", response);
        if (response.startsWith("229") && portFound) {
            var start = response.indexOf("(|||") + 4;
            var end = response.indexOf("|)");
            try {
                return Integer.parseInt(response.substring(start, end));
            } catch (NumberFormatException e) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    /**
     * Open data channel and send a LIST request.
     */
    private void listDir(BufferedReader in, PrintWriter out, String dir) throws IOException {
        var dataPort = requestEpsv(in, out);
        if (dataPort < 1) {

            System.out.println("Couldn't open data channel.");
        } else {
            try (var dataSocket = new Socket("ftp.funet.fi", dataPort);
                    var dataIn = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));) {
                out.println(String.format("LIST %s%s", rootPath, dir));
                readResponse(in); // 150 Accepted data connection
                readResponse(in); // 226
                readDataResponse(dataIn);
            }
        }
    }

    /**
     * Open data channel and send a RETR request.
     */
    private void retrieveFile(BufferedReader in, PrintWriter out, String filepath) throws IOException {
        var dataPort = requestEpsv(in, out);
        if (dataPort < 1) {
            System.out.println("Couldn't open data channel.");
        } else {
            out.println("TYPE I"); // Expect binary data.
            if (readResponse(in).equals("200")) { // Was TYPE command accepted
                try (var dataSocket = new Socket(rootServer, dataPort);
                        var dataIn = new DataInputStream(dataSocket.getInputStream());) {
                    out.println(String.format("retr %s%s", rootPath, filepath));
                    readResponse(in); // 150 Accepted data connection
                    readAndSaveFile(dataIn, filepath);
                    readResponse(in); // 226-File successfully transferred
                }
            }
        }
    }

    /**
     * Read a file from byte stream and save it to given filepath.
     */
    private void readAndSaveFile(DataInputStream dataIn, String filepath) throws IOException {
        try {
            var data = dataIn.readAllBytes();
            Path path = FileSystems.getDefault().getPath("").toAbsolutePath().resolve(filepath);
            Files.createDirectories(path.getParent());
            Files.write(path, data, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException e) {
            System.out.println("File already exists. Won't overwrite.");
        }
    }

    /**
     * Read textual data and print it to console.
     */
    private void readDataResponse(BufferedReader in) throws IOException {
        in.lines().forEach(line -> System.out.println(line));
    }

    /**
     * Read the response until the final line.
     */
    private String readResponse(BufferedReader in) throws IOException {
        var firstLine = in.readLine();
        System.out.println(firstLine);
        var statusCode = firstLine.substring(0, 3);
        if (firstLine.length() > 3 && firstLine.charAt(3) == '-') {
            while (true) {
                var line = in.readLine();
                System.out.println(line);
                if (line.startsWith(String.format("%s ", statusCode))) {
                    break;
                }
            }
        }
        return statusCode;
    }

}
