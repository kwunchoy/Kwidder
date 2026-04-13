package com.kwidder.bidder.lineitem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kwidder.bidder.http.JsonSupport;
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

    LineItemStore firstStore = new LineItemStore(storagePath, JsonSupport.mapper());
    LineItem created = firstStore.create(
        "Banner Durable",
        MediaType.BANNER,
        true,
        1.25d,
        5.00d,
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
    firstStore.reserveBids(MediaType.BANNER, 1.00d, 1, lineItem -> true);

    LineItemStore secondStore = new LineItemStore(storagePath, JsonSupport.mapper());

    assertEquals(true, Files.exists(storagePath));
    assertEquals(1, secondStore.list().size());
    LineItem reloaded = secondStore.list().get(0);
    assertEquals(created.id(), reloaded.id());
    assertEquals("Banner Durable", reloaded.name());
    assertEquals(1.25d, reloaded.bidCpm());
    assertEquals(5.00d, reloaded.budget());
    assertEquals(1.25d, reloaded.spent());
    assertEquals(3.75d, reloaded.remainingBudget());
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
}
