# Kwidder

Kwidder (Kwun's Bidder) is a Java demand-side platform (DSP) bidder focused on ingesting OpenRTB 2.6 bid requests and making deterministic bid or no-bid decisions. The scaffold is intentionally lean: a plain Java 21 HTTP service, Jackson-based request parsing, a pluggable bid engine, and tests that pin the current auction behavior.

## What this scaffold does

- Accepts `POST` bid requests on `/openrtb2/auction`
- Parses core OpenRTB 2.6 request objects with room for exchange-specific `ext` fields
- Applies a simple line-item-driven policy for banner and video impressions
- Returns either a valid OpenRTB bid response or HTTP `204 No Content` for a no-bid
- Returns HTTP `400` with no body for malformed bid requests
- Emits the `x-openrtb-version: 2.6` response header
- Exposes a browser UI on `/ui` for manual request testing
- Exposes `GET /healthz` for liveness checks

## Project layout

```text
src/main/java/com/kwidder/bidder/Application.java          Application entrypoint
src/main/java/com/kwidder/bidder/config/                   Environment-driven runtime settings
src/main/java/com/kwidder/bidder/model/openrtb/            OpenRTB request and response models
src/main/java/com/kwidder/bidder/service/                  Bid evaluation logic
src/main/java/com/kwidder/bidder/http/                     HTTP handlers and JSON support
src/test/java/com/kwidder/bidder/                          Unit and HTTP contract tests
examples/                                                  Sample OpenRTB request payloads
```

## Default bidding behavior

The first implementation is intentionally conservative so we have something predictable to iterate on:

- Banner and video impressions are supported today
- Kwidder only bids when an active line item matches the request media type
- Each line item sets its own bid CPM and total budget
- Kwidder spends line item budget on every bid response it returns, and stops bidding once that budget is exhausted
- Line items, budgets, and spent amounts persist across Kwidder restarts in a local JSON store
- Line items can target device types plus exact-match geo filters for country, region, city, and ZIP
- By default Kwidder returns one bid per impression, but a request can opt into multiple bids through `ext.kwidder.allow_multiple_bids` and `ext.kwidder.max_bids`
- Requests with no impressions are rejected as invalid
- If the publisher blocks our advertiser domain via `badv`, we do not bid
- A line item only bids when its bid CPM clears the request floor

This gives us a safe place to add richer decisioning later such as user targeting, deal logic, pacing, creative selection, category filtering, and frequency caps.

## Run locally

Once Java 21 and Maven are available:

```bash
mvn test
mvn exec:java
```

The service reads configuration from environment variables. A starter `.env.example` is included.
By default, line items are stored in `data/line-items.json`. You can change that with `KWIDDER_LINE_ITEM_STORE_PATH`.

Then open `http://localhost:8080/ui` to create line items with media type, bid CPM, budget, device type targeting, and geo targeting, paste a bid request, and inspect Kwidder's response in the browser.

To let one impression return multiple bids, include this request extension:

```json
{
  "ext": {
    "kwidder": {
      "allow_multiple_bids": true,
      "max_bids": 3
    }
  }
}
```

When enabled, Kwidder returns up to `max_bids` eligible bids for each impression, sorted from highest CPM to lowest CPM.

## Example request

```bash
curl -i \
  -X POST http://localhost:8080/openrtb2/auction \
  -H "Content-Type: application/json" \
  --data @examples/sample-banner-request.json
```

If the request is bid-eligible, you will get a JSON OpenRTB bid response. Otherwise the service returns `204 No Content`.

For a more exchange-like example with `source`, `regs`, `user.eids`, device geo, and a PMP deal, see:

- `examples/realistic-banner-request.json`
- `examples/realistic-banner-response.json`

For realistic video / CTV examples, see:

- `examples/realistic-video-request.json`
- `examples/realistic-video-response.json`

## Repo workflow

Once your GitHub repo exists, link it with:

```bash
git init
git add .
git commit -m "Initial Kwidder Java bidder scaffold"
git branch -M main
git remote add origin <your-github-repo-url>
git push -u origin main
```

## Feature roadmap

Here are 10 useful features to add next to make Kwidder feel more like a real bidder platform:

1. Video Ad Podding Support
Handle pod-level decisioning for CTV and long-form video, including pod position, slot selection, and competitive separation.

2. Campaign Budgets and Pacing
Track spend caps, daily budgets, and pacing logic so Kwidder does not overspend.

3. Domain and App Targeting
Allow line items to target specific site domains, app bundles, publishers, or placement IDs.

4. Geo Targeting
Support country, region, city, and ZIP targeting using the device geo object.

5. Device Targeting
Let line items target mobile, desktop, CTV, operating systems, and browser families.

6. Deal-Aware Bidding
Support PMP deal prioritization, preferred pricing, and deal-specific creatives.

7. Creative Library
Manage multiple banner and video creatives per line item and choose the best creative dynamically.

8. Frequency Capping
Limit how often the same user sees a campaign by using IDs like `user.id`, `buyeruid`, or `eids`.

9. Win Notification and Impression Tracking
Add endpoints for `nurl`, impression logging, click tracking, and billing event capture.

10. Analytics Dashboard
Show bid rate, no-bid reasons, win rate, spend, and per-line-item performance in the UI.
