# Operator Guide

## First Deployment

1. Define the office's registered filers and audit-office staffing
   capacity.
2. Define consent and purpose categories for filing-record
   documentation.
3. Run synthetic operating cases (filing logs, appointment scheduling,
   supply orders, compliance flags).
4. Enable human-reviewed sign-off for `:high`/`:safety-critical`
   actions — every `:flag-compliance-concern` and every over-threshold
   `:coordinate-supply-order`.
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path for every compliance observation
- provenance for all filing/scheduling/procurement records
- human review for all compliance-concern flags (never auto-decided)
- audit export for all gated actions

## Out of scope — do not operate this actor for

- finalizing a tax assessment
- imposing a penalty
- ordering a collection or lien action

These are structurally absent from this actor's op-allowlist (see
[`taxexcise.governor`](../src/taxexcise/governor.kotoba)). If your
deployment needs any of the above, that authority belongs to a human
tax official operating outside this actor, using this actor's
`:flag-compliance-concern` output only as an input to their own
decision.

## Certification

Certified operators must prove that the governor gates every
safety-critical robot action, that every compliance observation
escalates to a human tax official, and that no enforcement-authority
op has been added to the allowlist.
