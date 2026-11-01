# 06. Target API Specification (OpenAPI / REST Equivalent)

This specification defines a standard, RESTful API architecture replacing ad-hoc RPC Server Actions with structured, versioned, and documented HTTP endpoints.

---

## 1. Public Inquiry API

### `POST /api/v1/inquiries`
Processes public inquiries submitted from the contact form.

#### Request Headers
```http
Content-Type: application/json
Accept: application/json
X-Forwarded-For: <client-ip>
```

#### Request Body
```json
{
  "fullName": "Alex Rivera",
  "email": "alex.rivera@example.com",
  "company": "Fintech Solutions Ltd",
  "industry": "Financial Technology",
  "message": "Interested in evaluating autonomous agent workflows for risk assessment."
}
```

#### Response (`201 Created`)
```json
{
  "status": "SUCCESS",
  "message": "Inquiry successfully received.",
  "data": {
    "inquiryId": "inq_01HZX87K9W4M",
    "receivedAt": "2026-11-01T11:45:00Z"
  }
}
```

#### Error Response (`429 Too Many Requests`)
```json
{
  "type": "https://api.cynbit.in/errors/rate-limit-exceeded",
  "title": "Rate Limit Exceeded",
  "status": 429,
  "detail": "Maximum of 10 inquiries per 5 minutes allowed from this IP address.",
  "instance": "/api/v1/inquiries"
}
```

---

## 2. Public Career Application API

### `POST /api/v1/careers/{jobId}/applications`
Handles multi-field intern and engineer candidate applications.

#### Request Parameters
- `jobId` (Path, required): Identifier or slug of the targeted job opening (e.g., `backend-developer-intern`).

#### Request Body
```json
{
  "fullName": "Priya Sharma",
  "email": "priya.sharma@example.com",
  "phone": "+919876543210",
  "whatsapp": "+919876543210",
  "dob": "2003-05-14",
  "gender": "FEMALE",
  "noticePeriod": "Immediate",
  "skills": "Java, Spring Boot, PostgreSQL, Docker, Redis",
  "currentCity": "Jaipur",
  "permanentAddress": "32/115 Pratap Nagar, Jaipur, Rajasthan 302033",
  "portfolioUrl": "https://priyasharma.dev",
  "linkedinUrl": "https://linkedin.com/in/priyasharma",
  "professionalProfileUrl": "https://github.com/priyasharma",
  "message": "Eager to architect robust enterprise backend systems at Cynbit.",
  "agreedToTerms": true,
  "educationHistory": [
    {
      "qualificationLevel": "Bachelor's Degree",
      "degree": "B.Tech / B.E.",
      "specialization": "Computer Science & Engineering",
      "university": "Rajasthan Technical University",
      "graduationYear": 2025,
      "scoreType": "CGPA",
      "scoreValue": "8.75",
      "status": "Currently Pursuing"
    }
  ]
}
```

#### Response (`201 Created`)
```json
{
  "status": "SUCCESS",
  "message": "Application submitted successfully.",
  "data": {
    "applicationId": "app_01HZX89AB321",
    "isDuplicate": false,
    "trackingUrl": "https://portal.cynbit.in/status/app_01HZX89AB321",
    "submittedAt": "2026-11-01T11:45:00Z"
  }
}
```

---

## 3. Administrative Portal Authentication API

### `POST /api/v1/portal/auth/login`
Authenticates administrative personnel with individual user accounts.

#### Request Body
```json
{
  "username": "hr.admin@cynbit.in",
  "password": "StrongSecretPassword#2026"
}
```

#### Response (`200 OK`)
Sets `HttpOnly`, `Secure`, `SameSite=Strict` cookie containing the signed JWT:
```http
HTTP/1.1 200 OK
Set-Cookie: cynbit_session=<jwt-token>; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=28800
Content-Type: application/json

{
  "status": "SUCCESS",
  "message": "Authentication successful.",
  "data": {
    "userId": "usr_99812",
    "username": "hr.admin@cynbit.in",
    "role": "HR_ADMIN",
    "expiresIn": 28800
  }
}
```

---

## 4. Protected Document Logging & Retrieval API

### `POST /api/v1/portal/documents`
Generates and logs an authoritative HR document record. Requires `ROLE_HR_ADMIN` or `ROLE_SUPER_ADMIN`.

#### Request Body
```json
{
  "documentType": "OFFER_LETTER",
  "candidateName": "Rohit Verma",
  "employmentType": "Internship",
  "joiningDate": "2026-12-01",
  "salary": "Stipend",
  "duration": "6 Months",
  "reportingTo": "Lead Architect",
  "validUntil": "2026-11-15"
}
```

#### Response (`201 Created`)
```json
{
  "status": "SUCCESS",
  "message": "Document record created and reference assigned.",
  "data": {
    "referenceNumber": "CB/HR/INT/2026/8K3N7P2A",
    "documentType": "OFFER_LETTER",
    "candidateName": "Rohit Verma",
    "generatedAt": "2026-11-01T11:45:00Z",
    "checksum": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  }
}
```

### `GET /api/v1/portal/documents/{referenceNumber}`
Retrieves a logged document by its cryptographic reference number.
- Requires active authenticated session.
- Returns `404 Not Found` if the reference does not exist.

