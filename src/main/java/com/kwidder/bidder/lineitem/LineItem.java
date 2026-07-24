package com.kwidder.bidder.lineitem;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
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
    List<FrequencyCap> frequencyCaps,
    Map<String, Map<String, Integer>> frequencyCapCounts,
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
    this(id, name, mediaType, active, null, null, bidCpm, budget, spent, null, 0.0d, null, null, Map.of(), List.of(), Map.of(), targeting);
  }

  public LineItem {
    frequencyCap = frequencyCap == null || frequencyCap <= 0 ? null : frequencyCap;
    frequencyCounts = normalizeFrequencyCounts(frequencyCounts);
    frequencyCaps = normalizeFrequencyCaps(frequencyCaps);
    frequencyCapCounts = normalizePeriodCounts(frequencyCapCounts);
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

  public boolean canServeTo(String frequencyKey, LocalDate today) {
    if (!canServeTo(frequencyKey)) {
      return false;
    }
    if (frequencyCaps.isEmpty()) {
      return true;
    }
    String normalizedKey = normalizedFrequencyKey(frequencyKey);
    if (normalizedKey == null || today == null) {
      return false;
    }
    for (FrequencyCap cap : frequencyCaps) {
      if (frequencyCountFor(cap.period(), normalizedKey, today) >= cap.limit()) {
        return false;
      }
    }
    return true;
  }

  public int frequencyCountFor(String frequencyKey) {
    String normalizedKey = normalizedFrequencyKey(frequencyKey);
    if (normalizedKey == null) {
      return 0;
    }
    return frequencyCounts.getOrDefault(normalizedKey, 0);
  }

  public int frequencyCountFor(FrequencyCapPeriod period, String frequencyKey, LocalDate date) {
    String normalizedKey = normalizedFrequencyKey(frequencyKey);
    if (period == null || normalizedKey == null || date == null) {
      return 0;
    }
    return frequencyCapCounts
        .getOrDefault(period.bucket(date), Map.of())
        .getOrDefault(normalizedKey, 0);
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
    return new LineItem(id, name, mediaType, false, startDate, endDate, bidCpm, budget, spent, dailyBudget, dailySpent, dailySpentDate, frequencyCap, frequencyCounts, frequencyCaps, frequencyCapCounts, targeting);
  }

  public LineItem normalizedForDate(LocalDate today) {
    String normalizedToday = today == null ? null : today.toString();
    if (normalizedToday == null) {
      return this;
    }
    double normalizedDailySpent = normalizedToday.equals(dailySpentDate) ? dailySpent : 0.0d;
    Map<String, Map<String, Integer>> normalizedCapCounts = currentPeriodCounts(today);
    if (normalizedToday.equals(dailySpentDate)
        && Double.compare(normalizedDailySpent, dailySpent) == 0
        && normalizedCapCounts.equals(frequencyCapCounts)) {
      return this;
    }
    return new LineItem(
        id,
        name,
        mediaType,
        active,
        startDate,
        endDate,
        bidCpm,
        budget,
        spent,
        dailyBudget,
        normalizedDailySpent,
        normalizedToday,
        frequencyCap,
        frequencyCounts,
        frequencyCaps,
        normalizedCapCounts,
        targeting
    );
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
    Map<String, Map<String, Integer>> nextPeriodCounts = current.incrementedPeriodCounts(frequencyKey, today);
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
        frequencyCaps,
        nextPeriodCounts,
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

  private Map<String, Map<String, Integer>> incrementedPeriodCounts(String frequencyKey, LocalDate today) {
    if (frequencyCaps.isEmpty() || today == null) {
      return frequencyCapCounts;
    }
    String normalizedKey = normalizedFrequencyKey(frequencyKey);
    if (normalizedKey == null) {
      return frequencyCapCounts;
    }
    Map<String, Map<String, Integer>> nextCounts = new HashMap<>(frequencyCapCounts);
    for (FrequencyCap cap : frequencyCaps) {
      String bucket = cap.period().bucket(today);
      Map<String, Integer> bucketCounts = new HashMap<>(nextCounts.getOrDefault(bucket, Map.of()));
      bucketCounts.merge(normalizedKey, 1, Integer::sum);
      nextCounts.put(bucket, Map.copyOf(bucketCounts));
    }
    return Map.copyOf(nextCounts);
  }

  private Map<String, Map<String, Integer>> currentPeriodCounts(LocalDate today) {
    if (frequencyCaps.isEmpty() || frequencyCapCounts.isEmpty()) {
      return Map.of();
    }
    Map<String, Map<String, Integer>> current = new HashMap<>();
    for (FrequencyCap cap : frequencyCaps) {
      String bucket = cap.period().bucket(today);
      Map<String, Integer> counts = frequencyCapCounts.get(bucket);
      if (counts != null && !counts.isEmpty()) {
        current.put(bucket, counts);
      }
    }
    return Map.copyOf(current);
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

  private static List<FrequencyCap> normalizeFrequencyCaps(List<FrequencyCap> caps) {
    if (caps == null || caps.isEmpty()) {
      return List.of();
    }
    Map<FrequencyCapPeriod, Integer> limits = new EnumMap<>(FrequencyCapPeriod.class);
    for (FrequencyCap cap : caps) {
      if (cap != null && cap.period() != null && cap.limit() > 0) {
        limits.merge(cap.period(), cap.limit(), Math::min);
      }
    }
    List<FrequencyCap> normalized = new ArrayList<>();
    for (FrequencyCapPeriod period : FrequencyCapPeriod.values()) {
      Integer limit = limits.get(period);
      if (limit != null) {
        normalized.add(new FrequencyCap(period, limit));
      }
    }
    return List.copyOf(normalized);
  }

  private static Map<String, Map<String, Integer>> normalizePeriodCounts(
      Map<String, Map<String, Integer>> counts
  ) {
    if (counts == null || counts.isEmpty()) {
      return Map.of();
    }
    Map<String, Map<String, Integer>> normalized = new HashMap<>();
    for (Map.Entry<String, Map<String, Integer>> entry : counts.entrySet()) {
      if (entry.getKey() == null || entry.getKey().isBlank()) {
        continue;
      }
      Map<String, Integer> bucketCounts = normalizeFrequencyCounts(entry.getValue());
      if (!bucketCounts.isEmpty()) {
        normalized.put(entry.getKey().trim().toUpperCase(Locale.ROOT), bucketCounts);
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
