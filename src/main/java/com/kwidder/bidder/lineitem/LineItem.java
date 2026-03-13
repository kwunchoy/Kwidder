package com.kwidder.bidder.lineitem;

public record LineItem(
    String id,
    String name,
    MediaType mediaType,
    boolean active
) {
}
