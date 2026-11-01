# Backend Technical Audit & Enterprise Remediation Architecture
### Cynbit Technologies — Backend Developer Intern Technical Assessment

![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.3-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)
![Vercel Edge](https://img.shields.io/badge/Observed_Edge-Vercel%20Anycast%20(bom1)-000000?style=for-the-badge&logo=vercel&logoColor=white)
![Assessment Type](https://img.shields.io/badge/Audit_Scope-Passive%20%26%20Non--Intrusive-blue?style=for-the-badge)

---

## 1. Executive Summary & Assessment Scope

This repository constitutes the complete technical deliverable for the **Backend Developer Intern Technical Assessment** for **Cynbit Technologies Private Limited** (Jaipur, Rajasthan, India).

The objective was to conduct an objective, non-intrusive backend technical audit of the company's publicly observable web infrastructure ([https://www.cynbit.in/](https://www.cynbit.in/) and [https://portal.cynbit.in/](https://portal.cynbit.in/)), document empirical evidence for all findings, distinguish verified facts from potential risks, and engineer a production-ready **Java 17 + Spring Boot 3** reference backend demonstrating enterprise-grade remediations.

### Strict Assessment Guardrails
- **Passive & Read-Only**: Zero active exploitation, fuzzing, brute forcing, or state-altering requests.
- **Strict Separation**: The reference remediation service is an independent, isolated engineering blueprint. It does not interact with Cynbit's live production database.
- **Evidence-Based Reporting**: No assumptions about backend database schemas or server-side authorization were made without verifiable HTTP or client-bundle proof.

---

## 2. Discovered Architecture Summary

| Architecture Layer | Observable Production Stack | Verified Details |
| :--- | :--- | :--- |
| **Edge CDN & Hosting** | Vercel Global Edge Network | Anycast IPs `64.29.17.65`, `216.198.79.65`; PoP: `bom1` (Mumbai, India) |
| **Application Framework** | Next.js App Router (RSC) | React Server Components; Build ID: `x-Rd73BtzTiCyyJyTHd5H` |
| **API Paradigm** | Server Actions RPC (HTTP POST) | No active REST routes under `/api/*` (returns 404). Operations dispatch via Server Actions |
| **Public Host** | `www.cynbit.in` | Marketing, services, editorial content, and multi-field job applications |
| **Administrative Host** | `portal.cynbit.in` | Internal portal for HR documents, intern onboarding, and daily deliverable reviews |

---

## 3. Findings Matrix & Empirical Evidence

Findings are strictly classified into **Confirmed Observations**, **Potential Risks**, and **Configuration Observations**:

| ID | Category | Finding Description | Target Location | Actual Measured Impact |
| :--- | :--- | :--- | :--- | :--- |
| **F-01** | **Confirmed** | **RSC UI Template Disclosure in HTTP 307 Redirect Body** | `portal.cynbit.in/dashboard` | **Low/Informational**: Streamed RSC body leaks internal tool titles, descriptions, and route URLs to unauthenticated HTTP inspectors. |
| **F-02** | **Confirmed** | **Client-Side Document Reference Construction with Non-CSPRNG** | `portal.cynbit.in/dashboard/offer-letter` | **Medium**: Document IDs (`CB/HR/OFFER/2026/[10000-99999]`) are generated in the browser via `Math.random()` and submitted to server logging. |
| **F-03** | **Confirmed** | **Missing HTTP Defense-in-Depth Headers** | `www.cynbit.in` & `portal.cynbit.in` | **Low/Medium**: CSP, X-Frame-Options, and X-Content-Type-Options: nosniff are absent across both subdomains. |
| **F-04** | **Configuration** | **Wildcard CORS Returned on HTML Documents** | `www.cynbit.in` & `portal.cynbit.in` | **Low**: `Access-Control-Allow-Origin: *` returned on HTML document requests. Non-exploitable without credential flags, but overly permissive. |
| **F-05** | **Configuration** | **Observable Shared-Key Login Pattern** | `portal.cynbit.in/` | **Operational Risk**: Portal form sends only a `password` field ("Security Key"), lacking individual employee identities at the gateway. |
| **F-06** | **Performance** | **Uncached 307 Apex Domain Redirection** | `https://cynbit.in/` $\rightarrow$ `https://www.cynbit.in/` | **Latency Overhead**: Uses `307 Temporary Redirect` (`max-age=0`) instead of `308 Permanent`, incurring an uncacheable round-trip hop. |
| **R-01** | **Potential Risk** | **Serverless Rate Limiting on Public Ingestion Handlers** | `/contact` & `/careers/[slug]` | **Requires Verification**: Public forms lack visible CAPTCHA or client rate limits; requires server WAF review to verify abuse protection. |
| **R-02** | **Potential Risk** | **Authorization on Document Lookup Handlers** | `fetchDocumentByRef` | **Requires Verification**: Predictable reference pattern requires server-side verification to confirm whether active session context is enforced. |

---

## 4. Security Header Verification (`www.cynbit.in` vs `portal.cynbit.in`)

| HTTP Header | `www.cynbit.in` | `portal.cynbit.in` | Recommended Enterprise Standard |
| :--- | :--- | :--- | :--- |
| `Strict-Transport-Security` | **Present** (`max-age=63072000`) | **Present** (`max-age=63072000`) | `max-age=63072000; includeSubDomains; preload` |
| `Content-Security-Policy` | **Absent** | **Absent** | `default-src 'self'; frame-ancestors 'none';` |
| `X-Frame-Options` | **Absent** | **Absent** | `DENY` or `SAMEORIGIN` (prevents clickjacking) |
| `X-Content-Type-Options` | **Absent** | **Absent** | `nosniff` (prevents MIME sniffing) |
| `Referrer-Policy` | **Absent** | **Absent** | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | **Absent** | **Absent** | `camera=(), microphone=(), geolocation=()` |
| `Access-Control-Allow-Origin` | `*` (Wildcard) | `*` (Wildcard) | Whitelisted explicit origins only |

---

## 5. Measured Performance & Latency Telemetry

Measured using direct timing probes from an Indian client connection targeting Vercel Mumbai (`bom1`):

```
www.cynbit.in (100.4 KB HTML - Edge Cache HIT: Age 233854s)
├─ DNS & Socket TCP Handshake :  59.03 ms
├─ TLS 1.3 Key Exchange        :  75.51 ms (134.54 ms cumulative)
└─ Time to First Byte (TTFB)   : 463.91 ms (Total Transfer: 464.18 ms)

portal.cynbit.in (12.9 KB HTML - Edge Cache HIT: Age 232047s)
├─ DNS & Socket TCP Handshake : 110.07 ms
├─ TLS 1.3 Key Exchange        :  98.69 ms (208.76 ms cumulative)
└─ Time to First Byte (TTFB)   : 301.28 ms (Total Transfer: 301.47 ms)
```

---

## 6. Enterprise Remediation Architecture (Java 17 + Spring Boot 3)

The reference service in `backend-remediation-service/` demonstrates an enterprise replacement architecture:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        HTTP / Reverse Proxy                            │
│    (Strict HSTS, Dynamic CSP, X-Frame-Options: DENY, nosniff, 308)     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                  Spring Security & Defense Layer                       │
│  - Token Bucket Rate Limiter (TokenBucket - IP & Endpoint Category)    │
│  - Stateless JWT Authentication with HttpOnly, Secure, SameSite Cookies│
│  - Granular RBAC: ROLE_SUPER_ADMIN, ROLE_HR_ADMIN, ROLE_MENTOR         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                    API Gateway / Controller Layer                      │
│  - @Valid + Hibernate Validator DTOs (Strict Whitelisting & RegEx)     │
│  - RFC 7807 ProblemDetail Global Error Handler (@RestControllerAdvice) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                       Core Service Domain                              │
│  - DocumentReferenceService: SecureRandom Crockford Base32 CSPRNG      │
│  - SHA-256 Document Checksum Verification                              │
│  - In-Memory Concurrent Store with Duplicate Application Guard         │
└────────────────────────────────────────────────────────────────────────┘
```

### Key Technical Implementations
1. **Pre-Render Edge Access Guard**: Illustrated in [docs/05_REMEDIATION_ROADMAP.md](docs/05_REMEDIATION_ROADMAP.md) to halt unauthenticated requests in edge middleware before Next.js enters Server Component rendering.
2. **Cryptographic Reference Number Service**: Implemented in [`DocumentReferenceService.java`](backend-remediation-service/src/main/java/com/cynbit/remediation/service/DocumentReferenceService.java) utilizing Java's `SecureRandom` across Crockford Base32 charset (unambiguous, collision-resistant).
3. **HttpOnly Cookie-Based JWT Authentication & Safe JSON**: Implemented in [`AuthController.java`](backend-remediation-service/src/main/java/com/cynbit/remediation/controller/AuthController.java), [`JwtTokenProvider.java`](backend-remediation-service/src/main/java/com/cynbit/remediation/security/JwtTokenProvider.java), and [`JwtAuthenticationFilter.java`](backend-remediation-service/src/main/java/com/cynbit/remediation/security/JwtAuthenticationFilter.java). Issues `HttpOnly`, `Secure`, `SameSite=Strict` cookies aligned with token TTL. The raw JWT is never exposed in JSON responses. Dual-mode filter supports both browser cookies and `Authorization: Bearer` API tokens.
4. **Modern SPA CSRF Protection**: Implemented in [`SecurityConfig.java`](backend-remediation-service/src/main/java/com/cynbit/remediation/config/SecurityConfig.java) using `CookieCsrfTokenRepository.withHttpOnlyFalse()` and `X-XSRF-TOKEN` header validation for state-changing administrative operations, with public submission routes exempted.
5. **Token Bucket Rate Limiting with Trusted Proxy Resolution**: Implemented in [`RateLimitConfig.java`](backend-remediation-service/src/main/java/com/cynbit/remediation/config/RateLimitConfig.java) with synchronized token buckets, positive capacity validation, strict `X-Forwarded-For` verification only from explicitly configured trusted proxies, HTTP 429 `Retry-After` response calculation, and memory cleanup (`cleanupStaleBuckets`).
6. **RFC 7807 Global Exception Handling**: Implemented in [`GlobalExceptionHandler.java`](backend-remediation-service/src/main/java/com/cynbit/remediation/exception/GlobalExceptionHandler.java) providing structured `application/problem+json` error responses for validation, 401, 403, 404, 409, and 429 errors.
7. **Production MySQL Persistence Schema**: Defined in [`schema.sql`](backend-remediation-service/src/main/resources/schema.sql) with InnoDB engine, database-level unique constraints, and foreign key relationships, ready for live migration via `--spring.profiles.active=mysql`.

---

## 7. Setup & Build Instructions

### Prerequisites
- **JDK**: Java 17 or Java 21 LTS installed (`java -version`)
- **Maven**: Apache Maven 3.8+ installed (`mvn -version`)

### Compilation & Running Tests
From the workspace directory:

```powershell
# Navigate to the reference remediation service
cd backend-remediation-service

# Run all 26 unit, security, and concurrency tests
mvn clean test

# Package the standalone executable JAR
mvn package -DskipTests
```

### Running the Application

Because the service enforces strict secrets externalization without hardcoded fallbacks, `JWT_SECRET` must be supplied via an environment variable with at least 32 bytes (256 bits) of entropy:

**Windows PowerShell**:
```powershell
$env:JWT_SECRET = "cynbit-enterprise-secret-key-with-sufficient-entropy-for-hmac-sha256-production-2026"
java -jar target/cynbit-backend-remediation-1.0.0.jar
```

**Linux / macOS (Bash)**:
```bash
export JWT_SECRET="cynbit-enterprise-secret-key-with-sufficient-entropy-for-hmac-sha256-production-2026"
java -jar target/cynbit-backend-remediation-1.0.0.jar
```

### Frontend CSRF Integration Guide
1. Upon initial load, make a `GET` request to any endpoint (e.g. `GET /api/v1/portal/auth/health`).
2. Spring Security's `CookieCsrfTokenRepository` automatically emits an `XSRF-TOKEN` cookie (`HttpOnly=false`, `Path=/`).
3. For state-changing requests (`POST /api/v1/portal/documents/**`), read the cookie value in JavaScript:
   ```javascript
   const xsrfToken = document.cookie
     .split('; ')
     .find(row => row.startsWith('XSRF-TOKEN='))
     ?.split('=')[1];

   await fetch('/api/v1/portal/documents', {
     method: 'POST',
     credentials: 'include', // sends cynbit_session HttpOnly cookie
     headers: {
       'Content-Type': 'application/json',
       'X-XSRF-TOKEN': xsrfToken
     },
     body: JSON.stringify(documentPayload)
   });
   ```

### Architectural Disclaimers & Production Migration
- **Persistence Status**: The application currently runs using thread-safe in-memory collections (`ConcurrentHashMap`) for standalone evaluation. It does NOT maintain a live connection to a running MySQL instance by default. A complete production-ready DDL script is provided in [`schema.sql`](backend-remediation-service/src/main/resources/schema.sql) and can be activated via `--spring.profiles.active=mysql` once a MySQL 8.0+ server is provisioned.
- **Token Blacklist Scope**: Token revocation operates via an in-memory `TokenBlacklistService`. Blacklisted tokens are evicted naturally at expiration or cleared upon application restart. In a distributed multi-instance deployment, replace this with a shared Redis store with TTL eviction.
- **Rate Limiter Scope**: In-memory token buckets track client IPs per JVM instance. For clustered deployments behind load balancers, replace in-memory buckets with Redis (`Bucket4j-redis` or `Redisson`).

### Default Test Credentials (Configured in AuthService)
- **Super Admin**: `super.admin@cynbit.in` / `Admin@Cynbit#2026` (`ROLE_SUPER_ADMIN`)
- **HR Admin**: `hr.admin@cynbit.in` / `HrAdmin#Cynbit2026` (`ROLE_HR_ADMIN`)
- **Mentor**: `mentor@cynbit.in` / `Mentor#Cynbit2026` (`ROLE_MENTOR`)

---

## 8. Repository Documentation Index

1. [01. Executive Summary](docs/01_EXECUTIVE_SUMMARY.md) — Assessment overview and executive takeaways.
2. [02. Architecture Audit](docs/02_ARCHITECTURE_AUDIT.md) — Detailed mapping of edge CDN, routing, and Server Actions.
3. [03. Security Findings](docs/03_SECURITY_FINDINGS.md) — Categorized empirical findings, technical explanations, and boundaries.
4. [04. Performance Analysis](docs/04_PERFORMANCE_ANALYSIS.md) — Measured network latency, TLS timing, and caching telemetry.
5. [05. Remediation Roadmap](docs/05_REMEDIATION_ROADMAP.md) — Implementation blueprints for edge guards, CSP, and RBAC.
6. [06. Target API Specification](docs/06_API_SPECIFICATION.md) — Standard RESTful OpenAPI contract for all operations.
7. [Raw Evidence Directory](evidence/) — Raw HTTP headers, captured payloads, and inspection scripts.

---

## 9. Ethical Audit & Legal Disclaimer

This technical audit was conducted strictly as an authorized, passive, read-only technical assessment. No attempts were made to breach authentication, manipulate live database records, fuzz endpoints, or disrupt production services. All observations reflect publicly accessible HTTP headers, client JavaScript chunks, and response bodies served directly to standard web clients.

