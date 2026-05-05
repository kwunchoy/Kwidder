package com.kwidder.bidder.lineitem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kwidder.bidder.http.JsonSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LineItemStoreTest {
  @TempDir
  Path tempDir;

  @Test
  void persistsLineItemsAndSpentAmountsAcrossStoreRestarts() throws Exception {
    Path storagePath = tempDir.resolve("line-items.json");
    Clock fixedClock = Clock.fixed(Instant.parse("2026-04-19T12:00:00Z"), ZoneOffset.UTC);

    LineItemStore firstStore = new LineItemStore(storagePath, JsonSupport.mapper(), fixedClock);
    LineItem created = firstStore.create(
        "Banner Durable",
        MediaType.BANNER,
        true,
        "2026-04-01",
        "2026-04-30",
        1.25d,
        5.00d,
        2.00d,
        3,
        new LineItemTargeting(
            List.of(2),
            List.of("USA"),
            List.of("CA"),
            List.of("Los Angeles"),
            List.of("90001"),
            List.of("iOS"),
            List.of("Safari"),
            List.of("sportswire.example"),
            List.of("com.streamarena.tv"),
            List.of("deal-123")
        )
    );
    firstStore.reserveBids(MediaType.BANNER, 1.00d, 1, "user.id:user-123", lineItem -> true);

    LineItemStore secondStore = new LineItemStore(storagePath, JsonSupport.mapper(), fixedClock);

    assertEquals(true, Files.exists(storagePath));
    assertEquals(1, secondStore.list().size());
    LineItem reloaded = secondStore.list().get(0);
    assertEquals(created.id(), reloaded.id());
    assertEquals("Banner Durable", reloaded.name());
    assertEquals("2026-04-01", reloaded.startDate());
    assertEquals("2026-04-30", reloaded.endDate());
    assertEquals(1.25d, reloaded.bidCpm());
    assertEquals(5.00d, reloaded.budget());
    assertEquals(2.00d, reloaded.dailyBudget());
    assertEquals(1.25d, reloaded.spent());
    assertEquals(3.75d, reloaded.remainingBudget());
    assertEquals(1.25d, reloaded.dailySpent());
    assertEquals("2026-04-19", reloaded.dailySpentDate());
    assertEquals(0.75d, reloaded.remainingDailyBudget());
    assertEquals(3, reloaded.frequencyCap());
    assertEquals(1, reloaded.frequencyCountFor("user.id:user-123"));
    assertEquals(List.of(2), reloaded.targeting().deviceTypes());
    assertEquals(List.of("USA"), reloaded.targeting().countries());
    assertEquals(List.of("ca"), reloaded.targeting().regions());
    assertEquals(List.of("los angeles"), reloaded.targeting().cities());
    assertEquals(List.of("90001"), reloaded.targeting().zips());
    assertEquals(List.of("ios"), reloaded.targeting().operatingSystems());
    assertEquals(List.of("safari"), reloaded.targeting().browserFamilies());
    assertEquals(List.of("sportswire.example"), reloaded.targeting().domains());
    assertEquals(List.of("com.streamarena.tv"), reloaded.targeting().appBundles());
    assertEquals(List.of("deal-123"), reloaded.targeting().dealIds());
  }

  @Test
  void automaticallyTurnsExpiredActiveLineItemInactive() throws Exception {
    Path storagePath = tempDir.resolve("expired-line-items.json");
    Clock fixedClock = Clock.fixed(Instant.parse("2026-04-19T12:00:00Z"), ZoneOffset.UTC);

    LineItemStore store = new LineItemStore(storagePath, JsonSupport.mapper(), fixedClock);
    store.create("Expired Banner", MediaType.BANNER, true, "2026-04-01", "2026-04-10", 1.25d, 5.00d, LineItemTargeting.none());

    LineItem reloaded = store.list().get(0);

    assertEquals(false, reloaded.active());
  }

  @Test
  void stopsReservingBidsWhenDailyBudgetCapIsReached() {
    Clock fixedClock = Clock.fixed(Instant.parse("2026-04-19T12:00:00Z"), ZoneOffset.UTC);
    LineItemStore store = new LineItemStore(fixedClock);
    store.create("Daily Capped Banner", MediaType.BANNER, true, 1.25d, 10.00d, 2.50d, LineItemTargeting.none());

    assertEquals(1, store.reserveBids(MediaType.BANNER, 1.00d, 1, lineItem -> true).size());
    assertEquals(1, store.reserveBids(MediaType.BANNER, 1.00d, 1, lineItem -> true).size());
    assertEquals(0, store.reserveBids(MediaType.BANNER, 1.00d, 1, lineItem -> true).size());
    assertEquals(2.50d, store.list().get(0).dailySpent());
  }

  @Test
  void resetsDailySpendWhenCalendarDayChanges() throws Exception {
    Path storagePath = tempDir.resolve("daily-reset-line-items.json");
    Clock dayOneClock = Clock.fixed(Instant.parse("2026-04-19T12:00:00Z"), ZoneOffset.UTC);
    Clock dayTwoClock = Clock.fixed(Instant.parse("2026-04-20T12:00:00Z"), ZoneOffset.UTC);

    LineItemStore dayOneStore = new LineItemStore(storagePath, JsonSupport.mapper(), dayOneClock);
    dayOneStore.create("Daily Reset Banner", MediaType.BANNER, true, 1.25d, 10.00d, 2.50d, LineItemTargeting.none());
    dayOneStore.reserveBids(MediaType.BANNER, 1.00d, 1, lineItem -> true);
    dayOneStore.reserveBids(MediaType.BANNER, 1.00d, 1, lineItem -> true);

    LineItemStore dayTwoStore = new LineItemStore(storagePath, JsonSupport.mapper(), dayTwoClock);
    LineItem reloaded = dayTwoStore.list().get(0);

    assertEquals(2.50d, reloaded.spent());
    assertEquals(0.0d, reloaded.dailySpent());
    assertEquals("2026-04-20", reloaded.dailySpentDate());
    assertEquals(2.50d, reloaded.remainingDailyBudget());
    assertEquals(1, dayTwoStore.reserveBids(MediaType.BANNER, 1.00d, 1, lineItem -> true).size());
  }

  @Test
  void stopsReservingBidsWhenFrequencyCapIsReachedForIdentity() {
    LineItemStore store = new LineItemStore();
    store.create("Frequency Capped Banner", MediaType.BANNER, true, 1.25d, 10.00d, null, 2, LineItemTargeting.none());

    assertEquals(1, store.reserveBids(MediaType.BANNER, 1.00d, 1, "user.id:user-123", lineItem -> true).size());
    assertEquals(1, store.reserveBids(MediaType.BANNER, 1.00d, 1, "USER.ID:USER-123", lineItem -> true).size());
    assertEquals(0, store.reserveBids(MediaType.BANNER, 1.00d, 1, "user.id:user-123", lineItem -> true).size());
    assertEquals(1, store.reserveBids(MediaType.BANNER, 1.00d, 1, "user.id:user-456", lineItem -> true).size());
    assertEquals(2, store.list().get(0).frequencyCountFor("user.id:user-123"));
  }
}
