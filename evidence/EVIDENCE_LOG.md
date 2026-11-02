# Evidence Log: Sanitized Technical Proofs

**Assessment**: Cynbit Technologies — Backend Developer Intern Technical Assessment  
**Target Hosts**: `https://www.cynbit.in/` & `https://portal.cynbit.in/`  
**Scope**: Passive, authorized, read-only reconnaissance  
**Status**: Sanitized publication log (redacted proprietary markup, direct telephone numbers, and unindexed internal route paths)

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
    [\"$\",\"h3\",null,{\"children\":\"Offer Letter Generator\"}],
    [\"$\",\"p\",null,{\"children\":\"Generate official offer letters for new hires with custom terms and salary details.\"}],
    [\"$\",\"a\",null,{\"href\":\"[REDACTED_INTERNAL_ROUTE_1]\",\"children\":\"Get Started\"}],

    [\"$\",\"h3\",null,{\"children\":\"Bitelevate Offer Letter\"}],
    [\"$\",\"p\",null,{\"children\":\"Generate selection letters for candidates joining the Bitelevate Training Program.\"}],
    [\"$\",\"a\",null,{\"href\":\"[REDACTED_INTERNAL_ROUTE_2]\",\"children\":\"Get Started\"}],

    [\"$\",\"h3\",null,{\"children\":\"Joining Kit Generator\"}],
    [\"$\",\"p\",null,{\"children\":\"Create comprehensive onboarding documents including NDA and Terms of Employment.\"}],

    [\"$\",\"h2\",null,{\"children\":\"Central Document Log\"}],
    [\"$\",\"p\",null,{\"children\":\"A secure, permanent record of all generated HR documents.\"}],
    [\"$\",\"p\",null,{\"children\":\"No documents have been logged to the database yet.\"}],
  ..."])</script>
  <script>self.__next_f.push([1,"7:E{\"digest\":\"NEXT_REDIRECT;replace;/;307;\"}\n"])</script>
</body>
</html>
```

### Technical Significance & Verification
- Next.js App Router Server Components evaluate the component render function prior to emitting the redirect digest if `redirect('/')` is executed inside component logic rather than at the edge gatekeeper (`middleware.ts`).
- **Publication Sanitation**: Specific internal sub-route URLs and unindexed administrative endpoints have been redacted (`[REDACTED_INTERNAL_ROUTE_*]`) to avoid indexing Cynbit's internal directory on public repositories while fully preserving the technical proof of streaming RSC payload leakage in HTTP 307 redirects.

---

## 2. Client-Side Document Reference Construction

* **Observation Reference**: Finding F-02 / Finding 1.2 in [`docs/03_SECURITY_FINDINGS.md`](../docs/03_SECURITY_FINDINGS.md)
* **Target Location**: `portal.cynbit.in/dashboard/offer-letter` (Extracted from client bundle)
* **Observed Server Actions**:
  - `logDocumentToDB` (`78b2e843c9bec963d7ae83d753ba5797d36acf8f34`)
  - `fetchDocumentByRef` (`40244e9a6ff014cf50fb7ae4db6efa3442036103dd`)

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
  "40247ba4465344b3386595bab4303317cdcce907df",
  d.callServer,
  void 0,
  d.findSourceMapURL,
  "submitInquiry"
);
```

### 3.2 Public Contact Form (`submitInquiry`)
* **Calling Route**: `https://www.cynbit.in/contact`
* **Action Identifier Hash**: `40247ba4465344b3386595bab4303317cdcce907df`
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
* **Action Identifier Hash**: `40645a00e04f68541170eb8fc5f6fb052cfce904b7`
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
* **Action Identifier Hash**: `40d93da1cc26de4ef37bcb273184b7141ef77a50f8` (`loginToPortal`)

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

| Header | `www.cynbit.in` | `portal.cynbit.in` | Observed Value / Implication |
| :--- | :--- | :--- | :--- |
| `Strict-Transport-Security` | **Present** | **Present** | `max-age=63072000` (2-year HSTS enforcement) |
| `Content-Security-Policy` | **Absent** | **Absent** | No restriction on script/frame sources |
| `X-Frame-Options` | **Absent** | **Absent** | Portal allows framing; clickjacking risk |
| `X-Content-Type-Options` | **Absent** | **Absent** | No MIME-sniffing protection |
| `Referrer-Policy` | **Absent** | **Absent** | Default browser referrer behavior |
| `Permissions-Policy` | **Absent** | **Absent** | Browser APIs unrestricted |
| `Access-Control-Allow-Origin` | `*` (Wildcard) | `*` (Wildcard) | Overly permissive header on HTML responses |

*Full raw header captures are preserved in [`raw_headers_www.txt`](raw_headers_www.txt) and [`raw_headers_portal.txt`](raw_headers_portal.txt).*

### 5.2 Network Latency Probes
Measured from an Indian client connection targeting Vercel Edge PoP `bom1` (Mumbai, India):

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
1. **No Raw HTML Dumps**: Marketing text, direct employee phone numbers, and complete DOM trees have been replaced by targeted technical excerpts above.
2. **No Bulk Client Bundles**: Compiled Next.js Webpack chunks have been reduced to minimal, non-proprietary code snippets proving the specific technical mechanisms.
3. **No Unindexed Metadata**: Private administrative route names and unindexed portal details from `portal_dashboard.html` have been redacted.
4. **Preserved Raw Headers**: Raw HTTP response headers are preserved in [`raw_headers_www.txt`](raw_headers_www.txt) and [`raw_headers_portal.txt`](raw_headers_portal.txt) as standard, non-sensitive audit evidence.
