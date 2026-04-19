package com.kwidder.bidder.lineitem;

public record CreateLineItemRequest(
    String name,
    String mediaType,
    Boolean active,
    String startDate,
    String endDate,
    Double bidCpm,
    Double budget,
    LineItemTargeting targeting
) {
}
