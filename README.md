# Natto: A Hybrid Refinement Type System for Clojure

Natto is a proof-of-concept verification system that brings **Refinement
Types** to a subset of the Clojure programming language.\
By integrating the **Z3 Theorem Prover** directly into the Clojure macro
expansion phase, Natto allows developers to mathematically prove the
correctness of their code at compile time while maintaining optional
runtime enforcement.

------------------------------------------------------------------------

## Theory and Concepts

Standard type systems allow developers to specify the shape of data
(e.g., `Integer`, `Boolean`, `List`).\
**Refinement Types** extend this by attaching logical predicates to
these types (e.g., "an Integer `x` such that `x > 0`").

Natto implements a **Hybrid Verification** model:

### Static Verification

During compilation, the code is translated into logical formulas via a
**Verification Condition Generator (VCG)**.\
These formulas are sent to the SMT solver **Z3**.\
If the solver proves the formulas valid, the code is guaranteed to
satisfy its contract for all possible inputs.

### Runtime Enforcement

Refinement predicates are compiled into standard Clojure `:pre` and
`:post` assertions.\
This provides a safety net for interactions with unverified code and
allows for gradual adoption.

------------------------------------------------------------------------

## Prerequisites

To use Natto, the environment must satisfy:

-   **Clojure:** Version 1.11 or higher\
-   **Z3 Theorem Prover:** Shared libraries installed and available on
    `java.library.path`\
-   **Z3 Java Bindings:** `com.microsoft.z3` JAR included in project
    dependencies

------------------------------------------------------------------------

## Installation

Add the Z3 dependency to your `project.clj`:

``` clojure
:dependencies [[org.clojure/clojure "1.11.1"]
               [com.microsoft.z3/com.microsoft.z3 "4.12.2"]]
```

Ensure the native Z3 library path is provided to the JVM:

``` bash
lein repl -J-Djava.library.path=/path/to/z3/lib
```

------------------------------------------------------------------------

## Usage

### 1. Basic Refinements

The core macro is **`def-refined`**.\
It accepts type annotations using:

    [name :- BaseType | Predicate]

``` clojure
(ns my-app.core
  (:require [natto.core :refer [def-refined assume-refined]]))

;; Define a function that accepts an Integer and returns a non-negative integer.
(def-refined abs-diff
  [x :- Int, y :- Int]
  -> {ret [:- Int | (>= ret 0)]}
  (if (> x y)
    (- x y)
    (- y x)))
```

------------------------------------------------------------------------

### 2. Arrays and Data Structures

Natto maps Clojure vectors to the SMT **Theory of Arrays** and supports
`count`, `get`, `assoc`.

``` clojure
;; Safely retrieves the first element of a non-empty vector.
(def-refined safe-head
  [v :- (Array Int Int) | (> (count v) 0)]
  -> Int
  (get v 0))
```

------------------------------------------------------------------------

### 3. Records (Maps)

Heterogeneous maps are modeled as **Z3 Algebraic Datatypes**.

``` clojure
;; Validate a user record.
(def-refined validate-user
  [u :- (Record {:id Int, :active Bool})]
  -> Bool

  (if (:active u)
    (> (:id u) 0)
    false))
```

------------------------------------------------------------------------

### 4. Generics (Polymorphism)

Natto supports generics via **Uninterpreted Sorts**.

``` clojure
;; Generic identity function.
(def-refined identity-gen
  :forall [T]
  [x :- T]
  -> {ret [:- T | (= ret x)]}
  x)
```

------------------------------------------------------------------------

### 5. External Contracts

Use `assume-refined` to register trusted contracts for external
functions.

``` clojure
;; Assume contract for Math/sqrt requiring non-negative input.
(assume-refined Math/sqrt
  [x :- Int | (>= x 0)]
  -> {ret [:- Int | (= (* ret ret) x)]})

;; Use the external function safely.
(def-refined safe-calc [y :- Int | (> y 0)]
  -> Int
  (Math/sqrt y))
```

------------------------------------------------------------------------

## Configuration

Natto provides dynamic variables for controlling verification behavior:

### `natto.state/*verify-contracts*`

-   **Default:** `true`\
-   When `false`, skips verification and emits a normal function
    definition.

### `natto.state/*deep-checks*`

-   **Default:** `true`\
-   When `false`, skips deep runtime checks (e.g., scanning arrays).\
-   Provides **O(1)** performance in production vs **O(N)** development
    safety.

------------------------------------------------------------------------

## Architecture

Natto is composed of five modules:

-   **state:** Global registries for external contracts and toggles\
-   **z3_utils:** Functional wrapper over the Z3 Java API\
-   **types:** Parses annotation syntax, maps types to Z3 Sorts\
-   **vcg:** Traverses the AST, generating obligations & assumptions\
-   **core:** Public API macros; assembles and solves the final formula

------------------------------------------------------------------------

## Roadmap and Next Steps

This is a proof of concept. Future work is organized into tiers:

------------------------------------------------------------------------

### **Tier 1: Syntactic Enhancements**

-   Argument destructuring\
-   Explicit `do` block verification\
-   String operations via Z3 Sequence Theory

------------------------------------------------------------------------

### **Tier 2: Data Structures**

-   Homogeneous maps (`Map String Int`)\
-   Sets using Z3 Set Theory\
-   Deep `update-in` support

------------------------------------------------------------------------

### **Tier 3: Recursion and Control Flow**

-   Loop/Recur with loop invariants and termination metrics\
-   Recursive functions verification

------------------------------------------------------------------------

### **Tier 4: Higher-Order Functions**

-   Function types (e.g., `(Fn [Int] -> Int)`)\
-   Verified `map`, `filter`, `reduce` via Z3 axioms

------------------------------------------------------------------------

### **Tier 5: Advanced Polymorphism**

-   Bounded quantification: `:forall [T :< Number]`\
-   Enables arithmetic on generic types
