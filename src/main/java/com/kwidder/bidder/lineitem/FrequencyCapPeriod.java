package com.kwidder.bidder.lineitem;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.Locale;

public enum FrequencyCapPeriod {
  DAY,
  WEEK,
  MONTH;

  @JsonCreator
  public static FrequencyCapPeriod fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException error) {
      throw new IllegalArgumentException("frequency cap period must be DAY, WEEK, or MONTH");
    }
  }

  public String bucket(LocalDate date) {
    return switch (this) {
      case DAY -> "DAY:" + date;
      case WEEK -> {
        WeekFields fields = WeekFields.ISO;
        int year = date.get(fields.weekBasedYear());
        int week = date.get(fields.weekOfWeekBasedYear());
        yield "WEEK:%04d-W%02d".formatted(year, week);
      }
      case MONTH -> "MONTH:" + YearMonth.from(date);
    };
  }
}
