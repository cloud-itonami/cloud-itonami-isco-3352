(ns taxexcise.advisor
  "TaxFilingAdvisor — the advisor named in this repository's README,
  proposing a filing-documentation/logistics operation (log a filing
  record, schedule an audit appointment, flag a compliance concern
  for human review, or coordinate an office-supply order) from an
  intake request. Swappable mock/llm; the advisor ONLY proposes —
  `taxexcise.governor` independently re-derives every gate from the
  registered filer/office record and always escalates compliance
  concerns and over-threshold supply orders. Modeled on
  cloud-itonami-isco-3313's advisor.

  The advisor's op vocabulary is the SAME closed allowlist the
  governor enforces (see taxexcise.governor docstring): it can never
  propose finalizing a tax assessment, imposing a penalty, or
  ordering a collection/lien action, because no such op exists for it
  to propose. This is a documentation/logistics-coordination advisor,
  not a tax-assessment advisor.

  A proposal: {:op :log-filing-record|:schedule-audit-appointment|
                    :flag-compliance-concern|:coordinate-supply-order
               :effect :propose :filer-id str :office-id str
               :return-document-attached? boolean :filing-period str
               :appointment-date str :concern-detail str
               :order-amount number :item str
               :stake kw :confidence n :rationale str}"
  )

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake filer-id office-id
                              return-document-attached? filing-period
                              appointment-date concern-detail
                              order-amount item] :as _request}]
  {:op op
   :effect :propose
   :filer-id filer-id
   :office-id office-id
   :return-document-attached? (boolean return-document-attached?)
   :filing-period filing-period
   :appointment-date appointment-date
   :concern-detail concern-detail
   :order-amount order-amount
   :item item
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op)
                   (when filer-id (str " for filer " filer-id))
                   (when office-id (str " at office " office-id)))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a tax/excise filing-documentation and logistics-coordination
   advisor. Given a request, propose ONLY one of :log-filing-record,
   :schedule-audit-appointment, :flag-compliance-concern or
   :coordinate-supply-order, plus the relevant fields, an honest
   :confidence and a :stake. You have NO authority to finalize a tax
   assessment, impose a penalty, or order a collection/lien action —
   never propose or narrate performing any of those; if a request
   asks for one, propose :flag-compliance-concern instead so a human
   tax official can review it. Never propose logging a filing record
   without an attached return/receipt document, or scheduling an
   audit appointment beyond an office's registered daily capacity —
   the governor checks both against the registered record.
   Compliance concerns and over-threshold supply orders always
   require human sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
