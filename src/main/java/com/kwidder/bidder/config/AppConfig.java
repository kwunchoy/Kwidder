package com.kwidder.bidder.config;

public record AppConfig(
    int port,
    String seat,
    String currency,
    double bannerBaseBidCpm,
    double bannerMaxBidCpm,
    double videoBaseBidCpm,
    double videoMaxBidCpm,
    String adDomain,
    String bannerCreativeId,
    String bannerCampaignId,
    String videoCreativeId,
    String videoCampaignId,
    String videoMediaUrl,
    int videoDurationSeconds
) {
  public static AppConfig load() {
    return new AppConfig(
        envInt("PORT", 8080),
        env("KWIDDER_SEAT", "kwidder"),
        env("KWIDDER_CURRENCY", "USD"),
        envDouble("KWIDDER_BANNER_BASE_BID_CPM", 1.25d),
        envDouble("KWIDDER_BANNER_MAX_BID_CPM", 4.50d),
        envDouble("KWIDDER_VIDEO_BASE_BID_CPM", 18.75d),
        envDouble("KWIDDER_VIDEO_MAX_BID_CPM", 30.00d),
        env("KWIDDER_AD_DOMAIN", "ads.kwidder.dev"),
        env("KWIDDER_BANNER_CREATIVE_ID", "kwidder-banner-001"),
        env("KWIDDER_BANNER_CAMPAIGN_ID", "kwidder-campaign-001"),
        env("KWIDDER_VIDEO_CREATIVE_ID", "kwidder-vast-001"),
        env("KWIDDER_VIDEO_CAMPAIGN_ID", "kwidder-video-campaign-001"),
        env("KWIDDER_VIDEO_MEDIA_URL", "https://cdn.kwidder.dev/video/kwidder-ctv-15s.mp4"),
        envInt("KWIDDER_VIDEO_DURATION_SECONDS", 15)
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
