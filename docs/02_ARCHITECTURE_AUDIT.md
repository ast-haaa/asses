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

The application relies on Next.js Server Actions over HTTP POST rather than conventional REST endpoints (`/api/*`). The following Server Action references and operational signatures were extracted from client webpack manifests:

| Server Action Identifier | Action ID Hash | Route Context | Observable Parameters | Observed Client Payload Response |
| :--- | :--- | :--- | :--- | :--- |
| `submitInquiry` | `40247ba4465344b3...` | `/contact` | `fullName`, `email`, `company`, `industry`, `message` | `{ success: boolean }` |
| `submitCandidateApplication` | `40645a00e04f6854...` | `/careers/[slug]` | `jobId`, `fullName`, `email`, `phone`, `whatsapp`, `dob`, `gender`, `noticePeriod`, `skills`, `currentCity`, `permanentAddress`, `portfolioUrl`, `linkedinUrl`, `professionalProfileUrl`, `message`, `agreedToTerms`, `educationData` | `{ success: boolean, isDuplicate: boolean, trackingUrl: string, error: string }` |
| `loginToPortal` | `40d93da1cc26de4e...` | `portal.cynbit.in/` | `password` | `{ success: boolean, error: string }` |
| `adminGetInterns` | `00162e117247af97...` | `/dashboard/interns` | None (Session context) | Array of intern records |
| `adminCreateIntern` | `40bbe4bc0e325f31...` | `/dashboard/interns` | Intern profile fields | `{ success: boolean, error: string }` |
| `adminCreateTask` | `406f95b4b6209de5...` | `/dashboard/interns` | Task title, description, deadline | `{ success: boolean, error: string }` |
| `adminGetSubmissions` | `00c08cb87bdbc6bd...` | `.../submissions` | None | List of daily deliverables |
| `adminUpdateSubmissionStatus` | `70e82d7f651d07f2...` | `.../submissions` | `submissionId`, `status` | `{ success: boolean }` |
| `adminBatchUpdateSubmissionStatus`| `70d909e95734c649...` | `.../submissions` | `submissionIds[]`, `status` | `{ success: boolean }` |
| `fetchDocumentByRef` | `40244e9a6ff014cf...` | `.../offer-letter` | `referenceId` | Document payload metadata |
| `logDocumentToDB` | `78b2e843c9bec963...` | `.../offer-letter` | `docType`, `refNumber`, `candidateName`, `fullData` | `{ success: boolean }` |
