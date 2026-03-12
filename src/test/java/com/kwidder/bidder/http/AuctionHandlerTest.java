package com.kwidder.bidder.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kwidder.bidder.config.AppConfig;
import com.kwidder.bidder.service.BidEngine;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuctionHandlerTest {
  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void returnsBadRequestForMalformedJson() throws Exception {
    startServer(new AppConfig(0, "kwidder", "USD", 1.25d, 4.50d, "ads.kwidder.dev", "creative-1", "campaign-1"));

    HttpResponse<String> response = post("{");

    assertEquals(400, response.statusCode());
    assertEquals("2.6", response.headers().firstValue("x-openrtb-version").orElseThrow());
  }

  @Test
  void returnsNoContentForNoBid() throws Exception {
    startServer(new AppConfig(0, "kwidder", "USD", 1.25d, 0.50d, "ads.kwidder.dev", "creative-1", "campaign-1"));

    HttpResponse<String> response = post("""
        {"id":"req-1","imp":[{"id":"imp-1","bidfloor":1.0,"banner":{"w":300,"h":250}}]}
        """);

    assertEquals(204, response.statusCode());
    assertEquals("2.6", response.headers().firstValue("x-openrtb-version").orElseThrow());
  }

  private void startServer(AppConfig config) throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/openrtb2/auction", new AuctionHandler(new BidEngine(config), JsonSupport.mapper()));
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.start();
  }

  private HttpResponse<String> post(String body) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + server.getAddress().getPort() + "/openrtb2/auction"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
