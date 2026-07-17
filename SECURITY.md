# Security Policy

This project handles government tax and excise officials' filing-
documentation and logistics-coordination workflows. Treat
vulnerabilities as potentially high impact even when the demo data is
synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real filer or office data exposure
- authorization bypass
- Tax Excise Governor bypass
- any path that would let the actor finalize a tax assessment, impose
  a penalty, or order a collection/lien action (this must never be
  reachable — report it as a critical vulnerability, not a feature gap)
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on filer/office data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real filer/office data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
