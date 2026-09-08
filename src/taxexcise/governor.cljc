(ns taxexcise.governor
  "TaxExciseGovernor — the independent safety/traceability layer named
  in this repository's README/business-model.md, gating every
  documentation/logistics operation an advisor may propose for a
  registered filer or office. The governor never dispatches hardware
  itself and NEVER finalizes a tax assessment, imposes a penalty, or
  orders a collection/lien action. Modeled on
  cloud-itonami-isco-3313's accountingsupport.governor.

  ## No-enforcement-authority guarantee (structural, not gated)

  Government Tax and Excise Officials carry real legal authority to
  assess tax liability, impose penalties and order audits/collections.
  This actor is a documentation/logistics-coordination robot ONLY. The
  guarantee that it never exercises, simulates exercising, or proposes
  exercising that authority is enforced TWO ways, both structural:

    1. Closed op-allowlist (`allowed-ops`). This governor recognizes
       exactly four ops: :log-filing-record, :schedule-audit-appointment,
       :flag-compliance-concern, :coordinate-supply-order. There is no
       :finalize-tax-assessment, :impose-penalty,
       :order-collection-action or :order-lien-action op ANYWHERE in
       this codebase — those verbs do not exist in the vocabulary this
       advisor/governor pair can propose or gate. This is a
       structural absence, not an escalation path: adding an
       enforcement op is impossible without editing this source file,
       it cannot be reached at runtime by any proposal content.
    2. Defense-in-depth scope-exclusion text scan
       (`enforcement-scope-phrases`) over every proposal's free-text
       fields (`:rationale`, `:concern-detail`), independent of (1).
       Even though no enforcement OP can be proposed, a broken or
       adversarial advisor could still narrate performing one inside
       an allowed op's free text (e.g. a :log-filing-record proposal
       whose rationale claims to 'finalize the tax assessment'); this
       scan hard-blocks that too. Any single observation that MAY
       warrant an assessment/penalty/audit decision is surfaced only
       via :flag-compliance-concern, which ALWAYS escalates to a human
       tax official (see `always-escalate-ops`) and is never
       auto-commit-eligible.

  ## Known self-tripping bug pattern (guarded against here)

  `enforcement-scope-phrases` are deliberately phrased as FULL
  FINALIZATION/EXECUTION ACTION PHRASES ('finalize the tax
  assessment', 'impose the penalty', 'order the collection action',
  'order the lien action') and never as bare nouns ('assessment',
  'penalty', 'audit', 'collection', 'lien'). A bare-noun term list
  would false-positive on the mock advisor's own legitimate default
  rationale text for :flag-compliance-concern (which routinely and
  correctly uses words like 'penalty' or 'assessment' descriptively
  when surfacing a concern for human review) and on
  :schedule-audit-appointment (which routinely and correctly uses the
  word 'audit'). See taxexcise.governor-test's
  default-mock-advisor-proposals-never-self-trip test, which asserts
  every op the mock advisor can produce passes this scan cleanly.

  HARD invariants (:hard? true, ALWAYS :hold, permanent,
  un-overridable — no human approval can override these):
    1. closed op-allowlist    — :op must be one of the four
                                administrative ops in `allowed-ops`;
                                any other op (including any
                                hypothetical enforcement op) is
                                structurally unrecognized and blocked.
    2. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never itself finalizes any record; it
                                only gates what the advisor may
                                commit).
    3. filer provenance       — filer-scoped ops (:log-filing-record,
                                :flag-compliance-concern) require an
                                independently REGISTERED filer record.
    4. office provenance      — office-scoped ops
                                (:schedule-audit-appointment,
                                :coordinate-supply-order) require an
                                independently REGISTERED office record.
    5. return-document basis  — :log-filing-record requires
                                `:return-document-attached?` true
                                (logging a filing record without an
                                attached return/receipt document is a
                                fabricated record, not routine
                                documentation).
    6. appointment capacity   — :schedule-audit-appointment must not
                                push the office's committed same-op
                                record count to or beyond its
                                registered `:daily-appointment-capacity`
                                (scheduling beyond the registered
                                capacity is over-scheduling, not
                                routine logistics coordination; this
                                never determines an audit's outcome,
                                only whether staffing capacity permits
                                booking it).
    7. enforcement-scope scan — no free-text field may narrate
                                finalizing a tax assessment, imposing a
                                penalty, or ordering a collection/lien
                                action (defense-in-depth per the
                                no-enforcement-authority guarantee
                                above).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off,
  regardless of confidence):
    8. :op :flag-compliance-concern always escalates immediately and
       is never auto-commit-eligible — a human tax official reviews
       and decides whether any assessment/penalty/audit action is
       warranted; this actor never decides that itself.
    9. :op :coordinate-supply-order above `supply-order-cost-threshold`.
   10. low confidence (< `confidence-floor`)."
  (:require [taxexcise.store :as store]
            [kotoba.lang.text :as str]))

(def confidence-floor 0.6)
(def supply-order-cost-threshold 2000)

(def allowed-ops
  "The closed, exhaustive op-allowlist. No assessment/penalty/collection
  op exists here or anywhere else in this codebase — see the
  no-enforcement-authority guarantee in this namespace's docstring."
  #{:log-filing-record :schedule-audit-appointment
    :flag-compliance-concern :coordinate-supply-order})

(def ^:private filer-scoped-ops #{:log-filing-record :flag-compliance-concern})
(def ^:private office-scoped-ops #{:schedule-audit-appointment :coordinate-supply-order})
(def ^:private always-escalate-ops #{:flag-compliance-concern})

(def enforcement-scope-phrases
  "Full finalization/execution ACTION phrases only — never bare nouns.
  See this namespace's docstring, 'Known self-tripping bug pattern'."
  ["finalize the tax assessment" "finalize a tax assessment"
   "impose the penalty" "impose a penalty"
   "order the collection action" "order a collection action"
   "order the lien action" "order a lien action"])

(defn- proposal-text-fields [proposal]
  (keep identity [(:rationale proposal) (:concern-detail proposal)]))

(defn enforcement-scope-violation?
  "Defense-in-depth scan: true if any free-text field on `proposal`
  narrates performing a finalization/execution action this actor
  structurally cannot perform."
  [proposal]
  (let [texts (map str/lower (proposal-text-fields proposal))]
    (boolean (some (fn [phrase] (some #(str/includes? % phrase) texts))
                   enforcement-scope-phrases))))

(defn- hard-violations [{:keys [proposal]} filer-record office-record scheduled-count]
  (let [{:keys [op return-document-attached?]} proposal
        log? (= :log-filing-record op)
        schedule? (= :schedule-audit-appointment op)
        allowlisted? (contains? allowed-ops op)
        filer-scoped? (contains? filer-scoped-ops op)
        office-scoped? (contains? office-scoped-ops op)]
    (cond-> []
      (not allowlisted?)
      (conj {:rule :op-not-allowlisted
             :detail (str "op " (pr-str op) " is not in the closed administrative allowlist — this actor has no assessment/penalty/collection op to propose")})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はいかなる記録も直接執行しない）"})

      (and allowlisted? filer-scoped? (nil? filer-record))
      (conj {:rule :no-filer :detail "未登録 filer（納税者）への提案は不可"})

      (and allowlisted? office-scoped? (nil? office-record))
      (conj {:rule :no-office :detail "未登録 office（税務署）への提案は不可"})

      (and log? filer-record (not return-document-attached?))
      (conj {:rule :missing-return-document
             :detail "原始申告書/受領書が添付されていない filing record のログは架空記録であって通常の文書化業務ではない"})

      (and schedule? office-record (number? (:daily-appointment-capacity office-record))
           (some? scheduled-count)
           (>= scheduled-count (:daily-appointment-capacity office-record)))
      (conj {:rule :appointment-capacity-exceeded
             :detail (str "office の登録済み日次予約上限 " (:daily-appointment-capacity office-record)
                          " に対し既に " scheduled-count " 件確定済み — 上限を超える予約提案は通常の日程調整業務ではない")})

      (enforcement-scope-violation? proposal)
      (conj {:rule :enforcement-scope-violation
             :detail "提案内容が課税確定・処分（罰則）賦課・徴収（留置）措置の執行に言及している — この actor にはそのような op が構造的に存在しない"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `taxexcise.store/Store`. Pure — never mutates
  the store, never itself finalizes any record."
  [request context proposal store]
  (let [op (:op proposal)
        filer-scoped? (contains? filer-scoped-ops op)
        office-scoped? (contains? office-scoped-ops op)
        filer-record (when filer-scoped? (store/filer store (:filer-id request)))
        office-record (when office-scoped? (store/office store (:office-id request)))
        scheduled-count (when (= :schedule-audit-appointment op)
                          (count (filter #(= :schedule-audit-appointment (:op %))
                                         (store/records-of-office store (:office-id request)))))
        hard (hard-violations {:request request :proposal proposal}
                              filer-record office-record scheduled-count)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        over-threshold-supply? (and (= :coordinate-supply-order op)
                                    (number? (:order-amount proposal))
                                    (> (:order-amount proposal) supply-order-cost-threshold))
        always-risky? (or (contains? always-escalate-ops op) over-threshold-supply?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
