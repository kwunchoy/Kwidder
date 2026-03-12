package com.kwidder.bidder.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwidder.bidder.model.openrtb.BidRequest;
import com.kwidder.bidder.model.openrtb.BidResponse;
import com.kwidder.bidder.service.BidEngine;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public final class AuctionHandler implements HttpHandler {
  private final BidEngine bidEngine;
  private final ObjectMapper mapper;

  public AuctionHandler(BidEngine bidEngine, ObjectMapper mapper) {
    this.bidEngine = bidEngine;
    this.mapper = mapper;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
      HttpResponses.writeStatus(exchange, 405);
      return;
    }

    String body = HttpResponses.readBody(exchange);
    BidRequest request;
    try {
      request = mapper.readValue(body, BidRequest.class);
    } catch (Exception ignored) {
      HttpResponses.writeStatus(exchange, 400);
      return;
    }

    if (!isValid(request)) {
      HttpResponses.writeStatus(exchange, 400);
      return;
    }

    BidResponse response = bidEngine.evaluate(request);
    if (response == null) {
      HttpResponses.writeStatus(exchange, 204);
      return;
    }

    HttpResponses.writeJson(exchange, 200, response, mapper);
  }

  private boolean isValid(BidRequest request) {
    return request != null
        && request.id() != null
        && !request.id().isBlank()
        && request.imp() != null
        && !request.imp().isEmpty();
  }
}
