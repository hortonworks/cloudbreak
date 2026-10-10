# Encryption profile compliance is computed server-side from a dedicated approved cipher set

The compliance standards for an encryption profile (currently only `FIPS_140_3`) are computed by the Environment service on every read. The service evaluates the profile's TLS versions and the cipher suites that Cloudbreak allows (via `CipherSuiteProvider`) and returns the result in `EncryptionProfileResponse.complianceStandards`.

The evaluation uses a dedicated FIPS 140-3 approved cipher set that is intentionally separate from the set of "recommended" cipher suites:
- **Recommended cipher suites** provide editor guidance. They can legitimately diverge from compliance requirements, so reusing them for compliance would silently change compliance results whenever the guidance changes.
- **Compliance** is a strict, standard-specific rule that must be enforced consistently across all clients. For that reason, it is computed server-side and is never persisted.

As a result, the value automatically reflects profile edits, and clients such as the UI, CLI, and SDK do not need to reimplement the rule.

## Considered Options

- **Boolean `fipsCompliant`**: simpler, but each additional standard or future FIPS revision would require another API change; a list of standard names would not.
- **Deriving compliance from the recommended cipher suites**: avoids maintaining a separate list, but couples compliance behavior to editor guidance.
- **Configurable approved set in `application.yml`**: would allow different deployments to report different compliance results for the same profile, which is exactly the drift this decision avoids.
- **`clouderaInternalCipherSuites` is always empty in the response today**: there is currently nothing to evaluate from that field. The evaluator must be extended if and when the field is populated.
- **FIPS 140-3 only**: this is the only compliance standard currently checked, because it is enforced across cloud service providers through FedRAMP and by defense contractors under DoD/CMMC guidelines.
