package com.kwidder.bidder.config;

public record AppConfig(
    int port,
    String seat,
    String currency,
    double baseBidCpm,
    double maxBidCpm,
    String adDomain,
    String creativeId,
    String campaignId
) {
  public static AppConfig load() {
    return new AppConfig(
        envInt("PORT", 8080),
        env("KWIDDER_SEAT", "kwidder"),
        env("KWIDDER_CURRENCY", "USD"),
        envDouble("KWIDDER_BASE_BID_CPM", 1.25d),
        envDouble("KWIDDER_MAX_BID_CPM", 4.50d),
        env("KWIDDER_AD_DOMAIN", "ads.kwidder.dev"),
        env("KWIDDER_CREATIVE_ID", "kwidder-banner-001"),
        env("KWIDDER_CAMPAIGN_ID", "kwidder-campaign-001")
    );
  }

  private static String env(String key, String fallback) {
    String value = System.getenv(key);
    return value == null || value.isBlank() ? fallback : value;
  }

  private static int envInt(String key, int fallback) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  private static double envDouble(String key, double fallback) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }
}
