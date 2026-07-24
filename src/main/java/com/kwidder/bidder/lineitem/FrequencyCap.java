package com.kwidder.bidder.lineitem;

public record FrequencyCap(
    FrequencyCapPeriod period,
    int limit
) {
}
