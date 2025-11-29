(ns natto.core-test
  (:require [clojure.test :refer :all]
            [natto.core :refer :all]
            [natto.state :refer [*deep-checks*]])
  (:import (clojure.lang ExceptionInfo)))

;;; TEST HELPERS
(defmacro is-compile-fail
  "Verifies that the provided form fails to compile with a Refinement check error.
   This handles the fact that the Clojure compiler wraps macro exceptions."
  [form]
  `(try
     (macroexpand '~form)
     (is false "Expected compile-time failure, but verification succeeded!")
     (catch clojure.lang.Compiler$CompilerException e#
       (let [cause# (.getCause e#)
             msg# (if cause# (.getMessage cause#) "")]
         (if (or (clojure.string/includes? msg# "Refinement check")
                 (clojure.string/includes? msg# "Type Error")
                 (clojure.string/includes? msg# "Return Type Mismatch"))
           (is true) ;; Success: Caught the Z3 rejection
           (do (println "Wrong error type:" msg#) (throw e#)))))
     (catch ExceptionInfo e#
       (if (or (clojure.string/includes? (.getMessage e#) "Refinement check")
               (clojure.string/includes? (.getMessage e#) "Type Error")
               (clojure.string/includes? (.getMessage e#) "Return Type Mismatch"))
         (is true)
         (throw e#)))))

(defmacro expect-error
  "Verifies that compilation fails with an error message containing `match-text`."
  [match-text form]
  `(try
     (macroexpand '~form)
     (is false (str "Should have failed, but succeeded! Expected: " ~match-text))
     (catch Exception e#
       (let [msg# (if (instance? clojure.lang.Compiler$CompilerException e#)
                    (.getMessage (.getCause e#))
                    (.getMessage e#))]
         (if (clojure.string/includes? msg# ~match-text)
           (is true)
           (do (println "Caught WRONG Error:" msg#) (is false)))))))

;;; EXTERNAL CONTRACT SETUP
(defn legacy-sqrt [x] (int (Math/sqrt x)))

(assume-refined legacy-sqrt
  [x :- Int | (>= x 0)]
  -> {ret [:- Int | (and (>= ret 0)
                         (= (* ret ret) x))]})

(assume-refined Math/abs
  [x :- Int | (>= x 0)]
  -> {ret [:- Int | (= ret x)]})


;;; BASIC ARITHMETIC & LOGIC TESTS

(deftest arithmetic-logic-test
  (testing "Simple integer arithmetic verification"
    ;; Logic: if x > y, then x - y must be positive.
    (def-refined abs-diff [x :- Int, y :- Int]
      -> {ret [:- Int | (>= ret 0)]}
      (if (> x y) (- x y) (- y x)))
    (is (= 5 (abs-diff 10 5)))

    ;; Logic: x * x is always >= 0.
    (def-refined square-pos [x :- Int]
      -> {ret [:- Int | (>= ret 0)]}
      (* x x))
    (is (= 25 (square-pos -5))))

  (testing "Logic Failure Cases"
    ;; Logic: Claims to return x + 1 (greater), but returns x - 1 (smaller).
    ;; Z3 Goal: true => ((x - 1) > x)  -> FALSE
    (is-compile-fail
     (def-refined bad-inc [x :- Int]
       -> {ret [:- Int | (> ret x)]}
       (- x 1)))))


;;; CONTROL FLOW TESTS
(deftest control-flow-test
  (testing "Let Bindings & Sequential Scoping"
    ;; Logic: z depends on y, y depends on x.
    ;; x=10 -> y=15 -> z=30 -> ret=40. Post-condition checks math.
    (def-refined chained-let [x :- Int]
      -> {ret [:- Int | (= ret (+ (* 2 (+ x 5)) 10))]}
      (let [y (+ x 5)
            z (* 2 y)]
        (+ z 10)))
    (is (= 40 (chained-let 10))))

  (testing "Cond Desugaring (Case -> Cond -> If)"
    ;; Logic: Sign function using 'cond'
    (def-refined safe-sign [x :- Int]
      -> {ret [:- Int | (or (= ret 1) (= ret -1) (= ret 0))]}
      (cond
        (> x 0) 1
        (< x 0) -1
        :else   0))
    (is (= -1 (safe-sign -50))))

  (testing "Case Desugaring"
    ;; Logic: Traffic light using 'case'
    (def-refined traffic-light
      [color :- Int]
      -> {ret [:- String | (or (= ret "stop") (= ret "go") (= ret "yield"))]}
      (case color
        0 "stop"
        1 "go"
        "yield"))
    (is (= "stop" (traffic-light 0)))))


;;; ARRAY & VECTOR TESTS

(deftest array-theory-test
  (testing "Array Access (Get)"
    ;; Logic: If index 0 is > 10, returning it satisfies > 0.
    (def-refined valid-get [v :- (Array Int Int) | (> (get v 0) 10)]
      -> {ret [:- Int | (> ret 0)]}
      (get v 0))
    (is (= 20 (valid-get [20]))))

  (testing "Array Modification (Assoc)"
    ;; Logic: (store v 0 99) at index 0 must be 99.
    ;; This tests the axiom: (select (store a i v) i) == v
    (def-refined valid-assoc [v :- (Array Int Int)]
      -> {ret [:- (Array Int Int) | (= (get ret 0) 99)]}
      (assoc v 0 99))
    (is (= [99 2 3] (valid-assoc [1 2 3]))))

  (testing "Array Logic Failure"
    ;; Logic: Claims to update index 'i', but actually updates index 0.
    ;; Fails when i != 0.
    (is-compile-fail
     (def-refined bad-assoc
       [v :- (Array Int Int), i :- Int | (> i 0)]
       ;; Post: Result has 99 at index i
       -> {ret [:- (Array Int Int) | (= (get ret i) 99)]}
       ;; Body: Puts 99 at index 0 (Bug!)
       (assoc v 0 99)))))


;;; RECORD (MAP) TESTS
(deftest record-access-test
  (testing "Basic Record Definition and Access"
    ;; 1. Define a User record with an ID
    (def-refined get-user-id
      [u :- (Record {:id Int}) | (> (:id u) 0)]
      -> {ret [:- Int | (= ret (:id u))]}
      (:id u))

    ;; Runtime check (Shallow)
    (is (= 10 (get-user-id {:id 10})))

    ;; Runtime contract violation
    (is (thrown? AssertionError (get-user-id {:id -5})))))

(deftest record-logic-test
  (testing "Logic spanning multiple fields"
    ;; 2. A 'Bank Account' record.
    ;;    We verify that if a user is active, their balance must be positive.
    (def-refined validate-active-user
      [acc :- (Record {:active Bool, :balance Int})
             | (=> (:active acc) (> (:balance acc) 0))]
      -> Bool
      (:active acc))

    (is (true? (validate-active-user {:active true :balance 100})))
    (is (false? (validate-active-user {:active false :balance -50})))
    (is (thrown? AssertionError (validate-active-user {:active true :balance -10})))))

(deftest nested-record-test
  (testing "Nested Record Access"
    ;; 3. Accessing a Record inside a Record
    (def-refined get-nested-x
      [outer :- (Record {:inner (Record {:x Int})})]
      -> {ret [:- Int | (= ret (:x (:inner outer)))]}
      (:x (:inner outer)))

    (is (= 42 (get-nested-x {:inner {:x 42}})))))

(deftest record-update-test
  (testing "Assoc on Record (Reconstruction)"
    (def-refined update-id
      [u :- (Record {:id Int, :val Int}) | (= (:id u) 10)]
      ;; Post: The returned record has ID 99, but 'val' is unchanged
      -> {ret [:- (Record {:id Int, :val Int})
               | (and (= (:id ret) 99)
                      (= (:val ret) (:val u)))]}
      (assoc u :id 99))

    (is (= {:id 99 :val 50} (update-id {:id 10 :val 50})))))


;;; GENERICS TESTS

(deftest generic-array-test
  (testing "Accessing an Array of Generic T"
    ;; A generic 'nth' implementation
    (def-refined safe-nth
      :forall [T]
      [v :- (Array Int T), i :- Int]
      -> {ret [:- T | (= ret (get v i))]}
      (get v i))

    ;; Runtime: Works with Integers
    (is (= 42 (safe-nth [10 42 99] 1)))

    ;; Runtime: Works with Strings (Polymorphism!)
    (is (= "hello" (safe-nth ["bye" "hello"] 1)))))

(deftest generic-record-test
  (testing "Updating a Record field of type T"
    ;; A generic 'Box' updater
    (def-refined update-box
      :forall [T]
      [box :- (Record {:item T, :id Int})
       new-val :- T]

      ;; Post: ID stays same, item is updated
      -> {ret [:- (Record {:item T, :id Int})
               | (and (= (:id ret) (:id box))
                      (= (:item ret) new-val))]}

      (assoc box :item new-val))

    (let [int-box {:id 1 :item 100}
          str-box {:id 2 :item "foo"}]

      (is (= {:id 1 :item 999} (update-box int-box 999)))
      (is (= {:id 2 :item "bar"} (update-box str-box "bar"))))))

(deftest generic-failure-test
  (testing "Type Error: Mixing Generics"
    ;; You cannot return B when A is expected.
    (expect-error "Return Type Mismatch"
      (def-refined bad-swap
        :forall [A B]
        [x :- A, y :- B]
        -> A
        y)))

  (testing "Type Error: Operation on Opaque Type"
    ;; We cannot increment T.
    (expect-error "expects arguments of type IntSort"
      (def-refined bad-math
        :forall [T]
        [x :- T]
        -> T
        (+ x 1)))))


;;; EXTERNAL CONTRACT TESTS

(deftest external-contract-test
  (testing "Safe External Call"
    ;; Logic: We ensure x > 10, so x is positive (safe for sqrt).
    ;; Post: We return the result of sqrt.
    ;; We check that the result squared equals the input.
    (def-refined safe-ext [x :- Int | (> x 10)]
      -> {ret [:- Int | (= (* ret ret) x)]}
      (legacy-sqrt x))

    ;; 16 is a perfect square > 10, so this works with Integers
    (is (= 4 (safe-ext 16))))

  (testing "Unsafe External Call (The Explosion Test)"
    (is-compile-fail
     (def-refined unsafe-ext [x :- Int | (= x -5)]
       -> Int
       (legacy-sqrt x)))))


;;; RUNTIME SAFETY TESTS
(deftest runtime-safety-test
  (testing "Deep Type Checks"
    (def-refined sum-vec [v :- (Array Int Int)] -> Int 0)

    ;; 1. Valid Input
    (is (= 0 (sum-vec [1 2 3])))

    ;; 2. Invalid Type (String in Int Array)
    (is (thrown? AssertionError (sum-vec [1 "not-an-int" 3])))

    ;; 3. Disabled Checks
    (binding [*deep-checks* false]
      (is (= 0 (sum-vec [1 "not-an-int" 3]))))))


;;; UX / ERROR MESSAGE TESTS
(deftest ux-error-messages-test
  (testing "UX: Type Mismatch (Operator)"
    (expect-error "Operator '+' expects arguments of type IntSort"
      (def-refined bad-types [x :- Int, b :- Bool]
        -> Int
        (+ x b))))

  (testing "UX: Return Type Mismatch"
    (expect-error "Return Type Mismatch!"
      (def-refined bad-return [x :- Int]
        -> Int
        "Not an Int")))

  (testing "UX: Structural Error (Missing Field)"
    (expect-error "has no field :email"
      (def-refined bad-field [u :- (Record {:id Int})]
        -> Int
        (:email u))))

  (testing "UX: Logic Verification Failure (Counterexample)"
    (expect-error "Counterexample:"
      (def-refined logic-fail [x :- Int | (> x -10)]
        -> {ret [:- Int | (>= ret 0)]}
        (- x 1)))))
