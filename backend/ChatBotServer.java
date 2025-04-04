package backend;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

public class ChatBotServer {
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    public static void main(String[] args) throws IOException {
        String portStr = System.getenv("PORT");
        if (portStr == null || portStr.isEmpty()) {
            throw new IllegalStateException("PORT environment variable not set");
        }
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable not set");
        }

        int port = Integer.parseInt(portStr);
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // Removed static file serving (Handled via GitHub Pages)
        // server.createContext("/", new StaticFileHandler());

        // Chat API Endpoint
        server.createContext("/chat", new ChatHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Server started on http://localhost:" + port + "/");
    }

    static class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS Headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    StringBuilder requestBody = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        requestBody.append(line);
                    }

                    JSONObject requestJson = new JSONObject(requestBody.toString());
                    String userMessage = requestJson.getString("message");

                    String botResponse = getBotResponse(userMessage);

                    JSONObject responseJson = new JSONObject();
                    responseJson.put("response", botResponse);

                    byte[] responseBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private String getBotResponse(String message) throws IOException {
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return "Error: API key is missing!";
            }

            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject requestBody = new JSONObject()
                    .put("model", "gpt-3.5-turbo")
                    .put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", message)));

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                }

                if (conn.getResponseCode() != 200) {
                    return "Error: API returned code " + conn.getResponseCode();
                }

                String responseString = readStream(conn.getInputStream());
                JSONObject responseJson = new JSONObject(responseString);
                return responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: Failed to fetch response from API.";
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        private String readStream(InputStream stream) throws IOException {
            if (stream == null) return "No response from server.";
            BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }
}
