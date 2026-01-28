## DSS service contract

Goal: run DSS as an PDF validation service for the intranet.

### Runtime
- HTTP service (Docker / Cloud Run)
- Port: 8080
- Auth: Google IAM (ID token). Do NOT allow unauthenticated access.
- Scope: PDF/PAdES only for v1.
- Revocation: CRL + OCSP enabled.
- Trust lists: EU LOTL + NL TSL (or configured equivalent).

### Required endpoints

#### POST /api/validate/pdf
Request:
- multipart/form-data
  - file: PDF (required)
  - mode: "summary" (optional, default summary)
  - policy: "default" (optional)
  - requireRevocation: "true" | "false" (default true)
  - trustList: "eu" (optional)

Response (summary JSON):
{
  "overall": "VALID | INDETERMINATE | INVALID",
  "signatureCount": 1,
  "signatures": [
    {
      "id": "sig-1",
      "status": "VALID",
      "signingTime": "2026-01-24T20:25:05Z",
      "signer": "CN=...",
      "revocation": "OK | FAIL | INDETERMINATE",
      "trust": "OK | FAIL | INDETERMINATE",
      "substatus": ["PAdES_BASELINE_T", "LTV_OK"]
    }
  ],
  "errors": []
}

Error behavior:
- 4xx for bad input (missing/invalid PDF)
- 5xx for internal errors
- When possible, still return JSON with overall=INDETERMINATE + errors[]

#### GET /health
Returns 200 OK if the service is ready.

### Notes
- Keep output stable; intranet UI will map directly to this summary.
- If revocation/trust checks are unreachable, return overall=INDETERMINATE.
