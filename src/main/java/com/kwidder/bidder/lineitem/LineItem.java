package com.kwidder.bidder.lineitem;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public record LineItem(
    String id,
    String name,
    MediaType mediaType,
    boolean active,
    String startDate,
    String endDate,
    double bidCpm,
    double budget,
    double spent,
    LineItemTargeting targeting
) {
  public LineItem(
      String id,
      String name,
      MediaType mediaType,
      boolean active,
      double bidCpm,
      double budget,
      double spent,
      LineItemTargeting targeting
  ) {
    this(id, name, mediaType, active, null, null, bidCpm, budget, spent, targeting);
  }

  public LineItem {
    targeting = targeting == null ? LineItemTargeting.none() : targeting;
  }

  @JsonProperty("remainingBudget")
  public double remainingBudget() {
    return Math.max(0.0d, budget - spent);
  }

  public boolean canAfford(double amount) {
    return remainingBudget() + 1e-9 >= amount;
  }

  public boolean hasStarted(LocalDate today) {
    LocalDate start = parsedDate(startDate);
    return start == null || !today.isBefore(start);
  }

  public boolean hasEnded(LocalDate today) {
    LocalDate end = parsedDate(endDate);
    return end != null && end.isBefore(today);
  }

  public boolean isServingOn(LocalDate today) {
    return active && hasStarted(today) && !hasEnded(today);
  }

  public LineItem inactive() {
    return new LineItem(id, name, mediaType, false, startDate, endDate, bidCpm, budget, spent, targeting);
  }

  public LineItem spend(double amount) {
    return new LineItem(id, name, mediaType, active, startDate, endDate, bidCpm, budget, Math.min(budget, spent + amount), targeting);
  }

  private LocalDate parsedDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }
}
