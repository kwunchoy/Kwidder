package com.kwidder.bidder.lineitem;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
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

  public LineItemStore() {
    this.storagePath = null;
    this.mapper = null;
  }

  public LineItemStore(Path storagePath, ObjectMapper mapper) throws IOException {
    this.storagePath = storagePath;
    this.mapper = mapper;
    load();
  }

  public synchronized LineItem create(String name, MediaType mediaType, boolean active, double bidCpm, double budget, LineItemTargeting targeting) {
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

    LineItem lineItem = new LineItem(
        UUID.randomUUID().toString(),
        normalizedName,
        mediaType,
        active,
        bidCpm,
        budget,
        0.0d,
        targeting == null ? LineItemTargeting.none() : targeting
    );
    lineItems.put(lineItem.id(), lineItem);
    persist();
    return lineItem;
  }

  public List<LineItem> list() {
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

    List<LineItem> selected = new ArrayList<>(lineItems.values()).stream()
        .filter(LineItem::active)
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
      LineItem updated = lineItem.spend(lineItem.bidCpm());
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
        lineItems.put(lineItem.id(), lineItem);
      }
    }
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
