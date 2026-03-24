package com.kwidder.bidder.lineitem;

public record CreateLineItemRequest(
    String name,
    String mediaType,
    Boolean active,
    Double bidCpm,
    Double budget
) {
}
