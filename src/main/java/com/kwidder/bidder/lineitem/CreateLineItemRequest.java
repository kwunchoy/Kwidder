package com.kwidder.bidder.lineitem;

import java.util.List;

public record CreateLineItemRequest(
    String name,
    String mediaType,
    Boolean active,
    String startDate,
    String endDate,
    Double bidCpm,
    Double budget,
    Double dailyBudget,
    Integer frequencyCap,
    List<FrequencyCap> frequencyCaps,
    LineItemTargeting targeting
) {
}
