package com.kwidder.bidder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.kwidder.bidder.config.AppConfig;
import com.kwidder.bidder.http.JsonSupport;
import com.kwidder.bidder.lineitem.LineItemStore;
import com.kwidder.bidder.lineitem.MediaType;
import com.kwidder.bidder.model.openrtb.BidRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class BidEngineTest {
  @Test
  void returnsBannerBidForEligibleImpression() {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Banner Test", MediaType.BANNER, true, 1.25d, 10.00d);
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);
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
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Video Test", MediaType.VIDEO, true, 18.75d, 40.00d);
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);
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
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Banner Test", MediaType.BANNER, true, 1.25d, 10.00d);
    lineItemStore.create("Video Test", MediaType.VIDEO, true, 18.75d, 40.00d);
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);
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

  @Test
  void returnsNoBidWhenNoMatchingLineItemExists() {
    LineItemStore lineItemStore = new LineItemStore();
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);
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

    assertNull(engine.evaluate(request));
  }

  @Test
  void returnsNoBidWhenLineItemBidCpmDoesNotMeetFloor() {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Banner Test", MediaType.BANNER, true, 1.10d, 10.00d);
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

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
            1.25d,
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

    assertNull(engine.evaluate(request));
  }

  @Test
  void stopsBiddingWhenLineItemBudgetIsSpent() {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Banner Test", MediaType.BANNER, true, 1.25d, 2.50d);
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);
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
            1.00d,
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

    assertNotNull(engine.evaluate(request));
    assertNotNull(engine.evaluate(request));
    assertNull(engine.evaluate(request));
  }

  @Test
  void returnsMultipleBannerBidsSortedByPriceWhenRequestAllowsIt() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Banner High", MediaType.BANNER, true, 2.25d, 10.00d);
    lineItemStore.create("Banner Mid", MediaType.BANNER, true, 1.80d, 10.00d);
    lineItemStore.create("Banner Low", MediaType.BANNER, true, 1.50d, 10.00d);
    lineItemStore.create("Banner Too Low", MediaType.BANNER, true, 1.10d, 10.00d);
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-multi-1",
          "imp": [
            {
              "id": "imp-1",
              "bidfloor": 1.25,
              "banner": {
                "w": 300,
                "h": 250
              }
            }
          ],
          "ext": {
            "kwidder": {
              "allow_multiple_bids": true,
              "max_bids": 3
            }
          }
        }
        """, BidRequest.class);

    var response = engine.evaluate(request);

    assertNotNull(response);
    assertEquals(3, response.seatbid().get(0).bid().size());
    assertEquals(2.25d, response.seatbid().get(0).bid().get(0).price());
    assertEquals(1.80d, response.seatbid().get(0).bid().get(1).price());
    assertEquals(1.50d, response.seatbid().get(0).bid().get(2).price());
  }

  @Test
  void defaultsToSingleBidWhenRequestDoesNotAllowMultipleBids() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Banner High", MediaType.BANNER, true, 2.25d, 10.00d);
    lineItemStore.create("Banner Mid", MediaType.BANNER, true, 1.80d, 10.00d);
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-single-1",
          "imp": [
            {
              "id": "imp-1",
              "bidfloor": 1.25,
              "banner": {
                "w": 300,
                "h": 250
              }
            }
          ]
        }
        """, BidRequest.class);

    var response = engine.evaluate(request);

    assertNotNull(response);
    assertEquals(1, response.seatbid().get(0).bid().size());
    assertEquals(2.25d, response.seatbid().get(0).bid().get(0).price());
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
        15,
        "build/test-line-items.json"
    );
  }
}
