# Kwidder

Kwun's Bidder

Kwidder is a starter Java demand-side platform bidder focused on ingesting OpenRTB 2.6 bid requests and making deterministic bid or no-bid decisions. The scaffold is intentionally lean: a plain Java 21 HTTP service, Jackson-based request parsing, a pluggable bid engine, and tests that pin the current auction behavior.

## What this scaffold does

- Accepts `POST` bid requests on `/openrtb2/auction`
- Parses core OpenRTB 2.6 request objects with room for exchange-specific `ext` fields
- Applies a simple default policy for banner impressions
- Returns either a valid OpenRTB bid response or HTTP `204 No Content` for a no-bid
- Returns HTTP `400` with no body for malformed bid requests
- Emits the `x-openrtb-version: 2.6` response header
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

- Only banner impressions are bid today
- Requests with no impressions are rejected as invalid
- Banner impressions above the configured max CPM are skipped
- If the publisher blocks our advertiser domain via `badv`, we do not bid
- The bid price is derived from the configured base CPM and the request floor

This gives us a safe place to add richer decisioning later such as user targeting, deal logic, pacing, creative selection, category filtering, frequency caps, and campaign budget controls.

## Run locally

Once Java 21 and Maven are available:

```bash
mvn test
mvn exec:java
```

The service reads configuration from environment variables. A starter `.env.example` is included.

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

## Next build steps

- Expand OpenRTB 2.6 object coverage where your exchange integrations need stricter validation
- Add campaign, creative, and budget models
- Introduce bidder modules for targeting and pricing
- Add structured logging, metrics, and request tracing
- Add tests around exchange-specific request variations
