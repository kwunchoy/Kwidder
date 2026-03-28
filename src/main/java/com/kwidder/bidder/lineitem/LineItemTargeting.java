package com.kwidder.bidder.lineitem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record LineItemTargeting(
    List<Integer> deviceTypes,
    List<String> countries,
    List<String> regions,
    List<String> cities,
    List<String> zips
) {
  public LineItemTargeting {
    deviceTypes = deviceTypes == null ? List.of() : deviceTypes.stream()
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    countries = normalizeStrings(countries, true);
    regions = normalizeStrings(regions, false);
    cities = normalizeStrings(cities, false);
    zips = normalizeStrings(zips, false);
  }

  public static LineItemTargeting none() {
    return new LineItemTargeting(List.of(), List.of(), List.of(), List.of(), List.of());
  }

  public boolean hasDeviceTypeFilters() {
    return !deviceTypes.isEmpty();
  }

  public boolean hasGeoFilters() {
    return !countries.isEmpty() || !regions.isEmpty() || !cities.isEmpty() || !zips.isEmpty();
  }

  private static List<String> normalizeStrings(List<String> values, boolean uppercase) {
    if (values == null) {
      return List.of();
    }

    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String value : values) {
      if (value == null) {
        continue;
      }
      String trimmed = value.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      normalized.add(uppercase ? trimmed.toUpperCase(Locale.ROOT) : trimmed.toLowerCase(Locale.ROOT));
    }
    return List.copyOf(normalized);
  }
}
