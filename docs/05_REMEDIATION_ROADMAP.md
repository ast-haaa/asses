# 05. Enterprise Backend Remediation Roadmap

This document outlines practical, enterprise-grade engineering improvements to address the observations identified during the technical audit.

---

## 1. Remediation Matrix

| Finding / Area | Immediate Remediation | Target Architectural State |
| :--- | :--- | :--- |
| **RSC UI Disclosure on 307 Redirects** | Enforce auth checks in Edge `middleware.ts` before component invocation. | Pre-render edge gatekeeper; unauthenticated requests are halted at the edge before any component executes. |
| **Shared-Key Portal Login** | Migrate to individual user accounts with hashed credentials and JWT / Session tokens. | Granular Role-Based Access Control (RBAC) with audit trails: `SUPER_ADMIN`, `HR_ADMIN`, `MENTOR`, `INTERN`. |
| **Weak Client Reference IDs** | Authoritative backend generation using CSPRNG or UUIDv7. | Server-generated, collision-resistant reference IDs (`CB/HR/OFFER/2026/[CSPRNG]`). |
| **Missing HTTP Security Headers** | Configure edge response headers for CSP, X-Frame-Options, and nosniff. | Defense-in-depth header policy applied uniformly at the reverse proxy or Spring Security filter chain. |
| **Wildcard CORS on HTML** | Restrict CORS to specific authorized origins and remove from HTML routes. | Principle of least privilege: CORS applied only to externalized REST/API endpoints with whitelisted domains. |
| **Form Abuse & Flooding** | Implement IP-based Token Bucket rate limiting. | Distributed rate limiter (Bucket4j / Redis) enforcing rate tiers on `/contact` and `/careers`. |
| **Client-Side PDF Generation** | Move document rendering and database logging to an authoritative backend service. | Server-side PDF compilation (OpenPDF / iText) with server-side validation and atomic persistence. |
| **Apex Domain 307 Redirect** | Update Vercel project domain configuration to use 308 Permanent Redirect. | Cached canonical domain mapping avoiding redundant network round-trips. |

---

## 2. Detailed Technical Remediation Blueprints

### Blueprint 1: Pre-Render Edge Middleware Authorization Guard (Next.js)
To prevent Next.js from streaming React Server Component payloads in redirect responses, access control checks must be executed in `middleware.ts` prior to component rendering:

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

### Blueprint 2: Cryptographically Secure Reference Generation (Spring Boot / Java)
Primary business identifiers must be authoritative, tamper-resistant, and generated server-side:

```java
@Service
public class DocumentReferenceService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // Crockford Base32

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

### Blueprint 3: Token Bucket Rate Limiting (Bucket4j)
To protect public forms (`/contact` and `/careers`) against bot abuse without disrupting legitimate candidates:

```java
@Component
public class RateLimiterService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String clientIp, String endpoint) {
        String key = clientIp + ":" + endpoint;
        return buckets.computeIfAbsent(key, k -> createNewBucket(endpoint));
    }

    private Bucket createNewBucket(String endpoint) {
        Bandwidth limit = endpoint.contains("career") 
            ? Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)))
            : Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(5)));
        return Bucket.builder().addLimit(limit).build();
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

