# 01. Executive Summary: Backend Technical Audit

## Target Information
- **Organization**: Cynbit Technologies Private Limited (Jaipur, Rajasthan, India)
- **Primary Domain**: [https://www.cynbit.in/](https://www.cynbit.in/)
- **Discovered Subdomain**: [https://portal.cynbit.in/](https://portal.cynbit.in/)
- **Assessment Scope**: Passive, authorized, non-disruptive backend technical audit (Backend Developer Intern Technical Assessment).
- **Audit Date**: November 2026

---

## High-Level Assessment Overview

A comprehensive backend technical audit of Cynbit Technologies' public digital infrastructure was executed under strict read-only and non-intrusive constraints. The objective was to evaluate the publicly observable website architecture, inspect API requests, HTTP headers, payloads, authentication models, validation behaviors, and performance characteristics, without performing any exploitation or active penetration testing.

```
┌────────────────────────────────────────────────────────────────────────┐
│                      AUDIT SUMMARY DASHBOARD                           │
├───────────────────────────────┬────────────────────────────────────────┤
│ Architecture / Edge Platform  │ Next.js App Router (RSC) on Vercel CDN │
│ API Architecture Paradigm     │ Server Actions RPC (No public /api/*)  │
│ Confirmed Security Findings   │ 3 (RSC UI Leak, Non-CSPRNG IDs, Headers│
│ Confirmed Config Observations │ 2 (Wildcard CORS, Shared-Key Login)    │
│ Confirmed Performance Issues  │ 1 (307 Apex Redirection Caching)       │
│ Potential Server-Side Risks   │ 3 (Requiring Authorized Verification)  │
└───────────────────────────────┴────────────────────────────────────────┘
```

---

## Key Audit Takeaways

### 1. Modern Serverless & Component Architecture
The website operates on modern cloud infrastructure powered by **Vercel's Global Edge Network** (`bom1` Mumbai node) with **Next.js App Router** and React Server Components. Traditional REST/JSON endpoints (`/api/*`) are not deployed; rather, state transitions and form submissions execute via Next.js Server Actions over HTTP POST RPC.

### 2. Observable Security & Architectural Findings (Calibrated)
- **RSC UI Template Disclosure in 307 Redirects**: When accessing protected routes on `portal.cynbit.in` (such as `/dashboard`), the server issues an HTTP 307 redirect to `/`. However, the response body contains the fully streamed React Server Component template, exposing internal feature names, tools, and route URLs to unauthenticated HTTP inspectors.
- **Client-Side Document Reference Construction**: Internal document generators construct reference numbers in the browser using `Math.random()` and submit them to server logging actions, inverting the authoritative generation of business identifiers.
- **Shared-Key Login Design**: The administrative portal features an observable single-credential ("Security Key") login model, which inherently lacks multi-user accountability, individual employee tracking, and role differentiation.
- **Defense-in-Depth Header Gaps**: Standard hardening headers (`Content-Security-Policy`, `X-Frame-Options`, `X-Content-Type-Options: nosniff`) are absent on both `www.cynbit.in` and `portal.cynbit.in`.
- **Wildcard CORS Configuration**: An `Access-Control-Allow-Origin: *` header is returned on top-level HTML document requests.

### 3. Concrete Remediation & Reference Architecture
To demonstrate production-ready engineering standards, a modular reference backend has been engineered in **Java 17 + Spring Boot 3.3**:
- Centralized Spring Security 6 filter chain with cryptographic token authentication and granular Role-Based Access Control (RBAC).
- In-memory / distributed token bucket rate limiting using **Bucket4j**.
- Cryptographically secure reference number generator utilizing CSPRNG.
- Authoritative server-side document rendering and state logging.
- Strict DTO whitelisting and RFC 7807 global exception handling.
