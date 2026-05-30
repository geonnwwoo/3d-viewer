package lhs.finalproject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.InetSocketAddress;

//recieves zipped scans from an iPhone shortcut --> saves --> extracts images
public class BatchImageServer {

    //port number for server
    private static final int PORT = 5001;
    //host address
    private static final String HOST = "0.0.0.0";

    //starts server (needs to run to recieve)
    public static void main(String[] args) {
        try {
            startServer();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    //creates & starts the server
    public static void startServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(HOST, PORT);
        HttpServer server = HttpServer.create(address, 0);
        server.createContext("/upload_batch", new BatchHandler());
        server.setExecutor(null);
        printStartupMessage();
        server.start();
    }

    //prints when starting up
    public static void printStartupMessage() {
        System.out.println("server started");
        System.out.println("Waiting for batch upload...");
    }

    //gets zip file --> saves them --> extracts images
    public static class BatchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            
            String requestMethod = exchange.getRequestMethod();
            //accepts POST requests only
            if (requestMethod.equals("POST")) {
                InputStream inputStream = exchange.getRequestBody();
                String folderName = createFolderName();
                File directory = createDirectory(folderName);
                File zipFile = new File(directory, "upload.zip");
                saveZipFile(inputStream, zipFile);
                System.out.println("Success! Saved in: " + folderName);
                sendResponse(exchange,200,"batch recieved! :)");
            }
            else {
                sendResponse(exchange,405,"Only POST requests are allowed :(");
            }
        }

        //creates unique folder name w/ time stamp in milliseconds
        public String createFolderName() {
            long time = System.currentTimeMillis();
            return "scan_" + time;
        }

        //creates a directory to save the images directly in the "Downloads" folder in MacOS
        public File createDirectory(String folderName) {
            String downloads = System.getProperty("user.home") + "/Downloads/" + folderName;
            File directory = new File(downloads);
            directory.mkdir();
            return directory;
        }

        //saves and extracts the zip file
        public void saveZipFile(InputStream inputStream, File zipFile) throws IOException {
            //zip file saved from iPhone
            try (FileOutputStream outputStream = new FileOutputStream(zipFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            //unzips
            Unzipper.unzip(zipFile, zipFile.getParentFile());

            System.out.println("finsihed extracting!");
        }

        //sends a reply to iPhone indicating that the upload is finished
        public void sendResponse(HttpExchange exchange,int statusCode,String response) throws IOException {
            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(statusCode,responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.getResponseBody().close();
        }
    }
}

