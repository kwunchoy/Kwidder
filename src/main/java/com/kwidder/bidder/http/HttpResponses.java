package com.kwidder.bidder.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpResponses {
  private HttpResponses() {
  }

  public static void writeJson(HttpExchange exchange, int status, Object body, ObjectMapper mapper)
      throws IOException {
    byte[] payload = mapper.writeValueAsBytes(body);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.getResponseHeaders().set("x-openrtb-version", "2.6");
    exchange.sendResponseHeaders(status, payload.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(payload);
    }
  }

  public static void writeHtml(HttpExchange exchange, int status, String body) throws IOException {
    byte[] payload = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
    exchange.getResponseHeaders().set("x-openrtb-version", "2.6");
    exchange.sendResponseHeaders(status, payload.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(payload);
    }
  }

  public static void writeStatus(HttpExchange exchange, int status) throws IOException {
    exchange.getResponseHeaders().set("x-openrtb-version", "2.6");
    exchange.sendResponseHeaders(status, -1);
    exchange.close();
  }

  public static void writeHealth(HttpExchange exchange, String seat, ObjectMapper mapper) throws IOException {
    writeJson(exchange, 200, Map.of("status", "ok", "seat", seat), mapper);
  }

  public static String readBody(HttpExchange exchange) throws IOException {
    return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }
}
