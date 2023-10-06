(ns natto.macros-test
  (:require [clojure.test :refer :all]
            [natto.macros :refer [ta-def]]))

(deftest def-type-annotation

  (testing "Valid type annotation"
    (ta-def x :- String "Hello")
    (is (= "Hello" x)))

  (testing "Valid type annotation"
    (ta-def x :- Number 42)
    (is (= 42 x)))
  
  #_(testing "Valid type annotation with Integer"
    (ta-def z :- Integer 42)
    (is (= 42 z)))
  
  (testing "Invalid type annotation (type mismatch)"
    (is (thrown? IllegalArgumentException (ta-def y :- Integer "Hello"))))

  (testing "Invalid type annotation (wrong symbol)"
    (is (thrown? IllegalArgumentException (ta-def a :- Symbol 42)))))


; Run the tests
(run-tests)



;; tocheck
;;  (is (thrown? Exception (ta-def val :- Short '(short 42))))
;;  (is (thrown? Exception (ta-def val :- Atom (atom 42))))


#_(deftest incorrect-def-type-annotation

  (try
    (eval `(ta-def val :- Long "42"))
    (catch Exception e
      (is (instance? clojure.lang.Compiler$CompilerException e))))

  (try
    (ta-def val :- String 42)
    (catch Exception e
      (is (instance? clojure.lang.Compiler$CompilerException e))))

  (try
    (ta-def val :- Long 42)
    (catch Exception e
      (is (instance? clojure.lang.Compiler$CompilerException e))))

  (try
    (eval `(ta-def val :- Boolean 1))
    (catch Exception e
      (is (instance? clojure.lang.Compiler$CompilerException e)))))
