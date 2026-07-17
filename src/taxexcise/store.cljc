(ns taxexcise.store
  "SSoT for the ISCO-08 3352 Government Tax and Excise Officials
  documentation/logistics-coordination actor (itonami actor pattern,
  ADR-2607121000 / CLAUDE.md Actors section; README's 'Robotics
  premise' — a filing-intake and archival robot performs return
  receipt logging, audit-appointment scheduling and office-supply
  coordination under this advisor/governor pair, which never
  dispatches hardware itself and NEVER exercises any tax-assessment,
  penalty or collection authority). Modeled on
  cloud-itonami-isco-3313's accountingsupport.store.

  This is a documentation/logistics-coordination robot ONLY. It has no
  op, entity or field anywhere in this namespace that finalizes a tax
  assessment, imposes a penalty, or orders a collection/lien action —
  those verbs do not appear in this store's vocabulary because the
  actor structurally cannot perform them (see taxexcise.governor).

  Domain:

    filer   — a registered taxpayer/filer whose filing activity is
              being documented {:filer-id :name}. Required provenance
              for filer-scoped ops (:log-filing-record,
              :flag-compliance-concern).
    office  — a registered tax/excise office {:office-id :name
              :daily-appointment-capacity number}.
              `:daily-appointment-capacity` is the registered ceiling
              on same-day audit-appointment SCHEDULING (a logistics
              constraint, not an audit outcome) — scheduling beyond
              the office's registered capacity is over-scheduling, not
              routine logistics coordination. Required provenance for
              office-scoped ops (:schedule-audit-appointment,
              :coordinate-supply-order).
    record  — a committed operating record (a logged filing, a
              scheduled appointment, a flagged compliance concern, or
              a supply-order coordination) — written ONLY via
              commit-record!.
    ledger  — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (filer [s filer-id])
  (office [s office-id])
  (records-of-filer [s filer-id])
  (records-of-office [s office-id])
  (ledger [s])
  (register-filer! [s f])
  (register-office! [s o])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (filer [_ filer-id] (get-in @a [:filers filer-id]))
  (office [_ office-id] (get-in @a [:offices office-id]))
  (records-of-filer [_ filer-id] (filter #(= filer-id (:filer-id %)) (:records @a)))
  (records-of-office [_ office-id] (filter #(= office-id (:office-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-filer! [s f]
    (swap! a assoc-in [:filers (:filer-id f)] f) s)
  (register-office! [s o]
    (swap! a assoc-in [:offices (:office-id o)] o) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:filers {} :offices {} :records [] :ledger []}
                                   seed)))))
