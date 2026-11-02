# 03. Security, Authentication & Configuration Audit

## Taxonomy of Findings

To ensure scientific accuracy and prevent unsupported claims, observations are categorized into:
- **Category A: Confirmed Empirical Observations**: Directly verifiable through passive HTTP responses or public client bundles.
- **Category B: Potential Server-Side Risks**: Plausible risks inherent in the observed architecture, but requiring authorized server-side access to definitively confirm.
- **Category C: Configuration & Architectural Observations**: Deviations from standard industry hardening guidelines without direct exploitability.

---

## 1. Category A: Confirmed Empirical Observations

### Finding 1.1: React Server Component (RSC) UI Template Disclosure on HTTP 307 Redirects
- **Classification**: Information Disclosure (Low / Informational)
- **Target URL**: `https://portal.cynbit.in/dashboard`, `https://portal.cynbit.in/dashboard/interns`
- **Confidence**: 100% (Empirically verified)

#### Empirical Request & Response Trace
```http
GET /dashboard HTTP/1.1
Host: portal.cynbit.in
User-Agent: curl/8.21.0
Accept: */*

HTTP/1.1 307 Temporary Redirect
Date: Sun, 01 Nov 2026 05:54:10 GMT
Server: Vercel
Location: /
X-Matched-Path: /portal/dashboard
X-Powered-By: Next.js
Transfer-Encoding: chunked

<!DOCTYPE html>...
<script>self.__next_f.push([1,"...
[Internal Tool Title A]... [Sanitized operational description A]
[Internal Tool Title B]... [Sanitized operational description B]
[Internal Document Log Section]... [Sanitized log description]
href: \"[REDACTED_INTERNAL_ROUTE_A]\"
href: \"[REDACTED_INTERNAL_ROUTE_B]\"
..."])</script>
7:E{"digest":"NEXT_REDIRECT;replace;/;307;"}
```

#### Technical Explanation & Boundary
- **What is Occurring**: The captured HTTP response to an unauthenticated request to `/dashboard` returned an HTTP 307 redirect status code alongside a chunk-encoded body containing a serialized React Server Component (RSC) flight payload with UI layout strings, followed by a client-side redirect instruction (`NEXT_REDIRECT`).
- **Scope Boundary**: The exposed content consists **strictly of UI template strings and layout structures**. No user personal identifiable information (PII), database connection strings, secret keys, or live records were observed in this payload.
- **Definitive Assessment Limit**: This observation documents the observed wire response. It does not infer or prove internal server-side implementation details, such as the specific code location of the redirect call or edge middleware configuration.
- **Impact**: Enables unauthenticated observation of internal tool titles and layout structures.

---

### Finding 1.2: Client-Side Document Reference Construction with Standard Pseudo-Randomness
- **Classification**: Architectural & Cryptographic Design Defect (Medium)
- **Target Location**: `https://portal.cynbit.in/dashboard/offer-letter` (Documented in [`evidence/EVIDENCE_LOG.md § 2`](../evidence/EVIDENCE_LOG.md#2-client-side-document-reference-construction))
- **Confidence**: 100% (Empirically verified in public client bundle)

#### Empirical Code Extraction
```javascript
let r = "Internship" === a.employmentType;
let o = e || "CB/HR/"
  .concat(r ? "INT" : "OFFER", "/")
  .concat(new Date().getFullYear(), "/")
  .concat(Math.floor(1e4 + 9e4 * Math.random()));

if (!e) await (0, l.H)(r ? "Internship Offer" : "Offer Letter", o, a.candidateName, a);
```

#### Technical Explanation & Mathematical Collision Vulnerability
1. **Inversion of Control**: The primary business identifier (`refNumber`) is synthesized entirely in the client's browser JavaScript and passed upstream to the `logDocumentToDB` Server Action (`78b2e843c9bec963...`). The backend client does not authoritatively dictate the primary business key.
2. **Predictability & Finite Keyspace**: The suffix is constrained to a 5-digit integer between $10,000$ and $99,999$, providing a keyspace of exactly $90,000$ possibilities.
3. **Collision Vulnerability via Birthday Paradox**:
   Using the standard collision approximation formula:
   $$p \approx 1 - e^{-\frac{k^2}{2N}}$$
   where $N = 90,000$:
   - For $k = 100$ issued documents, the collision probability is already $\approx 5.4\%$.
   - For $k = 354$ documents, the collision probability reaches **$50.0\%$**.
   In an active HR environment generating offer letters, joining kits, and training certificates, identifier collisions will inevitably overwrite records or cause fatal database key violations.
4. **Non-Cryptographic PRNG**: `Math.random()` is deterministic and not cryptographically secure, allowing potential internal or external observers to predict sequential identifiers if internal state is deduced.

#### Recommended Enterprise Remediation Workflow
An enterprise document lifecycle requires:
1. **Server-Authoritative Generation**: The server generates the reference number upon receipt of an authorized document creation request.
2. **CSPRNG Entropy**: Suffixes generated via a Cryptographically Secure Pseudo-Random Number Generator (CSPRNG, such as Java's `SecureRandom`) across an unambiguous character set (such as Crockford Base32), providing $> 40\text{ bits}$ of entropy ($> 10^{12}$ combinations).
3. **Collision Handling**: An active retry loop with collision detection before record insertion.
4. **Database Uniqueness Enforcement**: A strict database unique constraint (`UNIQUE KEY uq_reference_number (reference_number)`) to ensure atomic uniqueness at the storage engine level.
5. **Transactional Persistence**: Atomic `@Transactional` database persistence ensuring document generation, reference allocation, and audit logs are committed together.
6. **Object-Level Authorization**: Enforcing role checks (`ROLE_HR_ADMIN`, `ROLE_SUPER_ADMIN`) or candidate ownership checks when resolving documents by reference number, preventing Insecure Direct Object References (IDOR).

#### Implementation Status Division
- **Implemented in Reference Service (`backend-remediation-service`)**:
  - Server-authoritative generation in [`DocumentReferenceService.java`](file:///e:/Majduri/WebDev/backend-remediation-service/src/main/java/com/cynbit/remediation/service/DocumentReferenceService.java) using `SecureRandom` and Crockford Base32 ($32^8 \approx 1.099 \times 10^{12}$ combinations).
  - Programmatic collision check and retry loop (up to 5 retries against in-memory store).
  - SHA-256 payload checksum computation for tamper detection.
  - Server-side role authorization requiring `ROLE_HR_ADMIN` or `ROLE_SUPER_ADMIN` on `POST /api/v1/portal/documents`.
- **Proposed for Production Integration (Not Yet Integrated in Live Prototype)**:
  - Storage-engine uniqueness constraint (defined in [`schema.sql`](file:///e:/Majduri/WebDev/backend-remediation-service/src/main/resources/schema.sql) but awaiting RDBMS integration).
  - Database transactional persistence (`@Transactional` over JPA/Hibernate).
  - Candidate-specific object-level authorization (ABAC) linking document access to verified employee/applicant identity.

---

### Finding 1.3: Separate Verification of HTTP Security Headers

The HTTP response headers were verified independently for `www.cynbit.in` and `portal.cynbit.in`:

| Security Header | Status on `www.cynbit.in` | Status on `portal.cynbit.in` | Standard Best-Practice Requirement | Technical Interpretation & Boundary |
| :--- | :--- | :--- | :--- | :--- |
| `Strict-Transport-Security` | **Present** (`max-age=63072000`) | **Present** (`max-age=63072000`) | Enforces HTTPS strictly for 2 years | Actively enforced across both subdomains. |
| `Content-Security-Policy` | **Absent** | **Absent** | Restricts sources of scripts, styles, objects | Defense-in-depth header absent. Does not prove all script or frame sources are unrestricted in practice. |
| `X-Frame-Options` | **Absent** | **Absent** | Defends against framing / Clickjacking | Explicit anti-framing header absent. Indicates missing defense-in-depth, but does not alone prove framing is exploitable in all modern browser contexts. |
| `X-Content-Type-Options` | **Absent** | **Absent** | Prevents MIME-type sniffing (`nosniff`) | Defense-in-depth header absent. |
| `Referrer-Policy` | **Absent** | **Absent** | Controls referrer header leakage | Default browser referrer behavior applies. |
| `Permissions-Policy` | **Absent** | **Absent** | Restricts browser features (geolocation, camera, mic) | Explicit feature restrictions not declared via header; browser defaults apply. |

- **Impact**: **Low (Defense-in-Depth Configuration Finding)**. The absence of these headers represents an opportunity to harden edge defense-in-depth. In accordance with strict audit integrity, missing headers indicate the lack of an explicit HTTP-level security policy declaration; they do not alone demonstrate active exploitability.

---

## 2. Category B: Potential Server-Side Risks (Requiring Authorized Verification)

The following areas cannot be definitively proven without server-side codebase access or authorized credential testing:

### Risk 2.1: Insecure Direct Object Reference (IDOR) on `fetchDocumentByRef`
- **Observation**: The client references Server Action `fetchDocumentByRef` (`40244e9a6ff014cf...`) which queries documents by their reference string.
- **Potential Risk**: Because reference numbers follow a predictable pattern (`CB/HR/OFFER/2026/[10000-99999]`), if the serverless handler does not validate active administrative session context prior to querying, unauthorized callers could theoretically enumerate historical offer letters.
- **Verification Requirement**: Requires authorized inspection of server-side data access code to determine whether ownership or session validation is enforced.

### Risk 2.2: Rate Limiting & Resource Exhaustion on Public Server Actions
- **Observation**: Public forms on `/contact` (`submitInquiry`) and `/careers/[slug]` (`submitCandidateApplication`) execute without client-side CAPTCHA or visible proof-of-work.
- **Potential Risk**: If Vercel Edge Middleware or downstream backend handlers lack IP-based token-bucket rate limiting, malicious bots could automate large volumes of form submissions, exhausting database storage or third-party email quotas.
- **Verification Requirement**: Requires authorized review of Vercel WAF rules and backend ingestion pipelines.

### Risk 2.3: Downstream Sanitization of Freeform Candidate Inputs
- **Observation**: Applications accept user-supplied URLs and address text (`portfolioUrl`, `linkedinUrl`, `professionalProfileUrl`, `permanentAddress`).
- **Potential Risk**: While React escapes output in JSX, if these inputs are rendered into server-generated PDFs, internal emails, or exported to CSV spreadsheets without sanitization, injection vectors (Formula Injection, HTML injection in email templates) could be realized.
- **Verification Requirement**: Requires authorized review of server-side validation schemas (e.g., Zod) and downstream PDF/email templating engines.

---

## 3. Category C: Configuration & Architectural Observations

### Observation 3.1: Observable Shared-Key Portal Login Design
- **Observation**: In `portal_chunk.js`, the authentication form accepts solely a `"Security Key"` and sends FormData with a single `password` field:
  ```javascript
  let t = new FormData();
  t.append("password", e);
  let a = await l(t); // loginToPortal
  ```
- **Context & Boundary**:
  - This is an **observable login design pattern**. It demonstrates that the initial authentication barrier relies on a single shared secret rather than individual user credentials.
  - **Important Caveat**: This observation does *not* prove that server-side RBAC or user session roles do not exist downstream once a session is established.
  - **Operational Impact**: A shared credential model precludes individual employee non-repudiation and fine-grained audit logging at the gateway level.

### Observation 3.2: Wildcard CORS on HTML Document Responses
- **Observation**:
  `Access-Control-Allow-Origin: *` is returned on root HTML responses on `https://www.cynbit.in/` and `https://portal.cynbit.in/`.
- **Context & Boundary**:
  - This is classified as a **configuration observation**, *not* a confirmed exploitable vulnerability.
  - **No API Vulnerability Established**: Returning wildcard `Access-Control-Allow-Origin: *` on top-level HTML document responses alone does not establish an API CORS vulnerability. Because `Access-Control-Allow-Credentials: true` is absent, cross-origin web applications cannot read authenticated user sessions via browser fetch requests.
  - Rather, this reflects an overly permissive edge CDN header policy that departs from the principle of least privilege.

