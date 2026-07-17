# cloud-itonami-isco-3352

Open Occupation Blueprint for **ISCO-08 3352**: Government Tax and Excise Officials.

This repository designs a forkable OSS business for tax/excise-office
filing documentation and logistics coordination: a filing-intake and
archival robot manages return-receipt logging, audit-appointment
scheduling and office-supply coordination under a governor-gated
actor, so the office keeps its own documentation trail instead of
renting a closed case-management SaaS.

## No-enforcement-authority guarantee — read this first

Government Tax and Excise Officials carry **real legal authority** to
assess tax liability, impose penalties and order audits/collections.
**This actor never exercises, simulates exercising, or proposes
exercising any of that authority.** It is a documentation/logistics-
coordination robot ONLY. This is enforced structurally, not merely
gated:

- The op-allowlist (`taxexcise.governor/allowed-ops`) contains
  **exactly four** administrative ops — `:log-filing-record`,
  `:schedule-audit-appointment`, `:flag-compliance-concern`,
  `:coordinate-supply-order` — and nothing resembling finalizing a
  tax assessment, imposing a penalty, or ordering a collection/lien
  action exists anywhere in this codebase for the advisor to propose
  or the governor to gate. Adding such an op would require editing
  this repository's source; no proposal content can reach it at
  runtime.
- A second, independent defense-in-depth layer
  (`taxexcise.governor/enforcement-scope-phrases`) scans every
  proposal's free text for finalization/execution action phrases
  (e.g. "finalize the tax assessment", "impose the penalty") and
  hard-blocks the proposal if found — even inside an otherwise
  allowed op.
- Any observation the robot logs that suggests an assessment,
  penalty or audit action *might* be warranted is surfaced **only**
  via `:flag-compliance-concern`, which **always escalates
  immediately** to a human tax official and is **never
  auto-commit-eligible**. The actor decides nothing about the
  underlying compliance question — it only routes the observation to
  a human.

**Maturity: `:implemented`.** `src/taxexcise/` implements the
`TaxExciseActor` as a `langgraph.graph/state-graph`
(`taxexcise.actor`) wired to a `TaxFilingAdvisor`
(`taxexcise.advisor`) and an independent `TaxExciseGovernor`
(`taxexcise.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+->
:commit (:ok?) +-> :request-approval (:escalate?, human-in-the-loop
interrupt) +-> :hold (:hard?)`. 24 tests / 54 assertions green
(`clojure -M:test`). HARD invariants (always hold,
permanent, un-overridable): closed op-allowlist, no-actuation
(`:effect` must be `:propose`), independently verified/registered
filer or office provenance before any action, an attached
return/receipt document before any filing record can be logged
(logging without one is a fabricated record, not routine
documentation), audit-appointment scheduling never pushed beyond the
office's registered daily capacity (a staffing/logistics ceiling —
this never determines an audit's outcome), and the enforcement-scope
text scan above. Always-escalate (human sign-off regardless of
confidence): `:flag-compliance-concern` (always, immediately) and
`:coordinate-supply-order` above the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a
**robot performs the physical/administrative domain work**. Here a
filing-intake and archival robot performs return-receipt scanning,
filing-record logging, audit-appointment-slot scheduling and office
supply-order coordination under an actor that proposes actions and an
independent **Tax Excise Governor** that gates them. The governor
never dispatches hardware itself and never exercises tax-assessment,
penalty or collection authority; `:high`/`:safety-critical` actions
(such as any compliance concern, or a supply order above the
registered cost threshold) require human sign-off.

## Core Contract

```text
filing intake + return/receipt documents + office staffing capacity
        |
        v
TaxFilingAdvisor -> TaxExciseGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated, documentation/logistics only) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
finalize a tax assessment, impose a penalty, order a collection/lien
action, suppress an operating record, or disclose sensitive data
without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3352`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
