package com.kwidder.bidder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.kwidder.bidder.config.AppConfig;
import com.kwidder.bidder.http.JsonSupport;
import com.kwidder.bidder.lineitem.LineItemStore;
import com.kwidder.bidder.lineitem.LineItemTargeting;
import com.kwidder.bidder.lineitem.MediaType;
import com.kwidder.bidder.model.openrtb.BidRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class BidEngineTest {
  @Test
  void returnsBannerBidForEligibleImpression() {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Banner Test", MediaType.BANNER, true, 1.25d, 10.00d, LineItemTargeting.none());
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);
    BidRequest request = bannerRequest("request-1", 1.10d);

    var response = engine.evaluate(request);

    assertNotNull(response);
    assertEquals(1, response.seatbid().size());
    assertEquals(1, response.seatbid().get(0).bid().size());
    assertEquals(1.25d, response.seatbid().get(0).bid().get(0).price());
  }

  @Test
  void returnsVideoBidForEligibleImpression() {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Video Test", MediaType.VIDEO, true, 18.75d, 40.00d, LineItemTargeting.none());
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
    lineItemStore.create("Banner Test", MediaType.BANNER, true, 1.25d, 10.00d, LineItemTargeting.none());
    lineItemStore.create("Video Test", MediaType.VIDEO, true, 18.75d, 40.00d, LineItemTargeting.none());
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

    assertNull(engine.evaluate(bannerRequest("request-1", 1.10d)));
  }

  @Test
  void returnsNoBidWhenLineItemBidCpmDoesNotMeetFloor() {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Banner Test", MediaType.BANNER, true, 1.10d, 10.00d, LineItemTargeting.none());
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    assertNull(engine.evaluate(bannerRequest("request-1", 1.25d)));
  }

  @Test
  void stopsBiddingWhenLineItemBudgetIsSpent() {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Banner Test", MediaType.BANNER, true, 1.25d, 2.50d, LineItemTargeting.none());
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);
    BidRequest request = bannerRequest("request-1", 1.00d);

    assertNotNull(engine.evaluate(request));
    assertNotNull(engine.evaluate(request));
    assertNull(engine.evaluate(request));
  }

  @Test
  void returnsMultipleBannerBidsSortedByPriceWhenRequestAllowsIt() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create("Banner High", MediaType.BANNER, true, 2.25d, 10.00d, LineItemTargeting.none());
    lineItemStore.create("Banner Mid", MediaType.BANNER, true, 1.80d, 10.00d, LineItemTargeting.none());
    lineItemStore.create("Banner Low", MediaType.BANNER, true, 1.50d, 10.00d, LineItemTargeting.none());
    lineItemStore.create("Banner Too Low", MediaType.BANNER, true, 1.10d, 10.00d, LineItemTargeting.none());
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
    lineItemStore.create("Banner High", MediaType.BANNER, true, 2.25d, 10.00d, LineItemTargeting.none());
    lineItemStore.create("Banner Mid", MediaType.BANNER, true, 1.80d, 10.00d, LineItemTargeting.none());
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

  @Test
  void returnsBidWhenDeviceTypeTargetingMatches() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create(
        "Desktop Banner",
        MediaType.BANNER,
        true,
        1.25d,
        10.00d,
        new LineItemTargeting(List.of(2), List.of(), List.of(), List.of(), List.of())
    );
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-device-match",
          "imp": [{"id": "imp-1", "bidfloor": 1.0, "banner": {"w": 300, "h": 250}}],
          "device": {"devicetype": 2}
        }
        """, BidRequest.class);

    assertNotNull(engine.evaluate(request));
  }

  @Test
  void returnsNoBidWhenDeviceTypeTargetingDoesNotMatch() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create(
        "CTV Banner",
        MediaType.BANNER,
        true,
        1.25d,
        10.00d,
        new LineItemTargeting(List.of(3), List.of(), List.of(), List.of(), List.of())
    );
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-device-miss",
          "imp": [{"id": "imp-1", "bidfloor": 1.0, "banner": {"w": 300, "h": 250}}],
          "device": {"devicetype": 2}
        }
        """, BidRequest.class);

    assertNull(engine.evaluate(request));
  }

  @Test
  void returnsBidWhenGeoTargetingMatches() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create(
        "California Banner",
        MediaType.BANNER,
        true,
        1.25d,
        10.00d,
        new LineItemTargeting(List.of(), List.of("USA"), List.of("CA"), List.of("Los Angeles"), List.of("90001"))
    );
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-geo-match",
          "imp": [{"id": "imp-1", "bidfloor": 1.0, "banner": {"w": 300, "h": 250}}],
          "device": {
            "devicetype": 2,
            "geo": {"country": "USA", "region": "CA", "city": "Los Angeles", "zip": "90001"}
          }
        }
        """, BidRequest.class);

    assertNotNull(engine.evaluate(request));
  }

  @Test
  void returnsNoBidWhenGeoTargetingDoesNotMatch() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create(
        "New York Banner",
        MediaType.BANNER,
        true,
        1.25d,
        10.00d,
        new LineItemTargeting(List.of(), List.of("USA"), List.of("NY"), List.of(), List.of())
    );
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-geo-miss",
          "imp": [{"id": "imp-1", "bidfloor": 1.0, "banner": {"w": 300, "h": 250}}],
          "device": {
            "devicetype": 2,
            "geo": {"country": "USA", "region": "CA", "city": "Los Angeles", "zip": "90001"}
          }
        }
        """, BidRequest.class);

    assertNull(engine.evaluate(request));
  }

  @Test
  void returnsBidWhenOperatingSystemTargetingMatches() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create(
        "iOS Banner",
        MediaType.BANNER,
        true,
        1.25d,
        10.00d,
        new LineItemTargeting(List.of(), List.of(), List.of(), List.of(), List.of(), List.of("iOS"), List.of())
    );
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-os-match",
          "imp": [{"id": "imp-1", "bidfloor": 1.0, "banner": {"w": 300, "h": 250}}],
          "device": {"os": "iOS"}
        }
        """, BidRequest.class);

    assertNotNull(engine.evaluate(request));
  }

  @Test
  void returnsNoBidWhenBrowserFamilyTargetingDoesNotMatch() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create(
        "Safari Banner",
        MediaType.BANNER,
        true,
        1.25d,
        10.00d,
        new LineItemTargeting(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of("Safari"))
    );
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-browser-miss",
          "imp": [{"id": "imp-1", "bidfloor": 1.0, "banner": {"w": 300, "h": 250}}],
          "device": {
            "ua": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
          }
        }
        """, BidRequest.class);

    assertNull(engine.evaluate(request));
  }

  @Test
  void returnsBidWhenDomainTargetingMatches() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create(
        "Sports Domain Banner",
        MediaType.BANNER,
        true,
        1.25d,
        10.00d,
        new LineItemTargeting(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of("sportswire.example"), List.of(), List.of())
    );
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-domain-match",
          "imp": [{"id": "imp-1", "bidfloor": 1.0, "banner": {"w": 300, "h": 250}}],
          "site": {"domain": "sportswire.example"}
        }
        """, BidRequest.class);

    assertNotNull(engine.evaluate(request));
  }

  @Test
  void returnsBidWhenAppBundleTargetingMatches() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create(
        "CTV App Banner",
        MediaType.BANNER,
        true,
        1.25d,
        10.00d,
        new LineItemTargeting(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of("com.streamarena.tv"), List.of())
    );
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-app-match",
          "imp": [{"id": "imp-1", "bidfloor": 1.0, "banner": {"w": 300, "h": 250}}],
          "app": {"bundle": "com.streamarena.tv"}
        }
        """, BidRequest.class);

    assertNotNull(engine.evaluate(request));
  }

  @Test
  void returnsBidWhenDealIdTargetingMatches() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create(
        "Deal Banner",
        MediaType.BANNER,
        true,
        1.25d,
        10.00d,
        new LineItemTargeting(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of("deal-123"))
    );
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-deal-match",
          "imp": [
            {
              "id": "imp-1",
              "bidfloor": 1.0,
              "banner": {"w": 300, "h": 250},
              "pmp": {
                "private_auction": 0,
                "deals": [{"id": "deal-123", "bidfloor": 1.0, "wseat": ["kwidder"], "wadomain": ["ads.kwidder.dev"]}]
              }
            }
          ]
        }
        """, BidRequest.class);

    assertNotNull(engine.evaluate(request));
  }

  @Test
  void returnsNoBidWhenDealIdTargetingDoesNotMatch() throws Exception {
    LineItemStore lineItemStore = new LineItemStore();
    lineItemStore.create(
        "Deal Banner",
        MediaType.BANNER,
        true,
        1.25d,
        10.00d,
        new LineItemTargeting(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of("deal-999"))
    );
    BidEngine engine = new BidEngine(config(4.50d, 30.00d), lineItemStore);

    BidRequest request = JsonSupport.mapper().readValue("""
        {
          "id": "request-deal-miss",
          "imp": [
            {
              "id": "imp-1",
              "bidfloor": 1.0,
              "banner": {"w": 300, "h": 250},
              "pmp": {
                "private_auction": 0,
                "deals": [{"id": "deal-123", "bidfloor": 1.0, "wseat": ["kwidder"], "wadomain": ["ads.kwidder.dev"]}]
              }
            }
          ]
        }
        """, BidRequest.class);

    assertNull(engine.evaluate(request));
  }

  private BidRequest bannerRequest(String requestId, double bidFloor) {
    return new BidRequest(
        requestId,
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
            bidFloor,
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
