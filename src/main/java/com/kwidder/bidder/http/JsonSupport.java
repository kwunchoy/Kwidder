package com.kwidder.bidder.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonSupport {
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private JsonSupport() {
  }

  public static ObjectMapper mapper() {
    return MAPPER;
  }
}
