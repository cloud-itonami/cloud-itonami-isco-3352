(ns taxexcise.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [taxexcise.store :as store]
            [taxexcise.advisor :as advisor]
            [taxexcise.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-filer! st {:filer-id "filer-1" :name "Acme Trading Co"})
    (store/register-office! st {:office-id "OFFICE-1" :name "Tax Office 1"
                                :daily-appointment-capacity 3})
    st))

(def ^:private filer-req {:filer-id "filer-1"})
(def ^:private office-req {:office-id "OFFICE-1"})

(defn- log-op [attached?]
  {:op :log-filing-record :effect :propose :filer-id "filer-1"
   :return-document-attached? attached? :filing-period "2026-Q2"
   :confidence 0.9 :stake :low :rationale "logged filing return"})

(defn- schedule-op []
  {:op :schedule-audit-appointment :effect :propose :office-id "OFFICE-1"
   :appointment-date "2026-08-01" :confidence 0.9 :stake :low
   :rationale "scheduled audit appointment"})

(defn- flag-op []
  {:op :flag-compliance-concern :effect :propose :filer-id "filer-1"
   :concern-detail "late-filing pattern observed over three periods"
   :confidence 0.9 :stake :low :rationale "flagged compliance concern for human review"})

(defn- supply-op [amount]
  {:op :coordinate-supply-order :effect :propose :office-id "OFFICE-1"
   :order-amount amount :item "printer toner"
   :confidence 0.9 :stake :low :rationale "coordinated office supply order"})

(deftest ok-log-filing-record-with-return-document-attached
  (let [st (fresh-store)
        v (governor/check filer-req {} (log-op true) st)]
    (is (:ok? v))))

(deftest hard-on-missing-return-document
  (testing "logging a filing record without an attached return/receipt document is a fabricated record, not routine documentation"
    (let [st (fresh-store)
          v (governor/check filer-req {} (assoc (log-op false) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :missing-return-document (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-filer
  (let [st (fresh-store)
        v (governor/check {:filer-id "nobody"} {} (assoc (log-op true) :confidence 0.99) st)]
    (is (:hard? v))
    (is (some #(= :no-filer (:rule %)) (:violations v)))))

(deftest hard-on-unregistered-office
  (let [st (fresh-store)
        v (governor/check {:office-id "ghost"} {} (assoc (schedule-op) :confidence 0.99) st)]
    (is (:hard? v))
    (is (some #(= :no-office (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check filer-req {} (assoc (log-op true) :effect :direct-write :confidence 0.99) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-op-not-allowlisted-finalize-assessment
  (testing "no tax-assessment-finalization op exists in the allowlist — proposing one is a structural, permanent block, not a gated one"
    (let [st (fresh-store)
          v (governor/check filer-req {} {:op :finalize-tax-assessment :effect :propose
                                          :filer-id "filer-1" :confidence 0.99 :stake :low
                                          :rationale "n/a"} st)]
      (is (:hard? v))
      (is (some #(= :op-not-allowlisted (:rule %)) (:violations v))))))

(deftest hard-on-op-not-allowlisted-impose-penalty
  (let [st (fresh-store)
        v (governor/check filer-req {} {:op :impose-penalty :effect :propose
                                        :filer-id "filer-1" :confidence 0.99 :stake :low
                                        :rationale "n/a"} st)]
    (is (:hard? v))
    (is (some #(= :op-not-allowlisted (:rule %)) (:violations v)))))

(deftest hard-on-op-not-allowlisted-order-collection-action
  (let [st (fresh-store)
        v (governor/check office-req {} {:op :order-collection-action :effect :propose
                                         :office-id "OFFICE-1" :confidence 0.99 :stake :low
                                         :rationale "n/a"} st)]
    (is (:hard? v))
    (is (some #(= :op-not-allowlisted (:rule %)) (:violations v)))))

(deftest hard-on-op-not-allowlisted-order-lien-action
  (let [st (fresh-store)
        v (governor/check filer-req {} {:op :order-lien-action :effect :propose
                                        :filer-id "filer-1" :confidence 0.99 :stake :low
                                        :rationale "n/a"} st)]
    (is (:hard? v))
    (is (some #(= :op-not-allowlisted (:rule %)) (:violations v)))))

(deftest hard-on-enforcement-scope-violation-in-rationale
  (testing "even inside an allowed op, narrating a finalization/execution action is hard-blocked (defense-in-depth)"
    (let [st (fresh-store)
          v (governor/check filer-req {} (assoc (log-op true) :confidence 0.99
                                                :rationale "recommend we finalize the tax assessment now") st)]
      (is (:hard? v))
      (is (some #(= :enforcement-scope-violation (:rule %)) (:violations v))))))

(deftest hard-on-enforcement-scope-violation-in-concern-detail
  (let [st (fresh-store)
        v (governor/check filer-req {} (assoc (flag-op) :confidence 0.99
                                              :concern-detail "we should impose the penalty immediately") st)]
    (is (:hard? v))
    (is (some #(= :enforcement-scope-violation (:rule %)) (:violations v)))))

(deftest hard-on-appointment-capacity-exceeded
  (testing "scheduling beyond the office's registered daily capacity is over-scheduling, not routine logistics coordination"
    (let [st (fresh-store)]
      (dotimes [i 3]
        (store/commit-record! st {:office-id "OFFICE-1" :op :schedule-audit-appointment
                                  :payload {:appointment-date (str "slot-" i)}}))
      (let [v (governor/check office-req {} (assoc (schedule-op) :confidence 0.99) st)]
        (is (:hard? v))
        (is (some #(= :appointment-capacity-exceeded (:rule %)) (:violations v)))))))

(deftest ok-schedule-audit-appointment-within-capacity
  (let [st (fresh-store)
        v (governor/check office-req {} (schedule-op) st)]
    (is (:ok? v))))

(deftest always-escalates-flag-compliance-concern-even-at-high-confidence
  (testing "a flagged compliance concern always requires human sign-off and is never auto-commit-eligible"
    (let [st (fresh-store)
          v (governor/check filer-req {} (assoc (flag-op) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v))
      (is (not (:ok? v))))))

(deftest escalates-supply-order-above-cost-threshold
  (let [st (fresh-store)
        v (governor/check office-req {} (assoc (supply-op 5000) :confidence 0.99) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest ok-supply-order-below-cost-threshold
  (let [st (fresh-store)
        v (governor/check office-req {} (supply-op 500) st)]
    (is (:ok? v))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check filer-req {} (assoc (log-op true) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest default-mock-advisor-proposals-never-self-trip
  (testing "the mock advisor's own default/descriptive rationale and concern-detail text never matches the enforcement-scope scan for any op in the allowlist — guards the known self-tripping bug pattern where a bare-noun term list (e.g. 'penalty', 'assessment', 'audit') would false-positive on legitimate descriptive language"
    (let [adv (advisor/mock-advisor)
          st (fresh-store)
          reqs [{:op :log-filing-record :filer-id "filer-1" :stake :low
                 :return-document-attached? true :filing-period "2026-Q2"}
                {:op :schedule-audit-appointment :office-id "OFFICE-1" :stake :low
                 :appointment-date "2026-08-01"}
                {:op :flag-compliance-concern :filer-id "filer-1" :stake :low
                 :concern-detail "repeated late filings suggest a penalty and assessment review may be warranted"}
                {:op :coordinate-supply-order :office-id "OFFICE-1" :stake :low
                 :order-amount 500 :item "printer toner"}
                ;; also check the pure default (no caller-supplied free text at all)
                {:op :flag-compliance-concern :filer-id "filer-1" :stake :low}]]
      (doseq [req reqs]
        (let [proposal (advisor/-advise adv st req)]
          (is (false? (governor/enforcement-scope-violation? proposal))
              (str "op " (:op req) " self-tripped on its own default/descriptive text: " (pr-str proposal))))))))
