package com.kwidder.bidder;

import com.kwidder.bidder.config.AppConfig;
import com.kwidder.bidder.http.AuctionHandler;
import com.kwidder.bidder.http.HealthHandler;
import com.kwidder.bidder.http.LineItemHandler;
import com.kwidder.bidder.http.JsonSupport;
import com.kwidder.bidder.http.RootHandler;
import com.kwidder.bidder.http.UiHandler;
import com.kwidder.bidder.lineitem.LineItemStore;
import com.kwidder.bidder.service.BidEngine;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class Application {
  private Application() {
  }

  public static void main(String[] args) throws IOException {
    AppConfig config = AppConfig.load();
    LineItemStore lineItemStore = new LineItemStore();
    BidEngine bidEngine = new BidEngine(config, lineItemStore);

    HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
    server.createContext("/", new RootHandler());
    server.createContext("/ui", new UiHandler());
    server.createContext("/healthz", new HealthHandler(config, JsonSupport.mapper()));
    server.createContext("/openrtb2/auction", new AuctionHandler(bidEngine, JsonSupport.mapper()));
    server.createContext("/api/line-items", new LineItemHandler(lineItemStore, JsonSupport.mapper()));
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
    System.out.printf("kwidder listening on :%d%n", config.port());
  }
}
