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
    Double dailyBudget,
    double dailySpent,
    String dailySpentDate,
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
    this(id, name, mediaType, active, null, null, bidCpm, budget, spent, null, 0.0d, null, targeting);
  }

  public LineItem {
    targeting = targeting == null ? LineItemTargeting.none() : targeting;
  }

  @JsonProperty("remainingBudget")
  public double remainingBudget() {
    return Math.max(0.0d, budget - spent);
  }

  @JsonProperty("remainingDailyBudget")
  public Double remainingDailyBudget() {
    if (dailyBudget == null) {
      return null;
    }
    return Math.max(0.0d, dailyBudget - dailySpent);
  }

  public boolean canAfford(double amount) {
    if (remainingBudget() + 1e-9 < amount) {
      return false;
    }
    return dailyBudget == null || remainingDailyBudget() + 1e-9 >= amount;
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
    return new LineItem(id, name, mediaType, false, startDate, endDate, bidCpm, budget, spent, dailyBudget, dailySpent, dailySpentDate, targeting);
  }

  public LineItem normalizedForDate(LocalDate today) {
    String normalizedToday = today == null ? null : today.toString();
    if (normalizedToday == null || normalizedToday.equals(dailySpentDate)) {
      return this;
    }
    return new LineItem(id, name, mediaType, active, startDate, endDate, bidCpm, budget, spent, dailyBudget, 0.0d, normalizedToday, targeting);
  }

  public LineItem spend(double amount, LocalDate today) {
    LineItem current = normalizedForDate(today);
    double nextSpent = Math.min(budget, current.spent + amount);
    double nextDailySpent = current.dailyBudget == null
        ? current.dailySpent + amount
        : Math.min(current.dailyBudget, current.dailySpent + amount);
    return new LineItem(
        id,
        name,
        mediaType,
        active,
        startDate,
        endDate,
        bidCpm,
        budget,
        nextSpent,
        dailyBudget,
        nextDailySpent,
        current.dailySpentDate,
        targeting
    );
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
