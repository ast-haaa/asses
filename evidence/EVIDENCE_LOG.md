# Evidence Log: Sanitized Technical Proofs

**Assessment**: Cynbit Technologies — Backend Developer Intern Technical Assessment  
**Target Hosts**: `https://www.cynbit.in/` & `https://portal.cynbit.in/`  
**Scope**: Passive, authorized, read-only reconnaissance  
**Status**: Sanitized publication log (redacted proprietary markup, direct telephone numbers, internal tool names, and unindexed internal route paths)

---

## 1. React Server Component (RSC) Template Disclosure

* **Observation Reference**: Finding F-01 / Finding 1.1 in [`docs/03_SECURITY_FINDINGS.md`](../docs/03_SECURITY_FINDINGS.md)
* **Target Request**: `GET /dashboard` on `portal.cynbit.in` (without active session cookie)
* **HTTP Response Status**: `HTTP/1.1 307 Temporary Redirect`
* **Response Header Highlights**:
  ```http
  HTTP/1.1 307 Temporary Redirect
  Date: Sun, 01 Nov 2026 05:54:10 GMT
  Server: Vercel
  Location: /
  X-Matched-Path: /portal/dashboard
  X-Powered-By: Next.js
  Transfer-Encoding: chunked
  ```

### Sanitized Evidence Excerpt
```html
<!DOCTYPE html>
<html id="__next_error__">
<head>
  <meta charSet="utf-8"/>
  <meta name="robots" content="noindex, nofollow, nocache"/>
  <title>Cynbit Portal | Cynbit Technologies</title>
</head>
<body>
  <!-- Next.js streamed flight chunks delivered in the 307 redirect body -->
  <script>(self.__next_f=self.__next_f||[]).push([0])</script>
  <script>self.__next_f.push([1,"...
    [\"$\",\"h3\",null,{\"children\":\"[Internal Tool Title A]\"}],
    [\"$\",\"p\",null,{\"children\":\"[Sanitized operational description A]\"}],
    [\"$\",\"a\",null,{\"href\":\"[REDACTED_INTERNAL_ROUTE_A]\",\"children\":\"Get Started\"}],

    [\"$\",\"h3\",null,{\"children\":\"[Internal Tool Title B]\"}],
    [\"$\",\"p\",null,{\"children\":\"[Sanitized operational description B]\"}],
    [\"$\",\"a\",null,{\"href\":\"[REDACTED_INTERNAL_ROUTE_B]\",\"children\":\"Get Started\"}],

    [\"$\",\"h2\",null,{\"children\":\"[Internal Document Log Section]\"}],
    [\"$\",\"p\",null,{\"children\":\"[Sanitized log description]\"}],
  ..."])</script>
  <script>self.__next_f.push([1,"7:E{\"digest\":\"NEXT_REDIRECT;replace;/;307;\"}\n"])</script>
</body>
</html>
```

### Empirical Scope & Technical Boundary
- **What the Evidence Demonstrates**: The captured wire trace shows that an unauthenticated HTTP `GET /dashboard` request received an HTTP 307 redirect status code while concurrently delivering a chunk-encoded body containing a serialized React Server Component (RSC) flight payload with UI layout strings, followed by a client-side redirect instruction (`NEXT_REDIRECT`).
- **Technical Boundary**: This observation documents the observed wire response. It does not establish the internal server-side implementation details (e.g., whether the redirect originated within a component, a layout, or a server function, or whether edge middleware was active). Specific internal tool names, sub-route paths, and operational descriptions have been replaced with sanitized placeholders (`[Internal Tool Title A]`, `[REDACTED_INTERNAL_ROUTE_*]`) to preserve publication privacy.

---

## 2. Client-Side Document Reference Construction

* **Observation Reference**: Finding F-02 / Finding 1.2 in [`docs/03_SECURITY_FINDINGS.md`](../docs/03_SECURITY_FINDINGS.md)
* **Target Location**: `portal.cynbit.in/dashboard/offer-letter` (Extracted from client bundle)
* **Observed Server Actions**:
  - `logDocumentToDB` (`[ACTION_ID_DOC_LOG]`)
  - `fetchDocumentByRef` (`[ACTION_ID_DOC_FETCH]`)

### Verifiable Client-Side Code Excerpt
```javascript
// Extracted from offer-letter client-side chunk
let r = "Internship" === a.employmentType;
let o = e || "CB/HR/"
  .concat(r ? "INT" : "OFFER", "/")
  .concat(new Date().getFullYear(), "/")
  .concat(Math.floor(1e4 + 9e4 * Math.random()));

// Submitting client-constructed identifier directly to serverless action
if (!e) await (0, l.H)(r ? "Internship Offer" : "Offer Letter", o, a.candidateName, a);
```

### Technical Significance & Verification
- Identifiers are formatted as `CB/HR/{INT|OFFER}/{YEAR}/{5_DIGIT_RANDOM}` where the random component is computed via browser `Math.random()`.
- Suffix range is strictly $[10000, 99999]$, providing only $90,000$ possible values.
- **Birthday Paradox Implication**: In an enterprise issuing hundreds of documents, a $50\%$ probability of reference collision occurs after generating only $\approx 354$ documents ($p \approx 1 - e^{-\frac{k^2}{2N}}$).
- The client-side generation inverts authoritative entity control, passing an untrusted client string to database logging.

---

## 3. Server Action RPC Transport & Form Serialization

* **Observation Reference**: Section 4 in [`docs/02_ARCHITECTURE_AUDIT.md`](../docs/02_ARCHITECTURE_AUDIT.md)
* **Observed Behavior**: State-changing endpoints do not use conventional REST (`/api/*`). Instead, Next.js Server Actions execute via HTTP `POST` RPC.

### 3.1 Server Action Client Registration Pattern
Extracted from public Webpack bundles:
```javascript
// RPC Client Reference Stub Registration
const submitInquiryAction = (0, d.createServerReference)(
  "[ACTION_ID_INQUIRY]",
  d.callServer,
  void 0,
  d.findSourceMapURL,
  "submitInquiry"
);
```

### 3.2 Public Contact Form (`submitInquiry`)
* **Calling Route**: `https://www.cynbit.in/contact`
* **Action Identifier**: `[ACTION_ID_INQUIRY]` (`submitInquiry`)
* **Client Dispatch Excerpt**:
  ```javascript
  let d = async e => {
    e.preventDefault();
    setLoading(true);
    let formData = new FormData(e.currentTarget);
    let res = await submitInquiryAction(formData);
    setLoading(false);
    res.success
      ? setShowSuccess(true)
      : setError("Something went wrong. Please try again or email us directly.");
  };
  ```
* **Submitted Fields**: `fullName` (text, required), `email` (email, required), `company` (text, optional), `industry` (text, optional), `message` (textarea, required).
* **Observed Return Shape**: `{ success: boolean }`.

### 3.3 Candidate Application Form (`submitCandidateApplication`)
* **Calling Route**: `https://www.cynbit.in/careers/[slug]`
* **Action Identifier**: `[ACTION_ID_CAREER_APP]` (`submitCandidateApplication`)
* **Client Dispatch Excerpt**:
  ```javascript
  let N = async e => {
    e.preventDefault();
    let r = new FormData(e.currentTarget);
    r.append("jobId", targetedJobSlug);
    r.append("educationData", JSON.stringify(educationHistoryArray));
    let s = await submitCandidateAction(r);
    if (s.success) {
      setSubmitted(true);
    } else if (s.isDuplicate && s.trackingUrl) {
      setDuplicateTrackingUrl(s.trackingUrl);
    } else {
      setError(s.error || "Something went wrong. Please check all required fields and try again.");
    }
  };
  ```
* **Submitted Fields**: Personal information, social/portfolio links, address, and JSON-serialized `educationData` array.
* **Observed Return Shape**: `{ success: boolean, isDuplicate?: boolean, trackingUrl?: string, error?: string }`.

---

## 4. Portal Shared-Key Authentication Pattern

* **Observation Reference**: Finding F-05 / Observation 3.1 in [`docs/03_SECURITY_FINDINGS.md`](../docs/03_SECURITY_FINDINGS.md)
* **Target Route**: `https://portal.cynbit.in/`
* **Action Identifier**: `[ACTION_ID_PORTAL_LOGIN]` (`loginToPortal`)

### Client Form Dispatch Excerpt
```javascript
// Extracted from portal authentication chunk
async function handleLogin(e) {
  e.preventDefault();
  setLoading(true);
  let formData = new FormData();
  formData.append("password", enteredSecurityKey); // Single "Security Key" input
  let res = await loginToPortal(formData);
  if (res.success) {
    try {
      localStorage.setItem("cynbit_portal_last_activity", Date.now().toString());
    } catch (err) {}
    window.location.href = "/dashboard";
  } else {
    setError(res.error || "Login failed");
    setLoading(false);
  }
}
```

### Technical Significance
- The authentication form collects only a `password` field labeled "Security Key".
- No individual username, email, or employee ID is transmitted at the login gateway.
- While downstream session checks may enforce roles once authenticated, a shared secret entry point inherently prevents individual accountability and audit trails at the initial authentication boundary.

---

## 5. HTTP Security Headers & Performance Telemetry

### 5.1 Security Header Comparison
Measured across both subdomains via HTTP/1.1 over TLS 1.3:

| Header | `www.cynbit.in` | `portal.cynbit.in` | Observed Status / Technical Interpretation |
| :--- | :--- | :--- | :--- |
| `Strict-Transport-Security` | **Present** | **Present** | `max-age=63072000` (Enforces HTTPS transport) |
| `Content-Security-Policy` | **Absent** | **Absent** | Header absent. Absence of this defense-in-depth header does not alone prove all script/frame sources are unrestricted. |
| `X-Frame-Options` | **Absent** | **Absent** | Header absent. Absence of an explicit anti-framing header indicates missing defense-in-depth; it does not alone establish that framing is exploitable in all modern browser contexts. |
| `X-Content-Type-Options` | **Absent** | **Absent** | Header absent (`nosniff` not declared). |
| `Referrer-Policy` | **Absent** | **Absent** | Header absent (browser defaults apply). |
| `Permissions-Policy` | **Absent** | **Absent** | Header absent. Explicit feature restrictions (camera, mic, geolocation) not declared via header; browser defaults apply. |
| `Access-Control-Allow-Origin` | `*` (Wildcard) | `*` (Wildcard) | Wildcard returned on HTML documents. Represents an overly permissive edge header configuration; does not establish an authenticated API CORS vulnerability since credentials are not enabled. |

*Full raw header captures are preserved in [`raw_headers_www.txt`](raw_headers_www.txt) and [`raw_headers_portal.txt`](raw_headers_portal.txt).*

### 5.2 Network Latency Baseline (Single-Session Spot Probes)
> [!NOTE]
> **Measurement Notice**:
> The following figures represent a point-in-time, single-session baseline captured from an individual client connection targeting Vercel Mumbai (`bom1`). They are point-in-time spot observations, not sustained load tests, multi-sample statistical benchmarks, or isolated server execution measurements.

```
Target: www.cynbit.in (Vercel Anycast 64.29.17.65)
├─ TCP Connect Latency    :  59.03 ms
├─ TLS Handshake Latency  :  75.51 ms (134.54 ms cumulative)
├─ Time to First Byte     : 463.91 ms
└─ Cache Status           : HIT (Age: 233854s, X-Nextjs-Prerender: 1)

Target: portal.cynbit.in (Vercel Anycast 64.29.17.65)
├─ TCP Connect Latency    : 110.07 ms
├─ TLS Handshake Latency  :  98.69 ms (208.76 ms cumulative)
├─ Time to First Byte     : 301.28 ms
└─ Cache Status           : HIT (Age: 232047s, X-Nextjs-Prerender: 1)
```

---

## 6. Audit Data Retention & Disclosure Principles

To maintain strict ethical standards and ensure the repository is safe for public GitHub hosting:
1. **No Raw HTML Dumps**: Marketing text, direct employee phone numbers, and complete DOM trees have been removed and replaced by targeted technical excerpts above.
2. **No Bulk Client Bundles**: Compiled Next.js Webpack chunks have been removed and replaced with minimal, non-proprietary code snippets proving the specific technical mechanisms.
3. **No Unindexed Metadata or Specific Tool Names**: Private administrative route names and unindexed portal feature names from the 307 redirect body have been sanitized.
4. **Sanitized Action Identifiers**: Exact hexadecimal Server Action hashes have been replaced with descriptive architectural labels (`[ACTION_ID_*]`).
5. **Preserved Raw Headers**: Raw HTTP response headers are preserved in [`raw_headers_www.txt`](raw_headers_www.txt) and [`raw_headers_portal.txt`](raw_headers_portal.txt) as standard, non-sensitive audit evidence.
