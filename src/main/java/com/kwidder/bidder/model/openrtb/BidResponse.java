package com.kwidder.bidder.model.openrtb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BidResponse(
    @JsonProperty("id") String id,
    @JsonProperty("seatbid") List<SeatBid> seatbid,
    @JsonProperty("bidid") String bidid,
    @JsonProperty("cur") String cur,
    @JsonProperty("customdata") String customdata,
    @JsonProperty("ext") JsonNode ext
) {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record SeatBid(
      @JsonProperty("bid") List<Bid> bid,
      @JsonProperty("seat") String seat,
      @JsonProperty("group") Integer group,
      @JsonProperty("ext") JsonNode ext
  ) {
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Bid(
      @JsonProperty("id") String id,
      @JsonProperty("impid") String impid,
      @JsonProperty("price") double price,
      @JsonProperty("adomain") List<String> adomain,
      @JsonProperty("cid") String cid,
      @JsonProperty("crid") String crid,
      @JsonProperty("adid") String adid,
      @JsonProperty("w") Integer w,
      @JsonProperty("h") Integer h,
      @JsonProperty("adm") String adm,
      @JsonProperty("mtype") Integer mtype,
      @JsonProperty("dealid") String dealid,
      @JsonProperty("ext") JsonNode ext
  ) {
  }
}
