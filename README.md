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
- Kwidder only bids when an active line item exists for the request media type
- Requests with no impressions are rejected as invalid
- Banner and video inventory use separate bid ceilings
- If the publisher blocks our advertiser domain via `badv`, we do not bid
- The bid price is derived from the configured media-type base CPM and the request floor

This gives us a safe place to add richer decisioning later such as user targeting, deal logic, pacing, creative selection, category filtering, frequency caps, and campaign budget controls.

## Run locally

Once Java 21 and Maven are available:

```bash
mvn test
mvn exec:java
```

The service reads configuration from environment variables. A starter `.env.example` is included.

Then open `http://localhost:8080/ui` to create line items, paste a bid request, and inspect Kwidder's response in the browser.

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
