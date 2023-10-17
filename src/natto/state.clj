(ns natto.state)

;; Toggle the refined type checking during development
(def ^:dynamic *verify-contracts* true)

;; Toggle O(N) runtime checks on param types. I.e: check all the values of an array is of type Int
(def ^:dynamic *deep-checks* true)

;; Registry for external function contracts
(defonce refined-registry (atom {}))

;; Counter for unique Z3 variable names
(def external-call-counter (atom 0))
