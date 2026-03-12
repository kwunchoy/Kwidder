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
    BidEngine engine = new BidEngine(config(4.50d, 30.00d));
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
  void returnsVideoBidForEligibleImpression() {
    BidEngine engine = new BidEngine(config(4.50d, 30.00d));
    BidRequest request = new BidRequest(
        "request-video-1",
        List.of(new BidRequest.Imp(
            "imp-video-1",
            null,
            new BidRequest.Video(
                List.of("video/mp4"),
                15,
                30,
                List.of(2, 3),
                1920,
                1080,
                1,
                1,
                List.of(15, 30),
                "pod-1",
                1,
                0.65d,
                List.of(new BidRequest.DurFloor(15, 15, 17.0d, null)),
                null
            ),
            null,
            null,
            new BidRequest.PMP(
                0,
                List.of(new BidRequest.Deal("deal-1", 18.0d, "USD", 1, List.of("kwidder"), List.of("ads.kwidder.dev"), null, null, null, null)),
                null
            ),
            null,
            null,
            18.5d,
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

    var response = engine.evaluate(request);

    assertNotNull(response);
    assertEquals(2, response.seatbid().get(0).bid().get(0).mtype());
    assertEquals(18.75d, response.seatbid().get(0).bid().get(0).price());
    assertEquals("deal-1", response.seatbid().get(0).bid().get(0).dealid());
  }

  @Test
  void returnsNoBidForUnsupportedMedia() {
    BidEngine engine = new BidEngine(config(4.50d, 30.00d));
    BidRequest request = new BidRequest(
        "request-1",
        List.of(new BidRequest.Imp(
            "imp-1",
            null,
            null,
            new BidRequest.Audio(List.of("audio/mp3"), 15, 30, null, null, null, null, null, null),
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

  private AppConfig config(double bannerMaxBidCpm, double videoMaxBidCpm) {
    return new AppConfig(
        8080,
        "kwidder",
        "USD",
        1.25d,
        bannerMaxBidCpm,
        18.75d,
        videoMaxBidCpm,
        "ads.kwidder.dev",
        "creative-1",
        "campaign-1",
        "video-creative-1",
        "video-campaign-1",
        "https://cdn.kwidder.dev/video/test-15s.mp4",
        15
    );
  }
}
