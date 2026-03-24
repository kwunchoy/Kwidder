package com.kwidder.bidder.lineitem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LineItemStore {
  private final ConcurrentHashMap<String, LineItem> lineItems = new ConcurrentHashMap<>();

  public LineItem create(String name, MediaType mediaType, boolean active, double bidCpm, double budget) {
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

    LineItem lineItem = new LineItem(UUID.randomUUID().toString(), normalizedName, mediaType, active, bidCpm, budget, 0.0d);
    lineItems.put(lineItem.id(), lineItem);
    return lineItem;
  }

  public List<LineItem> list() {
    return lineItems.values().stream()
        .sorted(Comparator.comparing(LineItem::name).thenComparing(LineItem::id))
        .toList();
  }

  public boolean delete(String id) {
    if (id == null || id.isBlank()) {
      return false;
    }
    return lineItems.remove(id) != null;
  }

  public synchronized List<LineItem> reserveBids(MediaType mediaType, double floorCpm, int maxBids) {
    if (maxBids <= 0) {
      return List.of();
    }

    List<LineItem> selected = new ArrayList<>(lineItems.values()).stream()
        .filter(LineItem::active)
        .filter(lineItem -> lineItem.mediaType() == mediaType)
        .filter(lineItem -> lineItem.bidCpm() + 1e-9 >= floorCpm)
        .filter(lineItem -> lineItem.canAfford(lineItem.bidCpm()))
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
    return reserved;
  }
}
