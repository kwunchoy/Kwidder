package com.kwidder.bidder.service;

import com.kwidder.bidder.config.AppConfig;
import com.kwidder.bidder.lineitem.LineItem;
import com.kwidder.bidder.lineitem.LineItemStore;
import com.kwidder.bidder.lineitem.LineItemTargeting;
import com.kwidder.bidder.lineitem.MediaType;
import com.kwidder.bidder.model.openrtb.BidRequest;
import com.kwidder.bidder.model.openrtb.BidResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class BidEngine {
  private final AppConfig config;
  private final LineItemStore lineItemStore;
  private final AtomicLong sequence = new AtomicLong();

  public BidEngine(AppConfig config, LineItemStore lineItemStore) {
    this.config = config;
    this.lineItemStore = lineItemStore;
  }

  public BidResponse evaluate(BidRequest request) {
    if (request == null || request.imp() == null || request.imp().isEmpty()) {
      return null;
    }

    List<BidResponse.Bid> bids = new ArrayList<>();
    for (BidRequest.Imp imp : request.imp()) {
      List<BidResponse.Bid> impBids = evaluateImp(request, imp);
      if (!impBids.isEmpty()) {
        bids.addAll(impBids);
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

  private List<BidResponse.Bid> evaluateImp(BidRequest request, BidRequest.Imp imp) {
    if (imp == null || imp.id() == null || imp.id().isBlank()) {
      return List.of();
    }
    if (!isSeatAllowed(request)) {
      return List.of();
    }
    if (isBlockedAdvertiser(request.badv(), config.adDomain())) {
      return List.of();
    }

    EligibleDeal eligibleDeal = eligibleDeal(imp);
    if (eligibleDeal == null && isPrivateAuction(imp)) {
      return List.of();
    }

    if (imp.banner() != null) {
      return evaluateBannerImp(request, imp, eligibleDeal);
    }

    if (imp.video() != null) {
      return evaluateVideoImp(request, imp, eligibleDeal);
    }

    return List.of();
  }

  private List<BidResponse.Bid> evaluateBannerImp(BidRequest request, BidRequest.Imp imp, EligibleDeal eligibleDeal) {
    BannerSize size = bannerSize(imp.banner());
    double floor = effectiveFloor(imp, eligibleDeal, null);
    List<LineItem> lineItems = lineItemStore.reserveBids(
        MediaType.BANNER,
        floor,
        request.maxBidsPerImp(),
        lineItem -> matchesTargeting(lineItem, request)
    );
    if (lineItems.isEmpty()) {
      return List.of();
    }

    List<BidResponse.Bid> bids = new ArrayList<>(lineItems.size());
    for (LineItem lineItem : lineItems) {
      bids.add(new BidResponse.Bid(
          nextId("bid"),
          imp.id(),
          lineItem.bidCpm(),
          List.of(config.adDomain()),
          config.bannerCampaignId(),
          config.bannerCreativeId(),
          config.bannerCreativeId(),
          size.width(),
          size.height(),
          bannerMarkup(request.id(), imp.id(), size),
          1,
          dealId(eligibleDeal),
          null
      ));
    }
    return bids;
  }

  private List<BidResponse.Bid> evaluateVideoImp(BidRequest request, BidRequest.Imp imp, EligibleDeal eligibleDeal) {
    BidRequest.Video video = imp.video();
    if (!supportsVideoMime(video)) {
      return List.of();
    }

    VideoSpec videoSpec = videoSpec(video);
    double floor = effectiveFloor(imp, eligibleDeal, videoSpec.durationSeconds());
    List<LineItem> lineItems = lineItemStore.reserveBids(
        MediaType.VIDEO,
        floor,
        request.maxBidsPerImp(),
        lineItem -> matchesTargeting(lineItem, request)
    );
    if (lineItems.isEmpty()) {
      return List.of();
    }

    List<BidResponse.Bid> bids = new ArrayList<>(lineItems.size());
    for (LineItem lineItem : lineItems) {
      bids.add(new BidResponse.Bid(
          nextId("bid"),
          imp.id(),
          lineItem.bidCpm(),
          List.of(config.adDomain()),
          config.videoCampaignId(),
          config.videoCreativeId(),
          config.videoCreativeId(),
          videoSpec.width(),
          videoSpec.height(),
          vastMarkup(request.id(), imp.id(), videoSpec),
          2,
          dealId(eligibleDeal),
          null
      ));
    }
    return bids;
  }

  private boolean isSeatAllowed(BidRequest request) {
    if (request.wseat() != null && !request.wseat().isEmpty() && !request.wseat().contains(config.seat())) {
      return false;
    }
    return request.bseat() == null || !request.bseat().contains(config.seat());
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

  private boolean matchesTargeting(LineItem lineItem, BidRequest request) {
    LineItemTargeting targeting = lineItem.targeting();
    if (targeting == null) {
      return true;
    }
    return matchesDeviceType(targeting, request.device()) && matchesGeo(targeting, request);
  }

  private boolean matchesDeviceType(LineItemTargeting targeting, BidRequest.Device device) {
    if (!targeting.hasDeviceTypeFilters()) {
      return true;
    }
    if (device == null || device.devicetype() == null) {
      return false;
    }
    return targeting.deviceTypes().contains(device.devicetype());
  }

  private boolean matchesGeo(LineItemTargeting targeting, BidRequest request) {
    if (!targeting.hasGeoFilters()) {
      return true;
    }

    BidRequest.Geo geo = request.device() != null && request.device().geo() != null
        ? request.device().geo()
        : request.user() == null ? null : request.user().geo();
    if (geo == null) {
      return false;
    }

    return matchesTextFilter(targeting.countries(), geo.country(), true)
        && matchesTextFilter(targeting.regions(), geo.region(), false)
        && matchesTextFilter(targeting.cities(), geo.city(), false)
        && matchesTextFilter(targeting.zips(), geo.zip(), false);
  }

  private boolean matchesTextFilter(List<String> filters, String value, boolean uppercase) {
    if (filters == null || filters.isEmpty()) {
      return true;
    }
    if (value == null || value.isBlank()) {
      return false;
    }
    String normalized = uppercase
        ? value.trim().toUpperCase(Locale.ROOT)
        : value.trim().toLowerCase(Locale.ROOT);
    return filters.stream().filter(Objects::nonNull).anyMatch(normalized::equals);
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

  private VideoSpec videoSpec(BidRequest.Video video) {
    int width = video.w() != null && video.w() > 0 ? video.w() : 1920;
    int height = video.h() != null && video.h() > 0 ? video.h() : 1080;
    int durationSeconds = selectedDuration(video);
    return new VideoSpec(width, height, durationSeconds);
  }

  private int selectedDuration(BidRequest.Video video) {
    int preferredDuration = config.videoDurationSeconds();
    if (video.rqddurs() != null && !video.rqddurs().isEmpty()) {
      if (video.rqddurs().contains(preferredDuration)) {
        return preferredDuration;
      }
      return video.rqddurs().stream().min(Comparator.naturalOrder()).orElse(preferredDuration);
    }

    int minimumDuration = video.minduration() != null ? video.minduration() : preferredDuration;
    int maximumDuration = video.maxduration() != null ? video.maxduration() : preferredDuration;
    if (preferredDuration >= minimumDuration && preferredDuration <= maximumDuration) {
      return preferredDuration;
    }

    return minimumDuration;
  }

  private boolean supportsVideoMime(BidRequest.Video video) {
    return video.mimes() != null
        && video.mimes().stream().anyMatch(mime -> "video/mp4".equalsIgnoreCase(mime));
  }

  private EligibleDeal eligibleDeal(BidRequest.Imp imp) {
    if (imp.pmp() == null || imp.pmp().deals() == null) {
      return null;
    }

    for (BidRequest.Deal deal : imp.pmp().deals()) {
      if (deal == null) {
        continue;
      }
      if (!isSeatAllowed(deal.wseat())) {
        continue;
      }
      if (!isAdvertiserAllowed(deal.wadomain())) {
        continue;
      }
      return new EligibleDeal(deal.id(), deal.bidfloor(), deal.durfloors());
    }

    return null;
  }

  private boolean isPrivateAuction(BidRequest.Imp imp) {
    return imp.pmp() != null && imp.pmp().privateAuction() != null && imp.pmp().privateAuction() == 1;
  }

  private boolean isSeatAllowed(List<String> wseat) {
    return wseat == null || wseat.isEmpty() || wseat.contains(config.seat());
  }

  private boolean isAdvertiserAllowed(List<String> allowedDomains) {
    return allowedDomains == null || allowedDomains.isEmpty() || allowedDomains.contains(config.adDomain());
  }

  private double effectiveFloor(BidRequest.Imp imp, EligibleDeal eligibleDeal, Integer durationSeconds) {
    double floor = imp.bidfloor() == null ? 0.0d : imp.bidfloor();
    if (eligibleDeal != null && eligibleDeal.bidFloor() != null) {
      floor = Math.max(floor, eligibleDeal.bidFloor());
    }
    floor = Math.max(floor, durationFloor(imp.video() == null ? null : imp.video().durfloors(), durationSeconds));
    floor = Math.max(floor, durationFloor(eligibleDeal == null ? null : eligibleDeal.durFloors(), durationSeconds));
    return floor;
  }

  private double durationFloor(List<BidRequest.DurFloor> durFloors, Integer durationSeconds) {
    if (durFloors == null || durFloors.isEmpty() || durationSeconds == null) {
      return 0.0d;
    }

    double floor = 0.0d;
    for (BidRequest.DurFloor durFloor : durFloors) {
      if (durFloor == null || durFloor.bidfloor() == null) {
        continue;
      }
      int minimum = durFloor.mindur() == null ? 0 : durFloor.mindur();
      int maximum = durFloor.maxdur() == null ? Integer.MAX_VALUE : durFloor.maxdur();
      if (durationSeconds >= minimum && durationSeconds <= maximum) {
        floor = Math.max(floor, durFloor.bidfloor());
      }
    }
    return floor;
  }

  private String dealId(EligibleDeal eligibleDeal) {
    return eligibleDeal == null ? null : eligibleDeal.id();
  }

  private String bannerMarkup(String requestId, String impId, BannerSize size) {
    String target = "https://" + config.adDomain() + "/click?req=" + requestId + "&imp=" + impId;
    return """
        <a href="%s" target="_blank" rel="noopener noreferrer">
          <div style="width:%dpx;height:%dpx;background:#0b132b;color:#fdfdfd;display:flex;align-items:center;justify-content:center;font-family:Arial,sans-serif;">
            Kwidder
          </div>
        </a>
        """.formatted(target, size.width(), size.height()).replace("\r", "").replace("\n", "").trim();
  }

  private String vastMarkup(String requestId, String impId, VideoSpec videoSpec) {
    String impressionUrl = "https://" + config.adDomain() + "/impression?req=" + requestId + "&imp=" + impId;
    String clickUrl = "https://" + config.adDomain() + "/click?req=" + requestId + "&imp=" + impId;
    return """
        <VAST version="4.2"><Ad id="%s"><InLine><AdSystem>Kwidder</AdSystem><AdTitle>Kwidder Video Ad</AdTitle><Impression><![CDATA[%s]]></Impression><Creatives><Creative sequence="1"><Linear><Duration>%s</Duration><MediaFiles><MediaFile delivery="progressive" type="video/mp4" width="%d" height="%d"><![CDATA[%s]]></MediaFile></MediaFiles><VideoClicks><ClickThrough><![CDATA[%s]]></ClickThrough></VideoClicks></Linear></Creative></Creatives></InLine></Ad></VAST>
        """.formatted(
        config.videoCreativeId(),
        impressionUrl,
        formatDuration(videoSpec.durationSeconds()),
        videoSpec.width(),
        videoSpec.height(),
        config.videoMediaUrl(),
        clickUrl
    ).replace("\r", "").replace("\n", "").trim();
  }

  private String formatDuration(int seconds) {
    int hours = seconds / 3600;
    int minutes = (seconds % 3600) / 60;
    int remainingSeconds = seconds % 60;
    return "%02d:%02d:%02d".formatted(hours, minutes, remainingSeconds);
  }

  private String nextId(String prefix) {
    return "%s-%06d".formatted(prefix.toLowerCase(Locale.ROOT), sequence.incrementAndGet());
  }

  private record BannerSize(int width, int height) {
  }

  private record VideoSpec(int width, int height, int durationSeconds) {
  }

  private record EligibleDeal(String id, Double bidFloor, List<BidRequest.DurFloor> durFloors) {
  }
}
