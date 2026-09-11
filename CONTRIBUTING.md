# Contributing

`cloud-itonami-isco-3352` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
clojure -M:test
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real filer, office or credential data.
- Keep production writes and disclosures behind Tax Excise Governor.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, safety and audit logging.
- **Never add an op resembling finalizing a tax assessment, imposing a
  penalty, or ordering a collection/lien action** — this actor's op-
  allowlist is closed by design (see `taxexcise.governor`'s
  no-enforcement-authority guarantee docstring); a PR proposing such an
  op must be rejected outright, not merely gated behind escalation.
- When adding or editing governor scope-exclusion terms, phrase them as
  full finalization/execution ACTION phrases ("finalize the tax
  assessment"), never bare nouns ("assessment") — bare nouns
  false-positive on legitimate descriptive text (see
  `default-mock-advisor-proposals-never-self-trip` in
  `test/taxexcise/governor_test.cljk`).
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
