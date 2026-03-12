package com.kwidder.bidder.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public final class RootHandler implements HttpHandler {
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().set("Location", "/ui");
    exchange.getResponseHeaders().set("x-openrtb-version", "2.6");
    exchange.sendResponseHeaders(302, -1);
    exchange.close();
  }
}
