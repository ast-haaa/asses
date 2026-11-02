# 02. Observable Architecture & Backend Behavior Audit

## 1. Edge & Hosting Infrastructure

Passive reconnaissance through DNS resolution, TLS certificate handshakes, and HTTP response headers identified the following host topology:

- **CDN & Edge Provider**: Vercel Global Edge Network
- **Resolved IP Addresses**: `64.29.17.65`, `216.198.79.65` (Vercel Anycast IP blocks)
- **Edge PoP (Point of Presence)**: `bom1` (Mumbai, India), identified via header `X-Vercel-Id: bom1::...`
- **TLS Handshake & Certificates**:
  - Provider: Let's Encrypt (CN: `YR2`)
  - RSA 2048-bit Key Exchange (`sha256WithRSAEncryption`)
  - `www.cynbit.in`: Serial `06:1c:33:93:16:4f:bf:06:e5:bc:de:09:7f:36:df:52:0b:d4`, valid until 2026-12-24
  - `portal.cynbit.in`: Serial `05:58:d5:1d:10:87:56:d4:90:34:ca:40:68:30:03:7f:3a:90`, valid until 2027-01-04
  - HSTS: `Strict-Transport-Security: max-age=63072000` (Enforced across both subdomains)

---

## 2. Web Application Framework & Build Architecture

- **Framework**: Next.js App Router (React 18 / 19 Server Components)
- **Build Identifier**: `x-Rd73BtzTiCyyJyTHd5H` (Extracted from RSC inline scripts and HTML root comments)
- **Rendering Model**: Hybrid Pre-rendering and Server-Side Streaming:
  - Static Pages: `X-Nextjs-Prerender: 1`, `X-Vercel-Cache: HIT`, `Cache-Control: public, max-age=0, must-revalidate`
  - Streaming / Dynamic Routes: `Transfer-Encoding: chunked`, `Vary: rsc, next-router-state-tree, next-router-prefetch, next-router-segment-prefetch`
- **Asset Bundles**: Chunked webpack modules hosted under `/_next/static/chunks/` with SHA-256 fingerprinting.

---

## 3. Discovered Routing & Endpoints Map

### 3.1 Public Marketing & Career Routes (`www.cynbit.in`)
Discovered via `robots.txt`, `sitemap.xml`, and client router chunks:
- Core Pages: `/`, `/story`, `/team`, `/contact`, `/services`, `/products`, `/case-studies`, `/impact`
- Training Program: `/bitelevate`, `/bitelevate/paths`
- Editorial & Content: `/blog`, `/blog/[slug]`, `/diaries`, `/diaries/[slug]`
- Career Portal: `/careers`, `/careers/[slug]` (e.g., `/careers/backend-developer-intern`, `/careers/full-stack-developer`)

### 3.2 Internal Administrative Subdomain (`portal.cynbit.in`)
Discovered via `robots.txt` redirect directive (`Disallow: /portal/`):
- Authentication Gateway: `/` (Single "Security Key" challenge)
- Main Administrative Dashboard: `/dashboard`
- Document Automation Hubs:
  - `/dashboard/offer-letter` (Offer Letter Generator)
  - `/dashboard/bitelevate-offer` (BitElevate Trainee Selection Letter)
  - `/dashboard/joining-kit` (NDA and Employment Onboarding Documents)
  - `/dashboard/experience-letter` (Experience & Relieving Certificate Generator)
  - `/dashboard/certificates` (Internship Completion Certificates)
- Intern Deliverable Management:
  - `/dashboard/interns` (Intern Profile Directory & Task Assignment)
  - `/dashboard/interns/submissions` (Daily EOD Deliverables & PR Review Board)

---

## 4. Discovered API & Server Action RPC Interfaces

### 4.1 Observable Next.js Server Action RPC Paradigm
Passive analysis of public network responses confirms that the application does not expose traditional RESTful endpoints (requests to `/api/*` return standard Next.js 404 HTML responses). Instead, all public and administrative state-changing operations are dispatched via **Next.js Server Actions** executing over HTTP `POST` requests.

#### Observable Protocol & Transport Behavior
- **RPC Client Registration**: Client-side bundles register server references using Webpack stubs:
  ```javascript
  const actionRef = (0, d.createServerReference)(actionIdHash, d.callServer, void 0, d.findSourceMapURL, exportName);
  ```
- **HTTP Transport**:
  - **Method**: `POST` targeted at the current document path (e.g. `POST /contact` or `POST /careers/backend-developer-intern`).
  - **Headers**: Includes `Next-Action: <action-id-hash>` indicating the specific server function to invoke.
  - **Body Payload**: Form data formatted as `multipart/form-data; boundary=...` containing field values and serialized JSON strings.
  - **Response Format**: Typically returns HTTP `200 OK` wrapping a serialized React Server Component flight chunk or JSON object, even when business-level validation fails (e.g., `{ success: false, error: "..." }`).

---

### 4.2 Detailed Action Breakdown (Extracted from Public Evidence)

The following Server Actions were empirically identified and extracted from public JavaScript bundles, now preserved in sanitized form in [`evidence/EVIDENCE_LOG.md`](../evidence/EVIDENCE_LOG.md):

#### 1. Public Inquiry Submission (`submitInquiry`)
- **Action Identifier**: `[ACTION_ID_INQUIRY]` (`submitInquiry`)
- **Source Verification**: Documented in [`EVIDENCE_LOG.md § 3.2`](../evidence/EVIDENCE_LOG.md#32-public-contact-form-submitinquiry)
- **Calling Route**: `https://www.cynbit.in/contact`
- **Identified Request Fields** (`FormData`):
  - `fullName` (`string`, required): Sender's full name.
  - `email` (`string`, required): Contact email address.
  - `company` (`string`, optional): Organization or enterprise name.
  - `industry` (`string`, optional): Industry vertical (e.g. Fintech, Healthcare).
  - `message` (`string`, required): Inbound message text.
- **Observed Response Shape**:
  ```typescript
  { success: boolean }
  ```
- **Client Handling**: On `success: true`, renders an "Inquiry Received" confirmation state. On `false`, displays fallback text: `"Something went wrong. Please try again or email us directly."`

#### 2. Candidate Application Submission (`submitCandidateApplication`)
- **Action Identifier**: `[ACTION_ID_CAREER_APP]` (`submitCandidateApplication`)
- **Source Verification**: Documented in [`EVIDENCE_LOG.md § 3.3`](../evidence/EVIDENCE_LOG.md#33-candidate-application-form-submitcandidateapplication)
- **Calling Route**: `https://www.cynbit.in/careers/[slug]`
- **Identified Request Fields** (`FormData`):
  - `jobId` (`string`, required): Targeted opening slug (e.g., `"backend-developer-intern"`).
  - Personal Details: `fullName`, `email`, `phone`, `whatsapp`, `dob`, `gender`, `noticePeriod`, `skills`, `currentCity`, `permanentAddress`.
  - Portfolio & Social: `portfolioUrl`, `linkedinUrl`, `professionalProfileUrl`, `message`.
  - Compliance: `agreedToTerms` (`boolean`).
  - Synthesized Education: `education` (string concatenated from degree/specialization), `university`, `graduationYear`.
  - `educationData` (`string`, JSON-serialized array): Detailed array of qualification objects:
    ```typescript
    Array<{
      qualificationLevel: string;
      degree: string;
      customDegree?: string;
      specialization: string;
      university: string;
      graduationYear: string;
      scoreType: string;
      scoreValue: string;
      status: string;
    }>
    ```
- **Observed Response Shape**:
  ```typescript
  {
    success: boolean;
    isDuplicate?: boolean;
    trackingUrl?: string;
    error?: string;
  }
  ```
- **Client Handling**: If `success: true`, displays success confirmation. If `isDuplicate: true`, extracts `trackingUrl` to inform the candidate of existing application status. Otherwise displays `error` string.

#### 3. Administrative Portal Login (`loginToPortal`)
- **Action Identifier**: `[ACTION_ID_PORTAL_LOGIN]` (`loginToPortal`)
- **Source Verification**: Documented in [`EVIDENCE_LOG.md § 4`](../evidence/EVIDENCE_LOG.md#4-portal-shared-key-authentication-pattern)
- **Calling Route**: `https://portal.cynbit.in/`
- **Identified Request Fields** (`FormData`):
  - `password` (`string`, required): The "Security Key" secret string.
- **Observed Response Shape**:
  ```typescript
  { success: boolean; error?: string }
  ```
- **Client Handling**: On `success: true`, triggers client-side router transition to `/dashboard`.

#### 4. Document Reference & Logging Actions (`logDocumentToDB` & `fetchDocumentByRef`)
- **Action Identifiers**:
  - `logDocumentToDB`: `[ACTION_ID_DOC_LOG]` (`logDocumentToDB`)
  - `fetchDocumentByRef`: `[ACTION_ID_DOC_FETCH]` (`fetchDocumentByRef`)
- **Source Verification**: Documented in [`EVIDENCE_LOG.md § 2`](../evidence/EVIDENCE_LOG.md#2-client-side-document-reference-construction)
- **Calling Route**: `https://portal.cynbit.in/dashboard/offer-letter`
- **Identified Parameters for `logDocumentToDB`**:
  - `docType` (`string`): e.g. `"Internship Offer"` or `"Offer Letter"`.
  - `refNumber` (`string`): Client-constructed reference (e.g. `CB/HR/INT/2026/84920`).
  - `candidateName` (`string`): Candidate's full name.
  - `fullData` (`object`): Payload containing `employmentType`, `address`, `jobTitle`, `joiningDate`, `salary`, `duration`, `reportingTo`, `validUntil`.
- **Identified Parameters for `fetchDocumentByRef`**:
  - `refNumber` (`string`): Reference string passed to fetch previous document state (triggered when URL contains `?redownload=...`).

#### 5. Proposed Administrative Management Endpoints
- **Architectural Scope**: The reference remediation backend replaces client-coupled Next.js RPC calls with standard RESTful endpoints (`/api/v1/portal/interns/**`) governed by server-side Spring Security RBAC (`ROLE_SUPER_ADMIN`, `ROLE_HR_ADMIN`, `ROLE_MENTOR`).
- **Demonstration Capabilities**: Exposes intern roster querying and deliverable review workflows using synthetic demonstration data fixtures, eliminating frontend coupling and private RPC dependencies.

---

### 4.3 Observable Architectural Limitations

Based strictly on the public evidence above, several architectural limitations are evident in the current setup:
1. **Coupling to Frontend Bundles**: Server Actions lack a language-agnostic OpenAPI contract, binding backend business operations tightly to Next.js client-side JavaScript.
2. **Generic HTTP Status Masking**: Because Server Actions typically return HTTP 200 with `{ success: false }`, edge caches, monitoring systems, and client libraries cannot rely on standard HTTP semantics (`400 Bad Request`, `401 Unauthorized`, `429 Too Many Requests`).
3. **Inversion of Identifier Authority**: As documented in [`EVIDENCE_LOG.md § 2`](../evidence/EVIDENCE_LOG.md#2-client-side-document-reference-construction), primary document identifiers (`refNumber`) are generated in browser JavaScript rather than being authoritatively assigned by the server.
4. **Coarse-Grained Authentication Input**: The login form submits only a single shared password without individual user identifiers, limiting identity auditing at the gateway layer.

---

### 4.4 Boundary: Observed Behavior vs. Proposed Target API Contracts

> [!IMPORTANT]
> **Definitive Assessment Boundary**:
> - **Observed Behavior**: The descriptions above represent **empirical facts** extracted exclusively from publicly accessible client chunks (`_next/static/chunks/*.js`) and network response headers. No internal backend code, database tables, or serverless handler bodies were accessed.
> - **Proposed Contracts**: The REST endpoints specified in [`06_API_SPECIFICATION.md`](06_API_SPECIFICATION.md) and implemented in the Spring Boot reference service represent an **independent remediation blueprint** proposing standard REST conventions, HTTP status codes, and server-side authority. They are not claims about Cynbit's internal backend architecture.


