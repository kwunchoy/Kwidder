package com.kwidder.bidder.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kwidder.bidder.config.AppConfig;
import com.kwidder.bidder.lineitem.LineItemStore;
import com.kwidder.bidder.lineitem.MediaType;
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
    startServer(config(4.50d, 30.00d), null);

    HttpResponse<String> response = post("{");

    assertEquals(400, response.statusCode());
    assertEquals("2.6", response.headers().firstValue("x-openrtb-version").orElseThrow());
  }

  @Test
  void returnsNoContentForNoBid() throws Exception {
    startServer(config(0.50d, 30.00d), store -> store.create("Banner Test", MediaType.BANNER, true));

    HttpResponse<String> response = post("""
        {"id":"req-1","imp":[{"id":"imp-1","bidfloor":1.0,"banner":{"w":300,"h":250}}]}
        """);

    assertEquals(204, response.statusCode());
    assertEquals("2.6", response.headers().firstValue("x-openrtb-version").orElseThrow());
  }

  @Test
  void returnsVideoBidForEligibleRequest() throws Exception {
    startServer(config(4.50d, 30.00d), store -> store.create("Video Test", MediaType.VIDEO, true));

    HttpResponse<String> response = post("""
        {"id":"req-video-1","imp":[{"id":"imp-video-1","bidfloor":18.5,"bidfloorcur":"USD","video":{"mimes":["video/mp4"],"minduration":15,"maxduration":30,"w":1920,"h":1080,"rqddurs":[15,30]},"pmp":{"private_auction":0,"deals":[{"id":"deal-1","bidfloor":18.0,"bidfloorcur":"USD","wseat":["kwidder"],"wadomain":["ads.kwidder.dev"]}]}}]}
        """);

    assertEquals(200, response.statusCode());
    assertEquals("2.6", response.headers().firstValue("x-openrtb-version").orElseThrow());
    assertEquals(true, response.body().contains("\"mtype\":2"));
    assertEquals(true, response.body().contains("\"dealid\":\"deal-1\""));
    assertEquals(true, response.body().contains("VAST version"));
  }

  @Test
  void returnsNoContentWhenNoMatchingLineItemExists() throws Exception {
    startServer(config(4.50d, 30.00d), null);

    HttpResponse<String> response = post("""
        {"id":"req-banner-1","imp":[{"id":"imp-banner-1","bidfloor":1.0,"banner":{"w":300,"h":250}}]}
        """);

    assertEquals(204, response.statusCode());
  }

  private void startServer(AppConfig config, java.util.function.Consumer<LineItemStore> seedData) throws IOException {
    LineItemStore lineItemStore = new LineItemStore();
    if (seedData != null) {
      seedData.accept(lineItemStore);
    }
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/openrtb2/auction", new AuctionHandler(new BidEngine(config, lineItemStore), JsonSupport.mapper()));
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

  private AppConfig config(double bannerMaxBidCpm, double videoMaxBidCpm) {
    return new AppConfig(
        0,
        "kwidder",
        "USD",
        1.25d,
        bannerMaxBidCpm,
        18.75d,
        videoMaxBidCpm,
        "ads.kwidder.dev",
        "creative-1",
        "campaign-1",
        "video-creative-1",
        "video-campaign-1",
        "https://cdn.kwidder.dev/video/test-15s.mp4",
        15
    );
  }
}
