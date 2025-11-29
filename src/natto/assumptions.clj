(ns natto.assumptions
  (:require [natto.core :refer [assume-refined]]))

(assume-refined clojure.core/inc
  [x :- Int]
  -> {ret [:- Int | (= ret (+ x 1))]})

(assume-refined clojure.core/dec
  [x :- Int]
  -> {ret [:- Int | (= ret (- x 1))]})

(assume-refined clojure.core/+
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (+ x y))]})

(assume-refined clojure.core/-
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (- x y))]})

(assume-refined clojure.core/*
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (* x y))]})

(assume-refined clojure.core/quot
  [x :- Int, y :- Int | (not= y 0)]
  -> {ret [:- Int | (= ret (div x y))]})

(assume-refined clojure.core/rem
  [x :- Int, y :- Int | (not= y 0)]
  -> {ret [:- Int | (= ret (rem x y))]})

(assume-refined clojure.core/max
  [x :- Int, y :- Int]
  -> {ret [:- Int | (and (>= ret x) (>= ret y))]})

(assume-refined clojure.core/min
  [x :- Int, y :- Int]
  -> {ret [:- Int | (and (<= ret x) (<= ret y))]})

(assume-refined Math/abs
  [x :- Int]
  -> {ret [:- Int | (and (>= ret 0) (>= ret x))]})

(assume-refined clojure.core/identity
  :forall [T]
  [x :- T]
  -> {ret [:- T | (= ret x)]})

(assume-refined clojure.core/=
  :forall [T]
  [x :- T, y :- T]
  ->  {ret [:- Bool | (= ret (= x y))]})

(assume-refined clojure.core/not=
  :forall [T]
  [x :- T, y :- T]
  -> {ret [:- Bool | (= ret (not (= x y)))]})

(assume-refined clojure.core/<
  [x :- Int, y :- Int]
  -> {ret [:- Bool | (= ret (< x y))]})

(assume-refined clojure.core/>
  [x :- Int, y :- Int]
  -> {ret [:- Bool | (= ret (> x y))]})

(assume-refined clojure.core/<=
  [x :- Int, y :- Int]
  -> {ret [:- Bool | (= ret (<= x y))]})

(assume-refined clojure.core/>=
  [x :- Int, y :- Int]
  -> {ret [:- Bool | (= ret (>= x y))]})

(assume-refined clojure.core/not
  [x :- Bool]
  -> {ret [:- Bool | (= ret (not x))]})

(assume-refined clojure.core/zero?
  [x :- Int]
  -> {ret [:- Bool | (= ret (= x 0))]})

(assume-refined clojure.core/pos?
  [x :- Int]
  -> {ret [:- Bool | (= ret (> x 0))]})

(assume-refined clojure.core/neg?
  [x :- Int]
  -> {ret [:- Bool | (= ret (< x 0))]})

(assume-refined clojure.core/even?
  [x :- Int]
  -> {ret [:- Bool | (= ret (= (mod x 2) 0))]})

(assume-refined clojure.core/odd?
  [x :- Int]
  -> {ret [:- Bool | (= ret (= (mod x 2) 1))]})

(assume-refined clojure.core/true?
  [x :- Bool]
  -> {ret [:- Bool | (= ret (= x true))]})

(assume-refined clojure.core/false?
  [x :- Bool]
  -> {ret [:- Bool | (= ret (= x false))]})

(assume-refined clojure.core/empty?
  :forall [T]
  [v :- (Array Int T)]
  -> {ret [:- Bool | (= ret (= (count v) 0))]})

(assume-refined clojure.core/first
  :forall [T]
  [v :- (Array Int T) | (> (count v) 0)]
  -> {ret [:- T | (= ret (get v 0))]})

(assume-refined clojure.core/second
  :forall [T]
  [v :- (Array Int T) | (> (count v) 1)]
  -> {ret [:- T | (= ret (get v 1))]})

(assume-refined clojure.core/last
  :forall [T]
  [v :- (Array Int T) | (> (count v) 0)]
  -> {ret [:- T | (= ret (get v (- (count v) 1)))]})

(assume-refined clojure.core/peek
  :forall [T]
  [v :- (Array Int T) | (> (count v) 0)]
  -> {ret [:- T | (= ret (get v (- (count v) 1)))]})

(assume-refined clojure.core/conj
  :forall [T]
  [v :- (Array Int T), x :- T]
  -> {ret [:- (Array Int T) | (= (count ret) (+ (count v) 1))]})

(assume-refined clojure.core/pop
  :forall [T]
  [v :- (Array Int T) | (> (count v) 0)]
  -> {ret [:- (Array Int T) | (= (count ret) (- (count v) 1))]})

(assume-refined clojure.core/str
  :forall [T]
  [x :- T]
  -> String)

(assume-refined clojure.core/bit-and
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (bit-and x y))]})

(assume-refined clojure.core/bit-or
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (bit-or x y))]})

(assume-refined clojure.core/bit-xor
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (bit-xor x y))]})

(assume-refined clojure.core/bit-not
  [x :- Int]
  -> {ret [:- Int | (= ret (bit-not x))]})

(assume-refined clojure.core/bit-shift-left
  [x :- Int, n :- Int]
  -> {ret [:- Int | (= ret (bit-shift-left x n))]})

(assume-refined clojure.core/bit-shift-right
  [x :- Int, n :- Int]
  -> {ret [:- Int | (= ret (bit-shift-right x n))]})

(assume-refined clojure.core/unsigned-bit-shift-right
  [x :- Int, n :- Int]
  -> {ret [:- Int | (= ret (unsigned-bit-shift-right x n))]})

(assume-refined clojure.core/bit-set
  [x :- Int, n :- Int]
  -> {ret [:- Int | (= ret (bit-set x n))]})

(assume-refined clojure.core/bit-clear
  [x :- Int, n :- Int]
  -> {ret [:- Int | (= ret (bit-clear x n))]})

(assume-refined clojure.core/bit-flip
  [x :- Int, n :- Int]
  -> {ret [:- Int | (= ret (bit-flip x n))]})

(assume-refined clojure.core/bit-test
  [x :- Int, n :- Int]
  -> {ret [:- Bool | (= ret (bit-test x n))]})

(assume-refined clojure.core/int
  [x :- Int]
  -> {ret [:- Int | (= ret x)]})

(assume-refined clojure.core/long
  [x :- Int]
  -> {ret [:- Int | (= ret x)]})

(assume-refined clojure.core/num
  [x :- Int]
  -> {ret [:- Int | (= ret x)]})

(assume-refined clojure.core/boolean
  :forall [T]
  [x :- T]
  -> {ret [:- Bool | (= ret (not (not x)))]})

(assume-refined clojure.core/unchecked-add
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (+ x y))]})

(assume-refined clojure.core/unchecked-sub
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (- x y))]})

(assume-refined clojure.core/unchecked-mul
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (* x y))]})

(assume-refined clojure.core/unchecked-inc
  [x :- Int]
  -> {ret [:- Int | (= ret (+ x 1))]})

(assume-refined clojure.core/unchecked-dec
  [x :- Int]
  -> {ret [:- Int | (= ret (- x 1))]})

(assume-refined clojure.core/unchecked-negate
  [x :- Int]
  -> {ret [:- Int | (= ret (- 0 x))]})

(assume-refined Math/addExact
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (+ x y))]})

(assume-refined Math/subtractExact
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (- x y))]})

(assume-refined Math/multiplyExact
  [x :- Int, y :- Int]
  -> {ret [:- Int | (= ret (* x y))]})

(assume-refined Math/incrementExact
  [x :- Int]
  -> {ret [:- Int | (= ret (+ x 1))]})

(assume-refined Math/decrementExact
  [x :- Int]
  -> {ret [:- Int | (= ret (- x 1))]})

(assume-refined Math/negateExact
  [x :- Int]
  -> {ret [:- Int | (= ret (- 0 x))]})

(assume-refined Math/floorDiv
  [x :- Int, y :- Int | (not= y 0)]
  -> {ret [:- Int | (= ret (div x y))]})

(assume-refined Math/floorMod
  [x :- Int, y :- Int | (not= y 0)]
  -> {ret [:- Int | (= ret (mod x y))]})

(assume-refined Math/round
  [x :- Int]
  -> {ret [:- Int | (= ret x)]})

(assume-refined clojure.core/subvec
  :forall [T]
  [v :- (Array Int T), start :- Int, end :- Int
   | (and (>= start 0) (<= end (count v)) (<= start end))]
  -> {ret [:- (Array Int T) | (= (count ret) (- end start))]})

(assume-refined clojure.core/nth
  :forall [T]
  [v :- (Array Int T), i :- Int | (and (>= i 0) (< i (count v)))]
  -> {ret [:- T | (= ret (get v i))]})

(assume-refined clojure.core/contains?
  :forall [T]
  [v :- (Array Int T), i :- Int]
  -> {ret [:- Bool | (= ret (and (>= i 0) (< i (count v))))]})

(assume-refined clojure.core/vector
  :forall [T]
  [x :- T]
  -> {ret [:- (Array Int T) | (and (= (count ret) 1) (= (get ret 0) x))]})

(assume-refined clojure.core/vec
  :forall [T]
  [x :- (Array Int T)]
  -> {ret [:- (Array Int T) | (= ret x)]})

(assume-refined clojure.string/trim
  [s :- String]
  -> {ret [:- String | (<= (count ret) (count s))]})

(assume-refined clojure.string/triml
  [s :- String]
  -> {ret [:- String | (<= (count ret) (count s))]})

(assume-refined clojure.string/trimr
  [s :- String]
  -> {ret [:- String | (<= (count ret) (count s))]})

(assume-refined clojure.string/upper-case
  [s :- String]
  -> {ret [:- String | (= (count ret) (count s))]})

(assume-refined clojure.string/lower-case
  [s :- String]
  -> {ret [:- String | (= (count ret) (count s))]})

(assume-refined clojure.string/reverse
  [s :- String]
  -> {ret [:- String | (= (count ret) (count s))]})

(assume-refined clojure.string/starts-with?
  [s :- String, substr :- String]
  -> Bool)

(assume-refined clojure.string/ends-with?
  [s :- String, substr :- String]
  -> Bool)

(assume-refined clojure.string/includes?
  [s :- String, substr :- String]
  -> Bool)

(assume-refined clojure.core/subs
  [s :- String, start :- Int | (and (>= start 0) (<= start (count s)))]
  -> {ret [:- String | (= (count ret) (- (count s) start))]})

(assume-refined clojure.core/compare
  :forall [T]
  [x :- T, y :- T]
  -> {ret [:- Int | (or (= ret -1) (= ret 0) (= ret 1))]})

(assume-refined clojure.core/identical?
  :forall [T]
  [x :- T, y :- T]
  -> {ret [:- Bool | (= ret (= x y))]})

(assume-refined clojure.core/complement
  :forall [T]
  [x :- Bool]
  -> {ret [:- Bool | (= ret (not x))]})
