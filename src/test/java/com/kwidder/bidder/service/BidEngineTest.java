package com.kwidder.bidder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.kwidder.bidder.config.AppConfig;
import com.kwidder.bidder.model.openrtb.BidRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class BidEngineTest {
  @Test
  void returnsBannerBidForEligibleImpression() {
    BidEngine engine = new BidEngine(config(4.50d));
    BidRequest request = new BidRequest(
        "request-1",
        List.of(new BidRequest.Imp(
            "imp-1",
            new BidRequest.Banner(
                List.of(new BidRequest.Format(300, 250, null, null, null, null)),
                300,
                250,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            "slot-1",
            null,
            1.10d,
            "USD",
            1,
            null
        )),
        null,
        null,
        null,
        null,
        null,
        null,
        1,
        120,
        0,
        null,
        null,
        null,
        List.of("USD"),
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    var response = engine.evaluate(request);

    assertNotNull(response);
    assertEquals(1, response.seatbid().size());
    assertEquals(1, response.seatbid().get(0).bid().size());
    assertEquals(1.25d, response.seatbid().get(0).bid().get(0).price());
  }

  @Test
  void returnsNoBidForUnsupportedMedia() {
    BidEngine engine = new BidEngine(config(4.50d));
    BidRequest request = new BidRequest(
        "request-1",
        List.of(new BidRequest.Imp(
            "imp-1",
            null,
            new BidRequest.Video(null, null, null, null, 640, 360, null, null, null, null, null, null, null, null),
            null,
            null,
            null,
            null,
            null,
            1.00d,
            "USD",
            null,
            null
        )),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    assertNull(engine.evaluate(request));
  }

  private AppConfig config(double maxBidCpm) {
    return new AppConfig(8080, "kwidder", "USD", 1.25d, maxBidCpm, "ads.kwidder.dev", "creative-1", "campaign-1");
  }
}
