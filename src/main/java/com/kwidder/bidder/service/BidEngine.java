package com.kwidder.bidder.service;

import com.kwidder.bidder.config.AppConfig;
import com.kwidder.bidder.model.openrtb.BidRequest;
import com.kwidder.bidder.model.openrtb.BidResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public final class BidEngine {
  private final AppConfig config;
  private final AtomicLong sequence = new AtomicLong();

  public BidEngine(AppConfig config) {
    this.config = config;
  }

  public BidResponse evaluate(BidRequest request) {
    if (request == null || request.imp() == null || request.imp().isEmpty()) {
      return null;
    }

    List<BidResponse.Bid> bids = new ArrayList<>();
    for (BidRequest.Imp imp : request.imp()) {
      BidResponse.Bid bid = evaluateImp(request, imp);
      if (bid != null) {
        bids.add(bid);
      }
    }

    if (bids.isEmpty()) {
      return null;
    }

    return new BidResponse(
        request.id(),
        List.of(new BidResponse.SeatBid(bids, config.seat(), null, null)),
        nextId("resp"),
        config.currency(),
        null,
        null
    );
  }

  private BidResponse.Bid evaluateImp(BidRequest request, BidRequest.Imp imp) {
    if (imp == null || imp.id() == null || imp.id().isBlank()) {
      return null;
    }
    if (imp.banner() == null) {
      return null;
    }

    double floor = imp.bidfloor() == null ? 0.0d : imp.bidfloor();
    if (floor > config.maxBidCpm()) {
      return null;
    }
    if (isBlockedAdvertiser(request.badv(), config.adDomain())) {
      return null;
    }

    BannerSize size = bannerSize(imp.banner());
    double price = priceForImp(floor);
    return new BidResponse.Bid(
        nextId("bid"),
        imp.id(),
        price,
        List.of(config.adDomain()),
        config.campaignId(),
        config.creativeId(),
        config.creativeId(),
        size.width(),
        size.height(),
        creativeMarkup(request.id(), imp.id(), size),
        1,
        firstDealId(imp),
        null
    );
  }

  private boolean isBlockedAdvertiser(List<String> badv, String adDomain) {
    if (badv == null) {
      return false;
    }
    for (String blocked : badv) {
      if (blocked != null && blocked.equalsIgnoreCase(adDomain)) {
        return true;
      }
    }
    return false;
  }

  private BannerSize bannerSize(BidRequest.Banner banner) {
    if (banner.w() != null && banner.h() != null && banner.w() > 0 && banner.h() > 0) {
      return new BannerSize(banner.w(), banner.h());
    }
    if (banner.format() != null && !banner.format().isEmpty()) {
      BidRequest.Format format = banner.format().get(0);
      if (format.w() != null && format.h() != null && format.w() > 0 && format.h() > 0) {
        return new BannerSize(format.w(), format.h());
      }
    }
    return new BannerSize(300, 250);
  }

  private double priceForImp(double floor) {
    double price = Math.max(config.baseBidCpm(), floor > 0.0d ? floor + 0.05d : config.baseBidCpm());
    return Math.min(price, config.maxBidCpm());
  }

  private String firstDealId(BidRequest.Imp imp) {
    if (imp.pmp() == null || imp.pmp().deals() == null || imp.pmp().deals().isEmpty()) {
      return null;
    }
    BidRequest.Deal deal = imp.pmp().deals().get(0);
    return deal == null ? null : deal.id();
  }

  private String creativeMarkup(String requestId, String impId, BannerSize size) {
    String target = "https://" + config.adDomain() + "/click?req=" + requestId + "&imp=" + impId;
    return """
        <a href="%s" target="_blank" rel="noopener noreferrer">
          <div style="width:%dpx;height:%dpx;background:#0b132b;color:#fdfdfd;display:flex;align-items:center;justify-content:center;font-family:Arial,sans-serif;">
            Kwidder
          </div>
        </a>
        """.formatted(target, size.width(), size.height()).replace("\r", "").replace("\n", "").trim();
  }

  private String nextId(String prefix) {
    return "%s-%06d".formatted(prefix.toLowerCase(Locale.ROOT), sequence.incrementAndGet());
  }

  private record BannerSize(int width, int height) {
  }
}
