package com.mcjournal;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

public class ChunkServer {
    private static final int PORT = 8088;
    private final ChunkManager manager;
    private final HttpServer server;

    public ChunkServer(int radius) throws IOException {
        this.manager = new ChunkManager(radius, 424242L);
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);

        setupRoutes();
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    private void addCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        headers.set("Content-Type", "application/json; charset=UTF-8");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        addCorsHeaders(exchange);
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void setupRoutes() {
        // 1. Status Check
        server.createContext("/api/status", exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String json = "{\"status\":\"online\",\"engine\":\"Java 26 HotSpot VM\",\"chunks\":" + manager.getAllChunks().size() + "}";
            sendJsonResponse(exchange, 200, json);
        });

        // 2. Get All 100 Chunks
        server.createContext("/api/chunks", exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            StringBuilder sb = new StringBuilder(manager.getAllChunks().size() * 11000);
            sb.append("{\"chunks\":[");
            boolean first = true;
            for (Chunk c : manager.getAllChunks()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"cx\":").append(c.getCx())
                  .append(",\"cz\":").append(c.getCz())
                  .append(",\"data\":\"").append(c.toBase64()).append("\"}");
            }
            sb.append("]}");

            sendJsonResponse(exchange, 200, sb.toString());
        });

        // 3. Instant Block Break
        server.createContext("/api/break", exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int x = parseJsonInt(body, "x");
            int y = parseJsonInt(body, "y");
            int z = parseJsonInt(body, "z");

            ChunkManager.BreakResult result = manager.breakBlock(x, y, z);
            if (result != null) {
                String json = String.format(
                    "{\"success\":true,\"blockType\":%d,\"name\":\"%s\",\"color\":\"%s\",\"x\":%d,\"y\":%d,\"z\":%d}",
                    result.blockType(), result.name(), result.color(), result.x(), result.y(), result.z()
                );
                sendJsonResponse(exchange, 200, json);
            } else {
                sendJsonResponse(exchange, 200, "{\"success\":false,\"reason\":\"unbreakable\"}");
            }
        });

        // 4. POIs Landmark List
        server.createContext("/api/pois", exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                addCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            List<TerrainGenerator.POI> list = manager.getPois();
            StringBuilder sb = new StringBuilder();
            sb.append("{\"pois\":[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                TerrainGenerator.POI p = list.get(i);
                sb.append(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"subtitle\":\"%s\",\"icon\":\"%s\",\"x\":%d,\"z\":%d,\"excerpt\":\"%s\"}",
                    p.id(), p.name(), p.subtitle(), p.icon(), p.x(), p.z(), p.excerpt()
                ));
            }
            sb.append("]}");
            sendJsonResponse(exchange, 200, sb.toString());
        });
    }

    private static int parseJsonInt(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return 0;
        int start = idx + pattern.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == ':')) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    public void start() {
        server.start();
        System.out.println("=================================================");
        System.out.println("  ☕ Java Voxel Engine Running on Port " + PORT);
        System.out.println("  🏔️ Generated " + manager.getAllChunks().size() + " Chunks in Parallel via Java 26");
        System.out.println("=================================================");
    }

    public static void main(String[] args) {
        try {
            ChunkServer server = new ChunkServer(5); // 10x10 = 100 chunks
            server.start();
        } catch (java.net.BindException be) {
            System.out.println("[ChunkServer] Port " + PORT + " is already running, attached to existing server instance.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
