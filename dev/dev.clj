(ns dev
  "The Natto Playground.
   Evaluate forms here to test the Refinement Type System."
  (:require [natto.core :refer [def-refined assume-refined]]
            [natto.state :refer [*deep-checks*]]
            [natto.assumptions]))

;; A simple function to calculate absolute difference.
;; Z3 verifies that the result is ALWAYS positive.
(def-refined abs-diff
  [x :- Int, y :- Int]
  -> {ret [:- Int | (>= ret 0)]}
  (if (> x y)
    (- x y)
    (- y x)))

;; (abs-diff 10 5) ;; => 5
;; (abs-diff 5 10) ;; => 5



;; Z3 understands 'cond' and 'let'.
;; This function returns a logic-based signum (-1, 0, 1).
(def-refined safe-sign
  [x :- Int]
  -> {ret [:- Int | (or (= ret -1) (= ret 0) (= ret 1))]}
  (cond
    (> x 0) 1
    (< x 0) -1
    :else   0))

;; (safe-sign -50) ;; => -1



;; A safe accessor that requires the vector to be non-empty.
;; If you remove the precondition `(> (count v) 0)`, this will fail to compile!
(def-refined safe-head
  [v :- (Array Int Int) | (> (count v) 0)]
  -> Int
  (get v 0))

;; (safe-head [10 20]) ;; => 10

;; RUNTIME SAFETY CHECK:
;; This will throw an AssertionError at runtime because the contract is violated.
;; (safe-head [])


;; We can define the shape of a map (Record).
;; Here we enforce a business rule: Discounted price must be lower than original.
(def-refined apply-discount
  ;; FIX: Constrain BOTH price and discount
  [item :- (Record {:price Int, :discount Int})
        | (and (>= (:price item) 0)
               (>= (:discount item) 0))]

  -> {ret [:- Int | (and (>= ret 0) (<= ret (:price item)))]}

  (let [new-price (- (:price item) (:discount item))]
    (if (< new-price 0)
      0
      new-price)))

;; (apply-discount {:price 100 :discount 20}) ;; => 80


;; A generic identity function that works for ANY type T.
(def-refined my-identity
  :forall [T]
  [x :- T]
  -> {ret [:- T | (= ret x)]}
  x)

;; (my-identity 42)      ;; => 42
;; (my-identity "hello") ;; => "hello"


;; Note: Math/abs and legacy-sqrt are already assumed in natto.assumptions

(def-refined safe-root
  [x :- Int]
  -> Int
  (if (< x 0)
    0
    ;; Safe to call because we proved x >= 0 in this branch
    (legacy-sqrt x)))

;; (safe-root 25) ;; => 5
;; (safe-root -5) ;; => 0


;; Example A: LOGIC ERROR
;; Z3 will find a counterexample (e.g., x = 5).
#_ (def-refined buggy-math
     [x :- Int | (> x 0)]
     -> {ret [:- Int | (> ret x)]} ;; Promise: Result > Input
     (- x 1))                      ;; Bug: Result < Input


;; Example B: TYPE ERROR
;; The VCG catches this before Z3 runs.
#_ (def-refined type-mismatch
     [x :- Int, b :- Bool]
     -> Int
     (+ x b)) ;; Cannot add Int + Bool


;; Example C: STRUCTURAL ERROR
;; Accessing a field that doesn't exist in the Record definition.
#_ (def-refined bad-record
     [u :- (Record {:id Int})]
     -> Int
     (:email u)) ;; Field :email not defined
