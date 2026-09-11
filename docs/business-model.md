# Business Model: Government Tax and Excise Officials Documentation & Logistics Coordination

## Classification

- Repository: `cloud-itonami-isco-3352`
- ISCO-08: `3352`
- Occupation: Government Tax and Excise Officials
- Social impact: tax-compliance-transparency, public-revenue-integrity,
  administrative-efficiency

## Scope note — documentation/logistics only, no enforcement authority

Government Tax and Excise Officials hold real legal authority to
assess tax liability, impose penalties and order audits/collections.
**This business model covers only the documentation and logistics
work around that authority** — filing-record intake, audit-appointment
scheduling, office-supply coordination, and surfacing compliance
observations for human review. It structurally excludes assessment,
penalty and collection decisions; see the README's "No-enforcement-
authority guarantee" and [`taxexcise.governor`](../src/taxexcise/governor.cljk).

## Customer

- tax/excise administration offices
- government revenue departments

## Offer

- filing/return-receipt record logging
- audit-appointment scheduling coordination (staffing/logistics only —
  never the audit outcome)
- office-supply procurement coordination
- compliance-observation flagging for human tax-official review

## Revenue

- per-office operating license
- per-filing-record documentation fee

## Trust Controls

- no filing record logged without an attached return/receipt document
- no audit appointment scheduled beyond the office's registered daily
  staffing capacity
- every compliance observation is surfaced only via
  `:flag-compliance-concern`, which always escalates immediately to a
  human tax official and is never auto-commit-eligible
- the op-allowlist is closed: no op resembling finalizing a tax
  assessment, imposing a penalty, or ordering a collection/lien action
  exists in this codebase — this is a structural, permanent exclusion,
  not a runtime gate
- documentation and scheduling records are auditable, not editable
