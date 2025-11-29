(ns natto.core
  (:require [clojure.walk :as walk]
            [natto.state :refer [refined-registry *deep-checks* *verify-contracts*]]
            [natto.types :refer [parse-args parse-return parse-base-type base-type->clj-pred]]
            [natto.vcg :refer [vcg-expr]]
            [natto.z3-utils :as z3 :refer [parse-z3-model uninterpreted-sort]])
  (:import (com.microsoft.z3 Status Sort)))

(defn => [p q] (or (not p) q))

(defmacro assume-refined [fn-name & forms]
  (let [has-generics? (= :forall (first forms))
        forms         (if has-generics? (drop 2 forms) forms)
        args-vec (first forms)
        sep (second forms)
        ret-form (nth forms 2)
        _ (when (not= '-> sep) (throw (Exception. "Expected '->' separator.")))

        parsed-args (parse-args args-vec)
        parsed-return (parse-return ret-form)]

    (swap! refined-registry assoc fn-name {:args parsed-args :return parsed-return})

    `(do (swap! refined-registry assoc '~fn-name
                {:args '~(vec parsed-args)
                 :return '~(first (vec (list parsed-return)))})
         (println (format "Assumed contract for '%s'" '~fn-name)))))

(defmacro def-refined [fn-name & forms]
  (let [has-generics? (= :forall (first forms))
        type-params   (if has-generics? (second forms) [])
        forms         (if has-generics? (drop 2 forms) forms)
        args-vec (first forms) sep (second forms) ret-form (nth forms 2) body (nth forms 3)
        _ (when (not= '-> sep) (throw (Exception. "Expected '->'")))
        parsed-args (parse-args args-vec) parsed-return (parse-return ret-form)

        arg-names (vec (map :name parsed-args))
        ret-name (:name parsed-return)
        ret-ref (:refinement parsed-return)
        post-logic (walk/postwalk-replace {ret-name '%} ret-ref)
        arg-checks (map (fn [a] `(or (not *deep-checks*) (~(base-type->clj-pred (:base-type a)) ~(:name a)))) parsed-args)
        ret-check `(or (not *deep-checks*) (~(base-type->clj-pred (:base-type parsed-return)) ~'%))]

    (if-not *verify-contracts*
      `(defn ~fn-name ~arg-names {:pre [~@arg-checks] :post [~ret-check]} ~body)

      (with-open [ctx# (z3/make-context)]
        (let [solver# (z3/make-solver ctx#)
              accessors-atom (atom {})
              type-env# (into {} (for [t type-params] [t (uninterpreted-sort ctx# (name t))]))

              symbols-map# (into {} (for [{:keys [name base-type]} parsed-args]
                                      (let [sort (parse-base-type base-type ctx# accessors-atom type-env#)
                                            expr (z3/const ctx# (str name) sort)]
                                        [name {:expr expr :sort sort :base-type base-type}])))
              accessors-map# @accessors-atom
              int-sort# (z3/int-sort ctx#) array-sort# (z3/array-sort ctx# int-sort# int-sort#)
              count-fn# (.mkFuncDecl ctx# "count" (into-array Sort [array-sort#]) int-sort#)
              uninterpreted-fns# {`count count-fn# 'count count-fn#}

              base-env {:ctx ctx# :solver solver# :symbols symbols-map#
                        :uninterpreted-fns uninterpreted-fns# :accessors-map accessors-map#
                        :type-env type-env#}

              pre-clj (cons 'and (map :refinement parsed-args))
              pre-result (vcg-expr pre-clj base-env)
              initial-env (assoc base-env :precondition-z3 (:expr pre-result))
              body-result (vcg-expr body initial-env)

              declared-ret-sort# (parse-base-type (:base-type parsed-return) ctx# nil type-env#)
              decl-str# (.toString declared-ret-sort#)
              act-str# (.toString (:sort body-result))
              _ (when (not= decl-str# act-str#) (throw (ex-info (str "Return Type Mismatch! Declared " decl-str# " Got " act-str#) {})))

              post-env (assoc-in initial-env [:symbols (:name parsed-return)] body-result)
              post-result (vcg-expr (:refinement parsed-return) post-env)

              all-asms (concat (:assumptions pre-result) (:assumptions body-result) (:assumptions post-result))
              all-obs (concat (:obligations pre-result) (:obligations body-result) (:obligations post-result))

              ;;LHS (z3/mk-and ctx# (cons (:expr pre-result) all-asms))
              ;;RHS (z3/mk-and ctx# (cons (:expr post-result) all-obs))
              P-main (:expr pre-result)
              Q-main (:expr post-result)
              safety-vc (z3/mk-implies ctx# P-main (z3/mk-and ctx# all-obs))
              assumptions-combined (z3/mk-and ctx# (cons P-main all-asms))
              functional-vc (z3/mk-implies ctx# assumptions-combined Q-main)
              ;;FINAL-VC (z3/mk-implies ctx# LHS RHS)
              FINAL-VC (z3/mk-and ctx# [safety-vc functional-vc])

              _# (z3/solver-add solver# (z3/mk-not ctx# FINAL-VC))

              status# (z3/solver-check solver#)]

          (if (= status# Status/UNSATISFIABLE)
            (do (println (format "Refinement check for '%s' passed!" fn-name))
                `(defn ~fn-name ~arg-names {:pre [~pre-clj ~@arg-checks] :post [~post-logic ~ret-check]} ~body))
            (let [model# (.getModel solver#)
                  counterexample# (parse-z3-model model#)
                  obs-strs# (mapv #(.toString %) all-obs)
                  error-msg# (str "Refinement check for '" fn-name "' FAILED!"
                                  "\n  • Status: " status#
                                  "\n  • Counterexample: " counterexample#
                                  "\n  • Obligations: " obs-strs#)]
              (throw (ex-info error-msg# {:z3-result status# :counterexample counterexample# :obligations obs-strs#})))))))))
