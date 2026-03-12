package com.kwidder.bidder.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public final class UiHandler implements HttpHandler {
  private static final String PAGE = """
      <!doctype html>
      <html lang="en">
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Kwidder Auction UI</title>
        <style>
          :root {
            --bg: #f2efe8;
            --panel: #fffdf8;
            --ink: #1f2937;
            --muted: #64748b;
            --accent: #0f766e;
            --accent-strong: #115e59;
            --border: #d7d2c9;
            --good: #166534;
            --warn: #92400e;
            --bad: #991b1b;
          }
          * { box-sizing: border-box; }
          body {
            margin: 0;
            font-family: Georgia, "Times New Roman", serif;
            color: var(--ink);
            background:
              radial-gradient(circle at top left, rgba(15,118,110,.14), transparent 28%),
              radial-gradient(circle at bottom right, rgba(180,83,9,.12), transparent 24%),
              linear-gradient(180deg, #f7f4ed 0%, var(--bg) 100%);
          }
          .wrap {
            max-width: 1280px;
            margin: 0 auto;
            padding: 32px 20px 40px;
          }
          .hero {
            display: grid;
            gap: 10px;
            margin-bottom: 24px;
          }
          h1 {
            margin: 0;
            font-size: clamp(2rem, 4vw, 3.4rem);
            line-height: 0.95;
            letter-spacing: -0.04em;
          }
          .sub {
            max-width: 760px;
            color: var(--muted);
            font-size: 1.02rem;
          }
          .grid {
            display: grid;
            grid-template-columns: 1.15fr 0.85fr;
            gap: 18px;
          }
          .panel {
            background: color-mix(in srgb, var(--panel) 92%, white 8%);
            border: 1px solid var(--border);
            border-radius: 18px;
            box-shadow: 0 8px 28px rgba(15, 23, 42, 0.06);
            overflow: hidden;
          }
          .panel-head {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 12px;
            padding: 16px 18px;
            border-bottom: 1px solid var(--border);
            background: rgba(255,255,255,.55);
          }
          .panel-head h2 {
            margin: 0;
            font-size: 1rem;
            text-transform: uppercase;
            letter-spacing: 0.08em;
          }
          .actions {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
          }
          button {
            border: 0;
            border-radius: 999px;
            padding: 10px 16px;
            font: inherit;
            cursor: pointer;
            transition: transform .12s ease, background-color .12s ease;
          }
          button:hover { transform: translateY(-1px); }
          .primary {
            background: var(--accent);
            color: white;
          }
          .primary:hover { background: var(--accent-strong); }
          .ghost {
            background: #ece7dd;
            color: var(--ink);
          }
          textarea, pre {
            width: 100%;
            min-height: 520px;
            margin: 0;
            border: 0;
            padding: 18px;
            background: transparent;
            color: var(--ink);
            font: 14px/1.55 "Cascadia Code", Consolas, monospace;
            tab-size: 2;
            white-space: pre-wrap;
            word-break: break-word;
          }
          textarea {
            resize: vertical;
            outline: none;
          }
          .meta {
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 12px;
            padding: 16px 18px;
            border-bottom: 1px solid var(--border);
            background: rgba(255,255,255,.5);
          }
          .meta-item {
            padding: 12px 14px;
            background: rgba(255,255,255,.65);
            border: 1px solid var(--border);
            border-radius: 14px;
          }
          .meta-label {
            display: block;
            margin-bottom: 6px;
            font-size: .72rem;
            letter-spacing: .08em;
            text-transform: uppercase;
            color: var(--muted);
          }
          .meta-value {
            font-family: "Cascadia Code", Consolas, monospace;
            font-size: .95rem;
          }
          .status-ok { color: var(--good); }
          .status-nobid { color: var(--warn); }
          .status-bad { color: var(--bad); }
          .foot {
            margin-top: 14px;
            color: var(--muted);
            font-size: .94rem;
          }
          @media (max-width: 980px) {
            .grid { grid-template-columns: 1fr; }
            textarea, pre { min-height: 360px; }
            .meta { grid-template-columns: 1fr; }
          }
        </style>
      </head>
      <body>
        <div class="wrap">
          <section class="hero">
            <h1>Kwidder Auction UI</h1>
            <div class="sub">
              Paste an OpenRTB 2.6 bid request, send it to Kwidder, and inspect the HTTP status, response headers, and returned bid payload.
            </div>
          </section>

          <section class="grid">
            <div class="panel">
              <div class="panel-head">
                <h2>Bid Request</h2>
                <div class="actions">
                  <button class="ghost" id="load-banner" type="button">Load Banner Example</button>
                  <button class="ghost" id="load-video" type="button">Load Video Example</button>
                  <button class="primary" id="send-request" type="button">Send To Kwidder</button>
                </div>
              </div>
              <textarea id="request-input" spellcheck="false"></textarea>
            </div>

            <div class="panel">
              <div class="panel-head">
                <h2>Bid Response</h2>
                <div class="actions">
                  <button class="ghost" id="format-response" type="button">Format Response</button>
                </div>
              </div>
              <div class="meta">
                <div class="meta-item">
                  <span class="meta-label">HTTP Status</span>
                  <span class="meta-value" id="status-code">Idle</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">OpenRTB Version</span>
                  <span class="meta-value" id="openrtb-version">-</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">Outcome</span>
                  <span class="meta-value" id="outcome">Awaiting request</span>
                </div>
              </div>
              <pre id="response-output">Send a request to see the response body here.</pre>
            </div>
          </section>

          <div class="foot">
            Kwidder posts requests to <code>/openrtb2/auction</code> and shows <code>204 No Content</code> when the engine decides not to bid.
          </div>
        </div>

        <script>
          const bannerExample = {
            id: "auction-001",
            at: 1,
            tmax: 120,
            cur: ["USD"],
            imp: [
              {
                id: "imp-1",
                tagid: "homepage-top",
                bidfloor: 1.1,
                bidfloorcur: "USD",
                banner: {
                  format: [
                    { w: 300, h: 250 },
                    { w: 320, h: 50 }
                  ],
                  pos: 1,
                  topframe: 1
                }
              }
            ],
            site: {
              id: "site-123",
              name: "Example Publisher",
              domain: "publisher.example",
              page: "https://publisher.example/article/kwidder-launch"
            },
            device: {
              ua: "Mozilla/5.0",
              ip: "203.0.113.10",
              devicetype: 2,
              language: "en"
            },
            user: { id: "user-123" }
          };

          const videoExample = {
            id: "req-video-4dd9ef17-d3e8-4c93-8d75-0c9cb41d9301",
            at: 1,
            tmax: 250,
            cur: ["USD"],
            wseat: ["kwidder"],
            source: {
              fd: 1,
              tid: "4f819587-e4e8-4c08-93fb-2f4e5c48a812",
              pchain: "4a4d9b8f7c6d5e10:9988"
            },
            imp: [
              {
                id: "imp-video-1",
                tagid: "preroll-main-break",
                bidfloor: 18.5,
                bidfloorcur: "USD",
                secure: 1,
                video: {
                  mimes: ["video/mp4", "application/javascript"],
                  minduration: 15,
                  maxduration: 30,
                  protocols: [2, 3, 5, 6, 7, 8],
                  w: 1920,
                  h: 1080,
                  placement: 1,
                  plcmt: 1,
                  rqddurs: [15, 30]
                },
                pmp: {
                  private_auction: 0,
                  deals: [
                    {
                      id: "deal-ctv-premium-001",
                      bidfloor: 18.0,
                      bidfloorcur: "USD",
                      wseat: ["kwidder"],
                      wadomain: ["ads.kwidder.dev"]
                    }
                  ]
                }
              }
            ],
            app: {
              id: "app-ctv-100",
              name: "StreamArena",
              bundle: "com.streamarena.tv"
            },
            device: {
              ua: "Mozilla/5.0 (Linux; Android 12; SHIELD Android TV)",
              ip: "198.51.100.44",
              devicetype: 3,
              make: "NVIDIA",
              model: "SHIELD Android TV",
              os: "Android TV",
              osv: "12",
              language: "en"
            }
          };

          const requestInput = document.getElementById("request-input");
          const responseOutput = document.getElementById("response-output");
          const statusCode = document.getElementById("status-code");
          const openrtbVersion = document.getElementById("openrtb-version");
          const outcome = document.getElementById("outcome");

          function setRequest(example) {
            requestInput.value = JSON.stringify(example, null, 2);
          }

          function setStatus(code) {
            statusCode.textContent = code;
            statusCode.className = "meta-value";
            if (code === 200) {
              statusCode.classList.add("status-ok");
            } else if (code === 204) {
              statusCode.classList.add("status-nobid");
            } else if (code >= 400) {
              statusCode.classList.add("status-bad");
            }
          }

          async function sendRequest() {
            let parsed;
            try {
              parsed = JSON.parse(requestInput.value);
            } catch (error) {
              setStatus("Invalid JSON");
              openrtbVersion.textContent = "-";
              outcome.textContent = "Fix request body";
              responseOutput.textContent = error.message;
              return;
            }

            responseOutput.textContent = "Sending request...";
            outcome.textContent = "Waiting for Kwidder";

            const response = await fetch("/openrtb2/auction", {
              method: "POST",
              headers: {
                "Content-Type": "application/json"
              },
              body: JSON.stringify(parsed)
            });

            setStatus(response.status);
            openrtbVersion.textContent = response.headers.get("x-openrtb-version") ?? "-";

            if (response.status === 204) {
              outcome.textContent = "No bid";
              responseOutput.textContent = "No Content";
              return;
            }

            if (response.status >= 400) {
              outcome.textContent = "Request rejected";
            } else {
              outcome.textContent = "Bid returned";
            }

            const text = await response.text();
            responseOutput.textContent = text || "(empty body)";
          }

          function formatResponse() {
            try {
              const parsed = JSON.parse(responseOutput.textContent);
              responseOutput.textContent = JSON.stringify(parsed, null, 2);
            } catch (_error) {
            }
          }

          document.getElementById("load-banner").addEventListener("click", () => setRequest(bannerExample));
          document.getElementById("load-video").addEventListener("click", () => setRequest(videoExample));
          document.getElementById("send-request").addEventListener("click", () => void sendRequest());
          document.getElementById("format-response").addEventListener("click", formatResponse);

          setRequest(bannerExample);
        </script>
      </body>
      </html>
      """;

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      HttpResponses.writeStatus(exchange, 405);
      return;
    }

    HttpResponses.writeHtml(exchange, 200, PAGE);
  }
}
