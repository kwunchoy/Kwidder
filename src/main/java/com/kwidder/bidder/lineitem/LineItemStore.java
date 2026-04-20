package com.kwidder.bidder.lineitem;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;

public final class LineItemStore {
  private final ConcurrentHashMap<String, LineItem> lineItems = new ConcurrentHashMap<>();
  private final Path storagePath;
  private final ObjectMapper mapper;
  private final Clock clock;

  public LineItemStore() {
    this.storagePath = null;
    this.mapper = null;
    this.clock = Clock.systemDefaultZone();
  }

  public LineItemStore(Clock clock) {
    this.storagePath = null;
    this.mapper = null;
    this.clock = clock;
  }

  public LineItemStore(Path storagePath, ObjectMapper mapper) throws IOException {
    this(storagePath, mapper, Clock.systemDefaultZone());
  }

  public LineItemStore(Path storagePath, ObjectMapper mapper, Clock clock) throws IOException {
    this.storagePath = storagePath;
    this.mapper = mapper;
    this.clock = clock;
    load();
  }

  public synchronized LineItem create(String name, MediaType mediaType, boolean active, double bidCpm, double budget, LineItemTargeting targeting) {
    return create(name, mediaType, active, null, null, bidCpm, budget, null, targeting);
  }

  public synchronized LineItem create(
      String name,
      MediaType mediaType,
      boolean active,
      double bidCpm,
      double budget,
      Double dailyBudget,
      LineItemTargeting targeting
  ) {
    return create(name, mediaType, active, null, null, bidCpm, budget, dailyBudget, targeting);
  }

  public synchronized LineItem create(
      String name,
      MediaType mediaType,
      boolean active,
      String startDate,
      String endDate,
      double bidCpm,
      double budget,
      LineItemTargeting targeting
  ) {
    return create(name, mediaType, active, startDate, endDate, bidCpm, budget, null, targeting);
  }

  public synchronized LineItem create(
      String name,
      MediaType mediaType,
      boolean active,
      String startDate,
      String endDate,
      double bidCpm,
      double budget,
      Double dailyBudget,
      LineItemTargeting targeting
  ) {
    String normalizedName = name == null ? "" : name.trim();
    if (normalizedName.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    if (mediaType == null) {
      throw new IllegalArgumentException("mediaType is required");
    }
    if (!Double.isFinite(bidCpm) || bidCpm <= 0.0d) {
      throw new IllegalArgumentException("bidCpm must be greater than 0");
    }
    if (!Double.isFinite(budget) || budget <= 0.0d) {
      throw new IllegalArgumentException("budget must be greater than 0");
    }
    if (dailyBudget != null && (!Double.isFinite(dailyBudget) || dailyBudget <= 0.0d)) {
      throw new IllegalArgumentException("dailyBudget must be greater than 0");
    }
    LocalDate parsedStartDate = parseDate(startDate, "startDate");
    LocalDate parsedEndDate = parseDate(endDate, "endDate");
    if (parsedStartDate != null && parsedEndDate != null && parsedEndDate.isBefore(parsedStartDate)) {
      throw new IllegalArgumentException("endDate must be on or after startDate");
    }

    LineItem lineItem = normalizeLineItemState(new LineItem(
        UUID.randomUUID().toString(),
        normalizedName,
        mediaType,
        active,
        normalizedDate(parsedStartDate),
        normalizedDate(parsedEndDate),
        bidCpm,
        budget,
        0.0d,
        dailyBudget,
        0.0d,
        LocalDate.now(clock).toString(),
        targeting == null ? LineItemTargeting.none() : targeting
    ), LocalDate.now(clock));
    lineItems.put(lineItem.id(), lineItem);
    persist();
    return lineItem;
  }

  public List<LineItem> list() {
    refreshLineItemsForToday(LocalDate.now(clock));
    return lineItems.values().stream()
        .sorted(Comparator.comparing(LineItem::name).thenComparing(LineItem::id))
        .toList();
  }

  public synchronized boolean delete(String id) {
    if (id == null || id.isBlank()) {
      return false;
    }
    boolean deleted = lineItems.remove(id) != null;
    if (deleted) {
      persist();
    }
    return deleted;
  }

  public synchronized List<LineItem> reserveBids(MediaType mediaType, double floorCpm, int maxBids, Predicate<LineItem> matcher) {
    if (maxBids <= 0) {
      return List.of();
    }
    LocalDate today = LocalDate.now(clock);
    refreshLineItemsForToday(today);

    List<LineItem> selected = new ArrayList<>(lineItems.values()).stream()
        .filter(lineItem -> lineItem.isServingOn(today))
        .filter(lineItem -> lineItem.mediaType() == mediaType)
        .filter(lineItem -> lineItem.bidCpm() + 1e-9 >= floorCpm)
        .filter(lineItem -> lineItem.canAfford(lineItem.bidCpm()))
        .filter(lineItem -> matcher == null || matcher.test(lineItem))
        .sorted(Comparator.comparing(LineItem::bidCpm).reversed().thenComparing(LineItem::name).thenComparing(LineItem::id))
        .limit(maxBids)
        .toList();

    if (selected.isEmpty()) {
      return List.of();
    }

    List<LineItem> reserved = new ArrayList<>(selected.size());
    for (LineItem lineItem : selected) {
      LineItem updated = lineItem.spend(lineItem.bidCpm(), today);
      lineItems.put(updated.id(), updated);
      reserved.add(updated);
    }
    persist();
    return reserved;
  }

  private void load() throws IOException {
    if (storagePath == null || mapper == null || Files.notExists(storagePath)) {
      return;
    }

    StoredLineItems storedLineItems = mapper.readValue(storagePath.toFile(), StoredLineItems.class);
    lineItems.clear();
    if (storedLineItems == null || storedLineItems.lineItems() == null) {
      return;
    }

    for (LineItem lineItem : storedLineItems.lineItems()) {
      if (lineItem != null && lineItem.id() != null && !lineItem.id().isBlank()) {
        lineItems.put(lineItem.id(), normalizeLineItemState(lineItem, LocalDate.now(clock)));
      }
    }
    refreshLineItemsForToday(LocalDate.now(clock));
  }

  private void refreshLineItemsForToday(LocalDate today) {
    boolean changed = false;
    for (LineItem lineItem : new ArrayList<>(lineItems.values())) {
      LineItem normalized = normalizeLineItemState(lineItem, today);
      if (!normalized.equals(lineItem)) {
        lineItems.put(normalized.id(), normalized);
        changed = true;
      }
    }
    if (changed) {
      persist();
    }
  }

  private LineItem normalizeLineItemState(LineItem lineItem, LocalDate today) {
    if (lineItem == null) {
      return null;
    }
    LineItem normalized = lineItem.normalizedForDate(today);
    if (normalized.active() && normalized.hasEnded(today)) {
      return normalized.inactive();
    }
    return normalized;
  }

  private LocalDate parseDate(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException error) {
      throw new IllegalArgumentException(fieldName + " must be in YYYY-MM-DD format");
    }
  }

  private String normalizedDate(LocalDate value) {
    return value == null ? null : value.toString();
  }

  private void persist() {
    if (storagePath == null || mapper == null) {
      return;
    }

    try {
      Path parent = storagePath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      Path tempPath = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
      mapper.writerWithDefaultPrettyPrinter().writeValue(tempPath.toFile(), new StoredLineItems(list()));
      try {
        Files.move(tempPath, storagePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(tempPath, storagePath, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException error) {
      throw new UncheckedIOException("Unable to persist line items", error);
    }
  }

  private record StoredLineItems(@JsonProperty("lineItems") List<LineItem> lineItems) {
  }
}
