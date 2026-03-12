package com.kwidder.bidder.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BidRequest(
    @JsonProperty("id") String id,
    @JsonProperty("imp") List<Imp> imp,
    @JsonProperty("site") Site site,
    @JsonProperty("app") App app,
    @JsonProperty("device") Device device,
    @JsonProperty("user") User user,
    @JsonProperty("source") Source source,
    @JsonProperty("regs") Regs regs,
    @JsonProperty("at") Integer at,
    @JsonProperty("tmax") Integer tmax,
    @JsonProperty("test") Integer test,
    @JsonProperty("wseat") List<String> wseat,
    @JsonProperty("bseat") List<String> bseat,
    @JsonProperty("allimps") Integer allimps,
    @JsonProperty("cur") List<String> cur,
    @JsonProperty("wlang") List<String> wlang,
    @JsonProperty("acat") List<String> acat,
    @JsonProperty("cattax") Integer cattax,
    @JsonProperty("bcat") List<String> bcat,
    @JsonProperty("badv") List<String> badv,
    @JsonProperty("bapp") List<String> bapp,
    @JsonProperty("ext") JsonNode ext
) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Imp(
      @JsonProperty("id") String id,
      @JsonProperty("banner") Banner banner,
      @JsonProperty("video") Video video,
      @JsonProperty("audio") Audio audio,
      @JsonProperty("native") Native nativeFormat,
      @JsonProperty("pmp") PMP pmp,
      @JsonProperty("tagid") String tagid,
      @JsonProperty("instl") Integer instl,
      @JsonProperty("bidfloor") Double bidfloor,
      @JsonProperty("bidfloorcur") String bidfloorcur,
      @JsonProperty("secure") Integer secure,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Banner(
      @JsonProperty("format") List<Format> format,
      @JsonProperty("w") Integer w,
      @JsonProperty("h") Integer h,
      @JsonProperty("wmax") Integer wmax,
      @JsonProperty("hmax") Integer hmax,
      @JsonProperty("wmin") Integer wmin,
      @JsonProperty("hmin") Integer hmin,
      @JsonProperty("btype") List<Integer> btype,
      @JsonProperty("battr") List<Integer> battr,
      @JsonProperty("pos") Integer pos,
      @JsonProperty("api") List<Integer> api,
      @JsonProperty("topframe") Integer topframe,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Format(
      @JsonProperty("w") Integer w,
      @JsonProperty("h") Integer h,
      @JsonProperty("wratio") Integer wratio,
      @JsonProperty("hratio") Integer hratio,
      @JsonProperty("wmin") Integer wmin,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Video(
      @JsonProperty("mimes") List<String> mimes,
      @JsonProperty("minduration") Integer minduration,
      @JsonProperty("maxduration") Integer maxduration,
      @JsonProperty("protocols") List<Integer> protocols,
      @JsonProperty("w") Integer w,
      @JsonProperty("h") Integer h,
      @JsonProperty("placement") Integer placement,
      @JsonProperty("plcmt") Integer plcmt,
      @JsonProperty("rqddurs") List<Integer> rqddurs,
      @JsonProperty("podid") String podid,
      @JsonProperty("podseq") Integer podseq,
      @JsonProperty("mincpmpersec") Double mincpmpersec,
      @JsonProperty("durfloors") List<DurFloor> durfloors,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Audio(
      @JsonProperty("mimes") List<String> mimes,
      @JsonProperty("minduration") Integer minduration,
      @JsonProperty("maxduration") Integer maxduration,
      @JsonProperty("rqddurs") List<Integer> rqddurs,
      @JsonProperty("podid") String podid,
      @JsonProperty("podseq") Integer podseq,
      @JsonProperty("mincpmpersec") Double mincpmpersec,
      @JsonProperty("durfloors") List<DurFloor> durfloors,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Native(
      @JsonProperty("request") String request,
      @JsonProperty("ver") String ver,
      @JsonProperty("api") List<Integer> api,
      @JsonProperty("battr") List<Integer> battr,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PMP(
      @JsonProperty("private_auction") Integer privateAuction,
      @JsonProperty("deals") List<Deal> deals,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Deal(
      @JsonProperty("id") String id,
      @JsonProperty("bidfloor") Double bidfloor,
      @JsonProperty("bidfloorcur") String bidfloorcur,
      @JsonProperty("at") Integer at,
      @JsonProperty("wseat") List<String> wseat,
      @JsonProperty("wadomain") List<String> wadomain,
      @JsonProperty("guar") Integer guar,
      @JsonProperty("mincpmpersec") Double mincpmpersec,
      @JsonProperty("durfloors") List<DurFloor> durfloors,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record DurFloor(
      @JsonProperty("mindur") Integer mindur,
      @JsonProperty("maxdur") Integer maxdur,
      @JsonProperty("bidfloor") Double bidfloor,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Site(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("domain") String domain,
      @JsonProperty("page") String page,
      @JsonProperty("cattax") Integer cattax,
      @JsonProperty("cat") List<String> cat,
      @JsonProperty("publisher") Publisher publisher,
      @JsonProperty("content") Content content,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record App(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("bundle") String bundle,
      @JsonProperty("domain") String domain,
      @JsonProperty("storeurl") String storeurl,
      @JsonProperty("cattax") Integer cattax,
      @JsonProperty("cat") List<String> cat,
      @JsonProperty("publisher") Publisher publisher,
      @JsonProperty("content") Content content,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Publisher(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("domain") String domain,
      @JsonProperty("cattax") Integer cattax,
      @JsonProperty("cat") List<String> cat,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Content(
      @JsonProperty("id") String id,
      @JsonProperty("title") String title,
      @JsonProperty("series") String series,
      @JsonProperty("url") String url,
      @JsonProperty("language") String language,
      @JsonProperty("cattax") Integer cattax,
      @JsonProperty("cat") List<String> cat,
      @JsonProperty("producer") Producer producer,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Producer(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("domain") String domain,
      @JsonProperty("cattax") Integer cattax,
      @JsonProperty("cat") List<String> cat,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Device(
      @JsonProperty("ua") String ua,
      @JsonProperty("ip") String ip,
      @JsonProperty("ipv6") String ipv6,
      @JsonProperty("devicetype") Integer devicetype,
      @JsonProperty("make") String make,
      @JsonProperty("model") String model,
      @JsonProperty("os") String os,
      @JsonProperty("osv") String osv,
      @JsonProperty("language") String language,
      @JsonProperty("ifa") String ifa,
      @JsonProperty("geo") Geo geo,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Geo(
      @JsonProperty("lat") Double lat,
      @JsonProperty("lon") Double lon,
      @JsonProperty("country") String country,
      @JsonProperty("region") String region,
      @JsonProperty("city") String city,
      @JsonProperty("zip") String zip,
      @JsonProperty("utcoffset") Integer utcoffset,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record User(
      @JsonProperty("id") String id,
      @JsonProperty("buyeruid") String buyeruid,
      @JsonProperty("consent") String consent,
      @JsonProperty("eids") List<Eid> eids,
      @JsonProperty("data") List<Data> data,
      @JsonProperty("geo") Geo geo,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Data(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("segment") List<Segment> segment,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Segment(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("value") String value,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Eid(
      @JsonProperty("source") String source,
      @JsonProperty("uids") List<Uid> uids,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Uid(
      @JsonProperty("id") String id,
      @JsonProperty("atype") Integer atype,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Source(
      @JsonProperty("fd") Integer fd,
      @JsonProperty("tid") String tid,
      @JsonProperty("pchain") String pchain,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Regs(
      @JsonProperty("coppa") Integer coppa,
      @JsonProperty("gpp") String gpp,
      @JsonProperty("gpp_sid") List<Integer> gppSid,
      @JsonProperty("ext") JsonNode ext
  ) {
  }
}
