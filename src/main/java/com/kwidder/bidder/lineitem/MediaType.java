package com.kwidder.bidder.lineitem;

public enum MediaType {
  BANNER,
  VIDEO;

  public static MediaType fromString(String value) {
    if (value == null) {
      throw new IllegalArgumentException("mediaType is required");
    }

    return MediaType.valueOf(value.trim().toUpperCase());
  }
}
