package com.kwidder.bidder.lineitem;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
    Integer frequencyCap,
    Map<String, Integer> frequencyCounts,
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
    this(id, name, mediaType, active, null, null, bidCpm, budget, spent, null, 0.0d, null, null, Map.of(), targeting);
  }

  public LineItem {
    frequencyCap = frequencyCap == null || frequencyCap <= 0 ? null : frequencyCap;
    frequencyCounts = normalizeFrequencyCounts(frequencyCounts);
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

  public boolean hasFrequencyCap() {
    return frequencyCap != null;
  }

  public boolean canServeTo(String frequencyKey) {
    if (!hasFrequencyCap()) {
      return true;
    }
    String normalizedKey = normalizedFrequencyKey(frequencyKey);
    return normalizedKey != null && frequencyCountFor(normalizedKey) < frequencyCap;
  }

  public int frequencyCountFor(String frequencyKey) {
    String normalizedKey = normalizedFrequencyKey(frequencyKey);
    if (normalizedKey == null) {
      return 0;
    }
    return frequencyCounts.getOrDefault(normalizedKey, 0);
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
    return new LineItem(id, name, mediaType, false, startDate, endDate, bidCpm, budget, spent, dailyBudget, dailySpent, dailySpentDate, frequencyCap, frequencyCounts, targeting);
  }

  public LineItem normalizedForDate(LocalDate today) {
    String normalizedToday = today == null ? null : today.toString();
    if (normalizedToday == null || normalizedToday.equals(dailySpentDate)) {
      return this;
    }
    return new LineItem(id, name, mediaType, active, startDate, endDate, bidCpm, budget, spent, dailyBudget, 0.0d, normalizedToday, frequencyCap, frequencyCounts, targeting);
  }

  public LineItem spend(double amount, LocalDate today) {
    return spend(amount, today, null);
  }

  public LineItem spend(double amount, LocalDate today, String frequencyKey) {
    LineItem current = normalizedForDate(today);
    double nextSpent = Math.min(budget, current.spent + amount);
    double nextDailySpent = current.dailyBudget == null
        ? current.dailySpent + amount
        : Math.min(current.dailyBudget, current.dailySpent + amount);
    Map<String, Integer> nextFrequencyCounts = current.incrementedFrequencyCounts(frequencyKey);
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
        frequencyCap,
        nextFrequencyCounts,
        targeting
    );
  }

  private Map<String, Integer> incrementedFrequencyCounts(String frequencyKey) {
    if (!hasFrequencyCap()) {
      return frequencyCounts;
    }
    String normalizedKey = normalizedFrequencyKey(frequencyKey);
    if (normalizedKey == null) {
      return frequencyCounts;
    }
    Map<String, Integer> nextCounts = new HashMap<>(frequencyCounts);
    nextCounts.merge(normalizedKey, 1, Integer::sum);
    return Map.copyOf(nextCounts);
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

  private static Map<String, Integer> normalizeFrequencyCounts(Map<String, Integer> counts) {
    if (counts == null || counts.isEmpty()) {
      return Map.of();
    }
    Map<String, Integer> normalized = new HashMap<>();
    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
      String key = normalizedFrequencyKey(entry.getKey());
      Integer value = entry.getValue();
      if (key != null && value != null && value > 0) {
        normalized.merge(key, value, Integer::sum);
      }
    }
    return Map.copyOf(normalized);
  }

  private static String normalizedFrequencyKey(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }
}
