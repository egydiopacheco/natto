(ns natto.types
  (:require [natto.z3-utils :as z3])
  (:import (com.microsoft.z3 Context)))

(defn parse-base-type
  ;; Arity 2: Just context (Default acc=nil, env={})
  ([type-form ctx]
   (parse-base-type type-form ctx nil {}))

  ;; Arity 3: Context + Accumulator (Default env={})
  ([type-form ctx acc-atom]
   (parse-base-type type-form ctx acc-atom {}))

  ;; Arity 4: Full implementation
  ([type-form ^Context ctx acc-atom type-env]
   (cond
     (= type-form 'Int)    (z3/int-sort ctx)
     (= type-form 'Bool)   (z3/bool-sort ctx)
     (= type-form 'String) (z3/string-sort ctx)

     ;; Handle Generic Type Variables
     (and (symbol? type-form) (contains? type-env type-form))
     (get type-env type-form)

     ;; Array Sort: (Array Int Int)
     (and (list? type-form) (= (first type-form) 'Array))
     (let [[_ domain range] type-form
           domain-sort (parse-base-type domain ctx acc-atom type-env)
           range-sort  (parse-base-type range ctx acc-atom type-env)]
       (z3/array-sort ctx domain-sort range-sort))

     ;; Record Sort: (Record {:x Int})
     (and (list? type-form) (= (first type-form) 'Record))
     (let [fields (second type-form)
           parsed-fields (reduce-kv (fn [m k v]
                                      (assoc m k (parse-base-type v ctx acc-atom type-env)))
                                    {}
                                    fields)
           rec-sort (z3/mk-record-sort ctx parsed-fields)]
       (when acc-atom
         (swap! acc-atom assoc rec-sort (z3/get-record-accessors rec-sort fields)))
       rec-sort)

     :else (throw (Exception. (str "Unsupported base type: " type-form))))))

(defn base-type->clj-pred [type-form]
  (cond
    (= type-form 'Int)    'integer?
    (= type-form 'Bool)   'boolean?
    (= type-form 'String) 'string?

    (and (list? type-form) (= (first type-form) 'Array))
    (let [[_ _ range-type] type-form]
      `(fn [x#] (and (vector? x#) (every? ~(base-type->clj-pred range-type) x#))))

    (and (list? type-form) (= (first type-form) 'Record))
    'map?

    ;; Fallback for generics or unknown types
    :else 'any?))

(defn parse-args [args-vec]
  (loop [remaining args-vec
         parsed []]
    (if (empty? remaining)
      parsed
      (let [sym (first remaining)]
        (when-not (symbol? sym)
          (throw (Exception. (str "Expected argument name (symbol), got: " sym))))
        (let [sep1 (second remaining)]
          (if (= sep1 ':-)
            (let [base-type (nth remaining 2 nil)
                  sep2      (nth remaining 3 nil)
                  predicate (nth remaining 4 nil)]
              (when (nil? base-type) (throw (Exception. (str "Missing type for argument: " sym))))
              (if (= sep2 '|)
                (recur (subvec remaining 5)
                       (conj parsed {:name sym :base-type base-type :refinement predicate}))
                (recur (subvec remaining 3)
                       (conj parsed {:name sym :base-type base-type :refinement true}))))
            (recur (rest remaining)
                   (conj parsed {:name sym :base-type 'Int :refinement true}))))))))

(defn parse-return [ret-form]
  (if (map? ret-form)
    (let [name      (first (keys ret-form))
          [sep1 base-type sep2 predicate] (first (vals ret-form))]
      (when (or (not= ':- sep1) (not= '| sep2))
        (throw (Exception. (str "Invalid return type syntax. Expected {name [:- Type | predicate]}. Got: " ret-form))))
      {:name name :base-type base-type :refinement predicate})
    {:name '% :base-type ret-form :refinement true}))
