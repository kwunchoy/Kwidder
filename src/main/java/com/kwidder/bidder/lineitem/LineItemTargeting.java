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
    List<String> zips,
    List<String> operatingSystems,
    List<String> browserFamilies,
    List<String> domains,
    List<String> appBundles,
    List<String> dealIds
) {
  public LineItemTargeting(List<Integer> deviceTypes, List<String> countries, List<String> regions, List<String> cities, List<String> zips) {
    this(deviceTypes, countries, regions, cities, zips, List.of(), List.of(), List.of(), List.of(), List.of());
  }

  public LineItemTargeting(
      List<Integer> deviceTypes,
      List<String> countries,
      List<String> regions,
      List<String> cities,
      List<String> zips,
      List<String> operatingSystems,
      List<String> browserFamilies
  ) {
    this(deviceTypes, countries, regions, cities, zips, operatingSystems, browserFamilies, List.of(), List.of(), List.of());
  }

  public LineItemTargeting {
    deviceTypes = deviceTypes == null ? List.of() : deviceTypes.stream()
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    countries = normalizeStrings(countries, true);
    regions = normalizeStrings(regions, false);
    cities = normalizeStrings(cities, false);
    zips = normalizeStrings(zips, false);
    operatingSystems = normalizeStrings(operatingSystems, false);
    browserFamilies = normalizeStrings(browserFamilies, false);
    domains = normalizeStrings(domains, false);
    appBundles = normalizeStrings(appBundles, false);
    dealIds = normalizeStrings(dealIds, false);
  }

  public static LineItemTargeting none() {
    return new LineItemTargeting(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
  }

  public boolean hasDeviceTypeFilters() {
    return !deviceTypes.isEmpty();
  }

  public boolean hasGeoFilters() {
    return !countries.isEmpty() || !regions.isEmpty() || !cities.isEmpty() || !zips.isEmpty();
  }

  public boolean hasOperatingSystemFilters() {
    return !operatingSystems.isEmpty();
  }

  public boolean hasBrowserFamilyFilters() {
    return !browserFamilies.isEmpty();
  }

  public boolean hasDomainFilters() {
    return !domains.isEmpty();
  }

  public boolean hasAppBundleFilters() {
    return !appBundles.isEmpty();
  }

  public boolean hasDealIdFilters() {
    return !dealIds.isEmpty();
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
