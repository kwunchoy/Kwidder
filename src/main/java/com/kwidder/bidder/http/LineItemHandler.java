package com.kwidder.bidder.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwidder.bidder.lineitem.CreateLineItemRequest;
import com.kwidder.bidder.lineitem.LineItem;
import com.kwidder.bidder.lineitem.LineItemStore;
import com.kwidder.bidder.lineitem.LineItemTargeting;
import com.kwidder.bidder.lineitem.MediaType;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

public final class LineItemHandler implements HttpHandler {
  private final LineItemStore lineItemStore;
  private final ObjectMapper mapper;

  public LineItemHandler(LineItemStore lineItemStore, ObjectMapper mapper) {
    this.lineItemStore = lineItemStore;
    this.mapper = mapper;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      switch (exchange.getRequestMethod().toUpperCase()) {
        case "GET" -> list(exchange);
        case "POST" -> create(exchange);
        case "DELETE" -> delete(exchange);
        default -> HttpResponses.writeStatus(exchange, 405);
      }
    } catch (IllegalArgumentException error) {
      HttpResponses.writeJson(exchange, 400, Map.of("error", error.getMessage()), mapper);
    }
  }

  private void list(HttpExchange exchange) throws IOException {
    List<LineItem> lineItems = lineItemStore.list();
    HttpResponses.writeJson(exchange, 200, Map.of("lineItems", lineItems), mapper);
  }

  private void create(HttpExchange exchange) throws IOException {
    CreateLineItemRequest request = mapper.readValue(HttpResponses.readBody(exchange), CreateLineItemRequest.class);
    MediaType mediaType = MediaType.fromString(request.mediaType());
    boolean active = request.active() == null || request.active();
    double bidCpm = request.bidCpm() == null ? 0.0d : request.bidCpm();
    double budget = request.budget() == null ? 0.0d : request.budget();
    Double dailyBudget = request.dailyBudget();
    LineItemTargeting targeting = request.targeting() == null ? LineItemTargeting.none() : request.targeting();
    LineItem lineItem = lineItemStore.create(
        request.name(),
        mediaType,
        active,
        request.startDate(),
        request.endDate(),
        bidCpm,
        budget,
        dailyBudget,
        targeting
    );
    HttpResponses.writeJson(exchange, 201, lineItem, mapper);
  }

  private void delete(HttpExchange exchange) throws IOException {
    String id = lineItemId(exchange.getRequestURI());
    if (id == null) {
      HttpResponses.writeStatus(exchange, 404);
      return;
    }

    if (!lineItemStore.delete(id)) {
      HttpResponses.writeStatus(exchange, 404);
      return;
    }

    HttpResponses.writeStatus(exchange, 204);
  }

  private String lineItemId(URI uri) {
    String path = uri.getPath();
    String prefix = "/api/line-items/";
    if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
      return null;
    }
    return path.substring(prefix.length());
  }
}
