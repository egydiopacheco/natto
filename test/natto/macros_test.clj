(ns natto.macros-test
  (:require [clojure.test :refer :all]
            [natto.macros :refer [ta-def]]))

(deftest correct-def-type-annotation

  (try
    (ta-def value :- Long 42)
    (is (= value 42))
    (catch Exception e
      (is false (str "An unexpected exception was thrown: " e))))
  
  (try
    (ta-def value :- String "42")
    (is (= value "42"))
    (catch Exception e
      (is false (str "An unexpected exception was thrown: " e))))

  (try
    (ta-def value :- Boolean true)
    (is (= value true))
    (catch Exception e
      (is false (str "An unexpected exception was thrown: " e))))
  
    (try
      (ta-def value :- Number 42)
      (is (= value 42))
      (catch Exception e
        (is false (str "An unexpected exception was thrown: " e)))))


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
