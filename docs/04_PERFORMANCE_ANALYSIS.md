# 04. Measured Performance & Network Analysis

## 1. Scope & Methodology

This performance analysis is based strictly on **empirically measured network metrics** collected via authenticated timing probes (`curl.exe` with formatted timing flags) targeting `www.cynbit.in` and `portal.cynbit.in` from an Indian client connection (resolving to Vercel's Mumbai Edge PoP: `bom1`).

No synthetic metrics, artificial benchmarks, or unverified backend processing times have been assumed or included.

---

## 2. Empirical Timing & Latency Telemetry

Measurements were collected using HTTP/1.1 over TLS 1.3 against the root path (`/`) of both target hosts:

| Metric / Phase | `www.cynbit.in` | `portal.cynbit.in` | Description |
| :--- | :--- | :--- | :--- |
| **Resolved IP** | `64.29.17.65` | `64.29.17.65` | Vercel Anycast edge routing |
| **Edge Node / PoP** | `bom1` (Mumbai, IN) | `bom1` (Mumbai, IN) | Geographic egress location |
| **TCP Handshake Time** | **$59.03\text{ ms}$** | **$110.07\text{ ms}$** | Socket connection latency |
| **TLS Handshake Time** | **$75.51\text{ ms}$** ($134.54\text{ ms}$ cumulative) | **$98.69\text{ ms}$** ($208.76\text{ ms}$ cumulative) | TLS certificate & key negotiation |
| **Time to First Byte (TTFB)** | **$463.91\text{ ms}$** | **$301.28\text{ ms}$** | Request dispatch to initial response byte |
| **Total Transfer Time** | **$464.18\text{ ms}$** | **$301.47\text{ ms}$** | Complete transmission of HTTP body |
| **Payload Size (HTML)** | **$100,498\text{ bytes}$** ($\approx 98.1\text{ KB}$) | **$12,979\text{ bytes}$** ($\approx 12.7\text{ KB}$) | Raw uncompressed response size |
| **Edge Cache State** | `HIT` (`Age: 233854s`) | `HIT` (`Age: 232047s`) | Static pre-render cached at Vercel edge |

---

## 3. Analysis of Measured Observations

### 3.1 Edge Caching Efficiency
- Both `www.cynbit.in` and `portal.cynbit.in` serve pre-rendered HTML (`X-Nextjs-Prerender: 1`) directly from the edge cache (`X-Vercel-Cache: HIT`).
- The `Age` header indicated cached persistence of over $64$ hours ($233,854\text{ s}$ and $232,047\text{ s}$, respectively), demonstrating effective static asset retention at the CDN layer for public marketing pages.

### 3.2 Canonical Domain Redirection Overhead
Empirical tracing of the root apex domain revealed a multi-step redirection sequence:

```
[Client]
   │
   ├─► 1. http://cynbit.in/
   │      HTTP/1.0 308 Permanent Redirect (server: Vercel)
   │      Location: https://cynbit.in/
   │
   ├─► 2. https://cynbit.in/
   │      HTTP/1.1 307 Temporary Redirect (Server: Vercel)
   │      Cache-Control: public, max-age=0, must-revalidate
   │      Location: https://www.cynbit.in/
   │
   └─► 3. https://www.cynbit.in/
          HTTP/1.1 200 OK (Content-Length: 100498 bytes)
```

- **Observed Deficiency**:
  The transition from `https://cynbit.in/` to `https://www.cynbit.in/` uses an HTTP `307 Temporary Redirect` with `max-age=0, must-revalidate`.
- **Performance Consequence**:
  Unlike a `301 Moved Permanently` or `308 Permanent Redirect`, modern user agents and web crawlers are instructed not to cache the canonical domain target. Consequently, repeat visitors entering `cynbit.in` must execute an additional round-trip redirect before reaching the target document.

### 3.3 HTML Document Payload Sizing
- The homepage payload for `www.cynbit.in` is $100,498\text{ bytes}$ ($\approx 98.1\text{ KB}$), reflecting inline React Server Component serialized state chunks (`self.__next_f.push(...)`) alongside structured JSON-LD schema metadata.
- Pre-gzipped or brotli-compressed transfer over HTTP/2 or HTTP/3 mitigates wire weight during typical browser interactions.
