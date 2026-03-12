package com.kwidder.bidder.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwidder.bidder.config.AppConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public final class HealthHandler implements HttpHandler {
  private final AppConfig config;
  private final ObjectMapper mapper;

  public HealthHandler(AppConfig config, ObjectMapper mapper) {
    this.config = config;
    this.mapper = mapper;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      HttpResponses.writeStatus(exchange, 405);
      return;
    }

    HttpResponses.writeHealth(exchange, config.seat(), mapper);
  }
}
