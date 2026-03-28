package com.kwidder.bidder.lineitem;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LineItem(
    String id,
    String name,
    MediaType mediaType,
    boolean active,
    double bidCpm,
    double budget,
    double spent,
    LineItemTargeting targeting
) {
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

  public LineItem spend(double amount) {
    return new LineItem(id, name, mediaType, active, bidCpm, budget, Math.min(budget, spent + amount), targeting);
  }
}
