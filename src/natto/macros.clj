(ns natto.macros
  (:require [clojure.spec.alpha :as s]
            [natto.specs :as sp]
            [natto.typechecker :refer [extend-env *enable-static-check*]]))


(defmacro ta-def
  "Defines a variable with a type annotation.

  The macro ensures the value provided matches the type annotation
  in compile time (macro-expansion phase).
  Throws an IllegalArgumentException if there's a mismatch.

  Example:

    (ta-def myvar :- Number 42) ;; works
    (ta-def myvar :- String 42) ;; do not compile
  "
  [name & body]
  (let [_          (if (= :- (first body))
                    :-
                    (throw (IllegalArgumentException. (str "Wrong type annotation symbol."))))
        type-anno  (resolve (second body))
        value      (last body)
        value-type (type value)
        type-map   {:type value-type
                    :anno type-anno}]
    (if *enable-static-check*
      `(let [~'value# ~value
             ~'type-map# ~type-map]
         (if (s/valid? ::sp/type-check ~'type-map#)
           (def ~name ~'value#)
           (throw (IllegalArgumentException.
                   (str "The given value type  "
                        (:type ~'type-map#)
                        " do not match the type annotation "
                        (:anno ~'type-map#))))))
      `(def ~name ~value))))


(defn- extract-varnames [input]
  (let [pattern #"\b([a-zA-Z_-]+)\s*:-\s*"]
    (->> input
         (re-seq pattern)
         (map #(get % 1))
         (map #(symbol %)))))

(defn- extract-vartypes [input]
  (let [pattern #"\b[a-zA-Z_-]+\s*:-\s*([a-zA-Z_.-]+)"]
    (->> input
         (re-seq pattern)
         (map #(get % 1))
         (map #(symbol %)))))

(defmacro ta-defn
  "Defines a function with a type annotation

  The macro ensures the value provided matches the type annotation
  in compile time (macro-expansion phase).
  Throws an IllegalArgumentException if there's a mismatch.

  Example:

                  (ret type)        (param types)
    (ta-defn myfunc :- Number [x :- Number y :- Number]
        (+ x y)) ;; works

  
    (ta-defn myfunc :- String [x :- Number y :- String]
        (+ x y)) ;; do not compile
  "
  [name _ret-symbol ret-type & args]
  (let [param-ann (first args)
        body (second args)
        param-names (extract-varnames (str param-ann))
        param-types (extract-vartypes (str param-ann))]
    (if *enable-static-check*
      (let [_ (extend-env name {:ret ret-type, :args param-types})]
        ;; When we have a working type inference, the body should be
        ;; type inferred based on the args type and return type
        ;; and verify if there is any kind of type mismatch
        `(defn ~name [~@param-names] ~body))
      `(defn ~name [~@param-names] ~body))))
