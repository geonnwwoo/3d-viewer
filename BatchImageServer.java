package lhs.finalproject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


import java.net.InetSocketAddress;


public class BatchImageServer {


    private static final int PORT = 5001;
    private static final String HOST = "0.0.0.0";


    public static void main(String[] args) {
        try {
            startServer();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void startServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(HOST, PORT);
        HttpServer server = HttpServer.create(address, 0);
        server.createContext("/upload_batch", new BatchHandler());
        server.setExecutor(null);
        printStartupMessage();
        server.start();
    }


    public static void printStartupMessage() {
        System.out.println("--- Server Started ---");
        System.out.println("1. Find your IP using ipconfig.");
        System.out.println("2. Set iPhone Shortcut URL to http://YOUR_IP:5001/upload_batch");
        System.out.println("Waiting for batch upload...");
    }


    public static class BatchHandler implements HttpHandler {


        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equals("POST")) {
                InputStream inputStream = exchange.getRequestBody();
                String folderName = createFolderName();
                File directory = createDirectory(folderName);
                File zipFile = new File(directory, "upload.zip");
                saveZipFile(inputStream, zipFile);
                System.out.println("Success! Batch saved in: " + folderName);
                sendResponse(exchange,200,"Batch received by Java Server!");
            }
            else {
                sendResponse(exchange,405,"Only POST requests are allowed.");
            }
        }


        public String createFolderName() {
            long time = System.currentTimeMillis();
            return "scan_" + time;
        }


        public File createDirectory(String folderName) {
            String downloads = System.getProperty("user.home") + "/Downloads/" + folderName;
            File directory = new File(downloads);
            directory.mkdir();
            return directory;
        }


        public void saveZipFile(InputStream inputStream, File zipFile) throws IOException {
            // 1. Save the Zip file from the iPhone
            try (FileOutputStream outputStream = new FileOutputStream(zipFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // 2. Call your new separate class to unzip it!
            // We pass the zip file and the directory it is sitting in
            Unzipper.unzip(zipFile, zipFile.getParentFile());

            System.out.println("Unzipper class finished extracting files.");
        }


        public void sendResponse(HttpExchange exchange,int statusCode,String response) throws IOException {
            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(statusCode,responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.getResponseBody().close();
        }
    }
}

