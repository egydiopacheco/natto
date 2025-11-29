(ns natto.vcg
  (:require [natto.state :refer [refined-registry external-call-counter]]
            [natto.z3-utils :as z3 :refer [ensure-sort]]
            [natto.types :refer [parse-base-type]])
  (:import (com.microsoft.z3 Expr Sort ArraySort IntSort BoolSort DatatypeSort)))

(declare vcg-expr)

(defn- mk-result
  ([expr sort base-type]
   {:expr expr :sort sort :base-type base-type :obligations [] :assumptions []})
  ([expr sort base-type obs asms]
   {:expr expr :sort sort :base-type base-type :obligations obs :assumptions asms}))

(def handlers
  {`+   (fn [ctx args-info _env]
          (z3/ensure-sort IntSort '+ args-info)
          (mk-result (z3/mk-add ctx (map :expr args-info)) (z3/int-sort ctx) 'Int))
   `-   (fn [ctx args-info _env]
          (z3/ensure-sort IntSort '- args-info)
          (let [expr (if (= (count args-info) 1)
                       (z3/mk-sub ctx [(z3/int-val ctx 0) (:expr (first args-info))])
                       (z3/mk-sub ctx (map :expr args-info)))]
            (mk-result expr (z3/int-sort ctx) 'Int)))
   `* (fn [ctx args-info _env]
          (z3/ensure-sort IntSort '* args-info)
          (mk-result (z3/mk-mul ctx (map :expr args-info)) (z3/int-sort ctx) 'Int))
   `>   (fn [ctx args-info _env]
          (z3/ensure-sort IntSort '> args-info)
          (mk-result (z3/mk-gt ctx (:expr (first args-info)) (:expr (second args-info))) (z3/bool-sort ctx) 'Bool))
   `<   (fn [ctx args-info _env]
          (z3/ensure-sort IntSort '< args-info)
          (mk-result (z3/mk-lt ctx (:expr (first args-info)) (:expr (second args-info))) (z3/bool-sort ctx) 'Bool))
   `>=  (fn [ctx args-info _env]
          (z3/ensure-sort IntSort '>= args-info)
          (mk-result (z3/mk-ge ctx (:expr (first args-info)) (:expr (second args-info))) (z3/bool-sort ctx) 'Bool))
   `<=  (fn [ctx args-info _env]
          (z3/ensure-sort IntSort '<= args-info)
          (mk-result (z3/mk-le ctx (:expr (first args-info)) (:expr (second args-info))) (z3/bool-sort ctx) 'Bool))
   `=   (fn [ctx args-info _env]
          (let [arg1 (first args-info) arg2 (second args-info)]
            (when (not= (.toString (:sort arg1)) (.toString (:sort arg2)))
              (throw (Exception. (str "'=' type mismatch: " (:sort arg1) " vs " (:sort arg2)))))
            (mk-result (z3/mk-eq ctx (:expr arg1) (:expr arg2)) (z3/bool-sort ctx) 'Bool)))
   `and (fn [ctx args-info _env]
          (z3/ensure-sort BoolSort 'and args-info)
          (mk-result (z3/mk-and ctx (map :expr args-info)) (z3/bool-sort ctx) 'Bool))
   `or  (fn [ctx args-info _env]
          (z3/ensure-sort BoolSort 'or args-info)
          (mk-result (z3/mk-or ctx (map :expr args-info)) (z3/bool-sort ctx) 'Bool))
   `not (fn [ctx args-info _env]
          (z3/ensure-sort BoolSort 'not args-info)
          (mk-result (z3/mk-not ctx (:expr (first args-info))) (z3/bool-sort ctx) 'Bool))
   `=>  (fn [ctx args-info _env]
          (z3/ensure-sort BoolSort '=> args-info)
          (mk-result (z3/mk-implies ctx (:expr (first args-info)) (:expr (second args-info)))
                     (z3/bool-sort ctx) 'Bool))

   `count (fn [ctx args-info env]
            (let [arg-info (first args-info) sort (:sort arg-info)]
              (cond
                (instance? ArraySort sort)
                (let [count-fn (get-in env [:uninterpreted-fns 'count] (get-in env [:uninterpreted-fns `count]))]
                  (mk-result (z3/mk-app ctx count-fn [(:expr arg-info)]) (z3/int-sort ctx) 'Int))
                (.isStringSort sort)
                (mk-result (z3/mk-length ctx (:expr arg-info)) (z3/int-sort ctx) 'Int)
                :else (throw (Exception. (str "count requires Array or String, got: " sort))))))

   `get   (fn [ctx args-info _env]
            (let [vec-info (first args-info) idx-info (second args-info)]
              (z3/ensure-sort ArraySort 'get [vec-info]) (z3/ensure-sort IntSort 'get [idx-info])
              (let [return-base-type (last (:base-type vec-info))
                    ;; **FIX**: Pass type-env for Generics
                    return-sort (parse-base-type return-base-type ctx nil (:type-env _env))]
                (mk-result (z3/mk-select ctx (:expr vec-info) (:expr idx-info)) return-sort return-base-type))))

   `assoc (fn [ctx args-info _env]
            (let [target-info (first args-info)
                  key-info    (second args-info)
                  val-info    (nth args-info 2)
                  target-sort (:sort target-info)
                  type-env    (:type-env _env)]
              (cond
                (instance? ArraySort target-sort)
                (let [vec-base (:base-type target-info)
                      domain-type (nth vec-base 1)
                      range-type  (nth vec-base 2)]
                  (z3/ensure-sort ArraySort 'assoc [target-info])
                  (z3/ensure-sort (class (parse-base-type domain-type ctx nil type-env)) 'assoc [key-info])
                  (z3/ensure-sort (class (parse-base-type range-type ctx nil type-env)) 'assoc [val-info])
                  (mk-result (z3/mk-store ctx (:expr target-info) (:expr key-info) (:expr val-info))
                             target-sort
                             vec-base))

                (instance? DatatypeSort target-sort)
                (let [field-kw (keyword (.getString (:expr key-info)))
                      fields-map (second (:base-type target-info))
                      accessors-map (:accessors-map _env)
                      sort-accessors (get accessors-map target-sort)
                      constructor (first (.getConstructors target-sort))

                      _ (when-not (contains? fields-map field-kw)
                          (throw (Exception. (str "Record has no field " field-kw))))

                      field-base-type (get fields-map field-kw)
                      _ (z3/ensure-sort (class (parse-base-type field-base-type ctx nil type-env)) 'assoc [val-info])

                      constructor-args
                      (map (fn [fname]
                             (if (= fname field-kw)
                               (:expr val-info)
                               (z3/mk-app ctx (get sort-accessors fname) [(:expr target-info)])))
                           (sort (keys fields-map)))]

                  (mk-result (z3/mk-app ctx constructor constructor-args)
                             target-sort
                             (:base-type target-info)))

                :else
                (throw (Exception. (str "assoc requires Array or Record, got: " target-sort))))))})

(def z3-op-dispatch (merge handlers (into {} (for [[k v] handlers] [(symbol (name k)) v]))))

(defn- clj-list->z3 [expr env]
  (let [op (first expr)
        translated-args-info (map #(vcg-expr % env) (rest expr))
        nested-obs (mapcat :obligations translated-args-info)
        nested-asms (mapcat :assumptions translated-args-info)]
    (cond
      (get z3-op-dispatch op)
      (let [op-fn (get z3-op-dispatch op)
            result (op-fn (:ctx env) translated-args-info env)]
        (-> result
            (update :obligations (fnil into []) nested-obs)
            (update :assumptions (fnil into []) nested-asms)))

      (keyword? op)
      (let [target-info (first translated-args-info)
            target-sort (:sort target-info)
            accessors-map (:accessors-map env)]
        (if-let [sort-accessors (get accessors-map target-sort)]
          (if-let [z3-func (get sort-accessors op)]
            (let [fields-map (second (:base-type target-info))
                  field-base-type (get fields-map op)]
              {:expr (z3/mk-app (:ctx env) z3-func [(:expr target-info)])
               :sort (.getRange z3-func)
               :base-type field-base-type
               :obligations nested-obs
               :assumptions nested-asms})
            (throw (Exception. (str "Record " target-sort " has no field " op))))
          (throw (Exception. (str "Keyword " op " used on non-Record: " target-sort)))))

      (get @refined-registry op)
      (let [contract (get @refined-registry op)
            ctx (:ctx env)
            ret-name (str (name op) "_ret_" (swap! external-call-counter inc))
            ret-base-type (:base-type (:return contract))
            ret-sort (parse-base-type ret-base-type ctx nil (:type-env env))
            ret-var (z3/const ctx ret-name ret-sort)

            arg-names (map :name (:args contract))
            subst-map (zipmap arg-names translated-args-info)

            pre-clj (cons 'and (map :refinement (:args contract)))
            pre-env (assoc env :symbols (merge (:symbols env) subst-map))
            pre-z3-result (vcg-expr pre-clj pre-env)
            pre-obligation (z3/mk-implies ctx (:precondition-z3 env) (:expr pre-z3-result))

            post-clj (:refinement (:return contract))
            post-name (:name (:return contract))
            post-env (assoc pre-env :symbols (assoc (:symbols pre-env) post-name {:expr ret-var :sort ret-sort :base-type ret-base-type}))
            post-z3-result (vcg-expr post-clj post-env)
            post-assumption (:expr post-z3-result)]

        {:expr ret-var
         :sort ret-sort
         :base-type ret-base-type
         :assumptions (concat nested-asms (:assumptions post-z3-result) [post-assumption])
         :obligations (concat nested-obs (:obligations pre-z3-result) [pre-obligation])})

      :else (throw (ex-info (str "Unsupported operator: " op) {:op op})))))

(defn- vcg-let-bindings [bindings-vec env]
  (if (empty? bindings-vec)
    [env [] []]
    (let [binding-sym (first bindings-vec)
          binding-val (second bindings-vec)
          rest-bindings (subvec bindings-vec 2)]

      (if (vector? binding-sym)
        (let [temp-sym (gensym "destruct_")
              expanded-bindings (apply concat
                                       [temp-sym binding-val]
                                       (map-indexed (fn [i sym] [sym (list 'get temp-sym i)]) binding-sym))]
          (vcg-let-bindings (vec (concat expanded-bindings rest-bindings)) env))

        (let [{:keys [obligations assumptions] :as z3-val-info} (vcg-expr binding-val env)
              new-env (assoc-in env [:symbols binding-sym] (dissoc z3-val-info :obligations :assumptions))
              [final-env rest-obs rest-asms] (vcg-let-bindings rest-bindings new-env)]
          [final-env (into obligations rest-obs) (into assumptions rest-asms)])))))

(defn- desugar-cond [clauses]
  (when (empty? clauses) (throw (Exception. "cond must have an :else clause")))
  (let [p (first clauses) e (second clauses) rest (nnext clauses)]
    (if (= :else p) e (list 'if p e (desugar-cond rest)))))

(defn- desugar-case [expr clauses]
  (let [default-val (last clauses)
        pairs (partition 2 (drop-last clauses))
        g-sym (gensym "case_")]
    (list 'let [g-sym expr]
          (cons 'cond (concat (mapcat (fn [[k v]] [(list '= g-sym k) v]) pairs) [:else default-val])))))

(defn vcg-expr [expr env]
  (let [source-meta (meta expr)
        result-map
        (cond
          (number? expr) {:expr (z3/int-val (:ctx env) expr) :sort (z3/int-sort (:ctx env)) :base-type 'Int :obligations [] :assumptions []}
          (boolean? expr) {:expr (z3/bool-val (:ctx env) expr) :sort (z3/bool-sort (:ctx env)) :base-type 'Bool :obligations [] :assumptions []}
          (or (string? expr) (keyword? expr)) (let [s (if (keyword? expr) (name expr) expr)] {:expr (z3/str-val (:ctx env) s) :sort (z3/string-sort (:ctx env)) :base-type 'String :obligations [] :assumptions []})
          (symbol? expr) (if-let [info (get-in env [:symbols expr])] (assoc info :obligations [] :assumptions []) (throw (ex-info (str "Unbound variable: " expr) {:variable expr})))
          (sequential? expr)
          (let [op (first expr)]
            (cond
              (= op 'let) (let [[bindings body] (rest expr) [final-env bind-obs bind-asms] (vcg-let-bindings bindings env) body-info (vcg-expr body final-env)] (-> body-info (update :obligations (fnil into []) bind-obs) (update :assumptions (fnil into []) (concat bind-asms (:assumptions body-info)))))
              (= op 'if) (let [[test then else] (rest expr) z3-test (vcg-expr test env) z3-then (vcg-expr then env) z3-else (vcg-expr else env)] (z3/ensure-sort BoolSort 'if [z3-test]) (if (not= (.toString (:sort z3-then)) (.toString (:sort z3-else))) (throw (Exception. "if branches must match types")) (let [all-obs (concat (:obligations z3-test) (:obligations z3-then) (:obligations z3-else)) all-asms (concat (:assumptions z3-test) (:assumptions z3-then) (:assumptions z3-else))] {:expr (z3/mk-ite (:ctx env) (:expr z3-test) (:expr z3-then) (:expr z3-else)) :sort (:sort z3-then) :base-type (:base-type z3-then) :obligations all-obs :assumptions all-asms})))
              (= op 'cond) (vcg-expr (desugar-cond (rest expr)) env)
              (= op 'case) (vcg-expr (desugar-case (second expr) (drop 2 expr)) env)
              :else (clj-list->z3 expr env)))
          :else (throw (ex-info (str "Unknown type: " (type expr)) {:expr expr})))]
    (if (and (map? result-map) source-meta) (assoc result-map :source-info source-meta) result-map)))
