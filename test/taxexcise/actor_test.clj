(ns taxexcise.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [taxexcise.actor :as actor]
            [taxexcise.advisor :as advisor]
            [taxexcise.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-filer! st {:filer-id "filer-1" :name "Acme Trading Co"})
    (store/register-office! st {:office-id "OFFICE-1" :name "Tax Office 1"
                                :daily-appointment-capacity 3})
    st))

(deftest commits-a-filing-record-with-return-document-attached
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:filer-id "filer-1" :op :log-filing-record :stake :low
                 :return-document-attached? true :filing-period "2026-Q2"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of-filer st "filer-1"))))))

(deftest holds-a-filing-record-without-return-document
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:filer-id "filer-1" :op :log-filing-record :stake :low
                 :return-document-attached? false :filing-period "2026-Q2"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of-filer st "filer-1")))))

(deftest interrupts-then-approves-flag-compliance-concern-on-human-approval
  (testing "a flagged compliance concern is never auto-commit-eligible: the graph always interrupts before commit, and only a resumed (human-approved) thread posts the record"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:filer-id "filer-1" :op :flag-compliance-concern :stake :low
                   :concern-detail "late-filing pattern observed over three periods"}
          interrupted (actor/run-request! graph request {} "thread-3")]
      (is (= :interrupted (:status interrupted)))
      (is (empty? (store/records-of-filer st "filer-1")))
      (let [resumed (actor/approve! graph "thread-3")]
        (is (= :done (:status resumed)))
        (is (= 1 (count (store/records-of-filer st "filer-1"))))))))

(deftest commits-a-schedule-audit-appointment-within-capacity
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:office-id "OFFICE-1" :op :schedule-audit-appointment :stake :low
                 :appointment-date "2026-08-01"}
        result (actor/run-request! graph request {} "thread-4")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of-office st "OFFICE-1"))))))

(deftest holds-a-schedule-audit-appointment-beyond-capacity
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})]
    (dotimes [i 3]
      (store/commit-record! st {:office-id "OFFICE-1" :op :schedule-audit-appointment
                                :payload {:appointment-date (str "slot-" i)}}))
    (let [request {:office-id "OFFICE-1" :op :schedule-audit-appointment :stake :low
                   :appointment-date "2026-08-05"}
          result (actor/run-request! graph request {} "thread-5")]
      (is (= :hold (:disposition (:state result))))
      (is (= 3 (count (store/records-of-office st "OFFICE-1")))))))

(deftest holds-when-a-misbehaving-advisor-proposes-an-unallowlisted-enforcement-op
  (testing "even if an advisor is broken/adversarial and proposes an op resembling tax-assessment finalization, the actor structurally cannot commit it: no such op is recognized by the governor, so :decide always routes to :hold, never :commit — the closed allowlist is enforced end-to-end through the graph, not merely at the governor unit-test level"
    (let [st (fresh-store)
          rogue-advisor (reify advisor/Advisor
                          (-advise [_ _store request]
                            {:op :finalize-tax-assessment :effect :propose
                             :filer-id (:filer-id request) :confidence 0.99 :stake :low
                             :rationale "attempting to finalize the tax assessment"}))
          graph (actor/build-graph {:store st :advisor rogue-advisor})
          request {:filer-id "filer-1" :op :finalize-tax-assessment :stake :high}
          result (actor/run-request! graph request {} "thread-6")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of-filer st "filer-1")))
      (let [ledger-entry (last (store/ledger st))]
        (is (= :hold (:disposition ledger-entry)))
        (is (some #(= :op-not-allowlisted (:rule %))
                  (:violations (:verdict ledger-entry))))))))
