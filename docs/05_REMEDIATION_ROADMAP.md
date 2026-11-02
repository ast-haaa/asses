# 05. Enterprise Backend Remediation Roadmap

This document outlines practical, evidence-grounded engineering remediations addressing the specific observations identified during the Cynbit Technologies backend technical audit.

---

## 1. Architectural Relationship & Assessment Boundaries

> [!IMPORTANT]
> **Independent Reference Implementation Notice**:
> The Java 17 + Spring Boot 3 service in [`backend-remediation-service/`](../backend-remediation-service/) is an **independent, isolated reference architecture**. It does **not** alter, replace, or deploy to Cynbit's live production Next.js application or Vercel infrastructure.
> 
> **Persistence Implementation Status**:
> To enable self-contained evaluation and automated testing without external dependencies, business entities in the reference service are managed via **thread-safe in-memory collections (`ConcurrentHashMap`)**. Complete production MySQL 8.0 DDL ([`schema.sql`](../backend-remediation-service/src/main/resources/schema.sql)) and a `mysql` profile are provided, but live JPA/Hibernate database persistence is **not** currently integrated.

---

## 2. Evidence-Based Remediation Matrix

Every recommendation directly corresponds to an empirical observation gathered from Cynbit's public infrastructure:

| ID | Finding / Observable Area | Empirical Cynbit Evidence | Technical Reasoning & Risk | Expected Enterprise Benefit | Recommended Implementation Approach |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **REC-01** | **RSC UI Disclosure in 307 Redirects** | Streamed component template in 307 body on `portal.cynbit.in/dashboard` ([`evidence/EVIDENCE_LOG.md § 1`](../evidence/EVIDENCE_LOG.md#1-react-server-component-rsc-template-disclosure)). | Next.js executes redirect within component rendering tree, serializing internal tool descriptions into the HTTP response. | Completely prevents unauthenticated reconnaissance of internal admin tooling. | Enforce session check in Vercel Edge `middleware.ts` before component invocation. |
| **REC-02** | **Observable Shared-Key Login** | Form posts single `password` ("Security Key") to `loginToPortal` ([`evidence/EVIDENCE_LOG.md § 4`](../evidence/EVIDENCE_LOG.md#4-portal-shared-key-authentication-pattern)). | Shared secret prevents non-repudiation, individual employee auditing, and role segregation. | Individual accountability, single-user credential revocation, and fine-grained authorization. | Individual user accounts, BCrypt (factor 12), and role-based access control (`SUPER_ADMIN`, `HR_ADMIN`, `MENTOR`). |
| **REC-03** | **Client-Side Reference Construction** | Suffix generated via `Math.random()` in browser memory ([`evidence/EVIDENCE_LOG.md § 2`](../evidence/EVIDENCE_LOG.md#2-client-side-document-reference-construction)). | Keyspace of only 90,000 suffixes yields a 50% collision probability after ~354 documents (Birthday Paradox). Inverts entity authority. | Collision-resistant identifiers (> 10¹² combinations), cryptographic entropy, tamper-evident checksums. | Server-authoritative generation using `SecureRandom` + Crockford Base32, collision checks, and DB unique constraint. |
| **REC-04** | **Missing Defense-in-Depth Headers** | Absence of CSP, X-Frame-Options, nosniff on `www.cynbit.in` and `portal.cynbit.in` ([`evidence/raw_headers_www.txt`](../evidence/raw_headers_www.txt)). | Missing headers leave the internal portal open to clickjacking framing and MIME confusion attacks. | Hardens browser client against UI redressing, clickjacking, and MIME sniffing. | Configure `Content-Security-Policy`, `X-Frame-Options: DENY`, and `X-Content-Type-Options: nosniff`. |
| **REC-05** | **Wildcard CORS on HTML** | `Access-Control-Allow-Origin: *` returned on top-level HTML documents. | Overly permissive configuration that diverges from the principle of least privilege. | Eliminates unnecessary cross-origin exposure; restricts API communication to trusted domains. | Remove CORS from HTML routes; restrict API endpoints to explicit trusted origins (`cynbit.in`, `portal.cynbit.in`). |
| **REC-06** | **Public Form Flooding & Resource Exhaustion** | Inquiries (`/contact`) and applications (`/careers/[slug]`) submit without CAPTCHA or rate limits. | Automated bot scripts can exhaust database storage and third-party email notification quotas. | Protects backend ingestion pipelines while ensuring seamless candidate applications. | IP-based Token Bucket rate limiter with positive capacity checks, trusted proxy resolution, and HTTP 429 `Retry-After`. |
| **REC-07** | **Client-Side PDF Compilation** | Client chunk renders PDFs in browser memory via `jspdf` using local state ([`evidence/EVIDENCE_LOG.md § 2`](../evidence/EVIDENCE_LOG.md#2-client-side-document-reference-construction)). | Client-side compilation allows tampering with compensation or contract terms prior to export. | Authoritative legal contract generation matching server database records byte-for-byte. | Move PDF compilation to backend service with immutable SHA-256 payload checksums. |
| **REC-08** | **Apex Domain 307 Redirection Overhead** | `https://cynbit.in/` redirects to `https://www.cynbit.in/` via HTTP 307 with `max-age=0` ([`evidence/EVIDENCE_LOG.md § 5.2`](../evidence/EVIDENCE_LOG.md#52-network-latency-baseline-single-session-spot-probes)). | Non-cacheable temporary redirect forces repeat visitors to execute an extra network round-trip. | Eliminates 100–200 ms latency overhead on repeat visits and consolidates SEO indexing. | Configure Vercel domain redirect to use HTTP `308 Permanent Redirect`. |

---

## 3. Implementation Status: Reference Service vs. Production Target

To provide complete transparency regarding what has been constructed in the Spring Boot reference repository:

```
┌───────────────────────────────────────────────────────────┬───────────────────────────────────────────────────────────┐
│       IMPLEMENTED IN REFERENCE REPOSITORY                 │        PROPOSED FOR FULL PRODUCTION ROLLOUT               │
│       (backend-remediation-service)                       │        (Production Migration Roadmap)                     │
├───────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────┤
│ ✔ Server-authoritative DocumentReferenceService with      │ ⏳ Database-level unique constraint enforcement            │
│   SecureRandom & Crockford Base32 (> 10¹² combinations)   │    (DDL ready in schema.sql; requires active MySQL instance)│
│ ✔ In-memory collision retry loop (up to 5 retries)        │ ⏳ Transactional ACID persistence via Spring Data JPA      │
│ ✔ Deterministic SHA-256 payload checksum computation      │ ⏳ Object-level authorization (ABAC candidate ownership)   │
│ ✔ Server-side RBAC on document endpoints                  │ ⏳ Server-side PDF rendering (OpenPDF/iText integration)  │
│ ✔ HttpOnly, Secure, SameSite=Strict cookie JWT auth       │ ⏳ Distributed Token Blacklist via Redis with TTL         │
│ ✔ CSRF Protection with XSRF-TOKEN cookie & header         │ ⏳ Distributed multi-instance Rate Limiter via Redis       │
│ ✔ Token Bucket rate limiting with trusted proxy check     │ ⏳ Automated Key Management via AWS/GCP Secret Manager    │
│ ✔ RFC 7807 ProblemDetail error handling & DTO validation │ ⏳ Next.js Edge middleware.ts deployment to Vercel        │
└───────────────────────────────────────────────────────────┴───────────────────────────────────────────────────────────┘
```

---

## 4. Technical Remediation Blueprints

### Blueprint 1: Pre-Render Edge Middleware Authorization Guard (Next.js)
To prevent Next.js from streaming React Server Component payloads in redirect responses, access control checks must execute in `middleware.ts` before component rendering begins:

```typescript
// middleware.ts (Next.js Edge Guard)
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  if (pathname.startsWith('/dashboard') || pathname.startsWith('/portal/dashboard')) {
    const sessionToken = request.cookies.get('cynbit_session')?.value;
    
    // Halt evaluation BEFORE entering Server Component rendering tree
    if (!sessionToken) {
      const loginUrl = new URL('/', request.url);
      return NextResponse.redirect(loginUrl, 307);
    }
  }
  
  return NextResponse.next();
}

export const config = {
  matcher: ['/dashboard/:path*', '/portal/dashboard/:path*'],
};
```

---

### Blueprint 2: Cryptographically Secure Document Reference Generation (Java 17)
Primary business identifiers must be generated server-side using cryptographic pseudo-randomness:

```java
@Service
public class DocumentReferenceService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // Crockford Base32 (32 chars)

    public String generateReferenceNumber(DocumentType type, int year) {
        StringBuilder suffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            suffix.append(CHARSET.charAt(SECURE_RANDOM.nextInt(CHARSET.length())));
        }
        return String.format("CB/HR/%s/%d/%s", type.getCode(), year, suffix.toString());
    }
}
```

---

### Blueprint 3: Token Bucket Rate Limiting with Trusted Proxy IP Resolution
Protects public forms against flooding while respecting reverse-proxy environments:

```java
@Component
public class RateLimiterService {
    private final Set<String> trustedProxies;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService(RateLimitProperties properties) {
        this.trustedProxies = new HashSet<>(properties.getTrustedProxies());
    }

    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.contains(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}
```

---

### Blueprint 4: Defense-in-Depth HTTP Security Headers (Spring Security 6)
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .headers(headers -> headers
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; script-src 'self'; frame-ancestors 'none';"))
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            .contentTypeOptions(Customizer.withDefaults())
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(63072000))
        )
        .build();
}
```
