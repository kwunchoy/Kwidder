package com.kwidder.bidder.lineitem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LineItemStore {
  private final ConcurrentHashMap<String, LineItem> lineItems = new ConcurrentHashMap<>();

  public LineItem create(String name, MediaType mediaType, boolean active) {
    String normalizedName = name == null ? "" : name.trim();
    if (normalizedName.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }

    LineItem lineItem = new LineItem(UUID.randomUUID().toString(), normalizedName, mediaType, active);
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

  public boolean hasActiveLineItem(MediaType mediaType) {
    for (LineItem lineItem : new ArrayList<>(lineItems.values())) {
      if (lineItem.active() && lineItem.mediaType() == mediaType) {
        return true;
      }
    }
    return false;
  }
}
