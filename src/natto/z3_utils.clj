(ns natto.z3-utils
  (:import (com.microsoft.z3 Context Solver Model BoolExpr Expr Sort DatatypeSort)))
;; ;; --- Context & Solver ---
(defn make-context [] (Context.))
(defn make-solver [^Context ctx] (.mkSolver ctx))

;; --- Sorts (Types) ---
(defn int-sort [^Context ctx] (.mkIntSort ctx))
(defn bool-sort [^Context ctx] (.mkBoolSort ctx))
(defn string-sort [^Context ctx] (.mkStringSort ctx))
(defn array-sort [^Context ctx d r] (.mkArraySort ctx d r))
(defn uninterpreted-sort [^Context ctx name] (.mkUninterpretedSort ctx (.mkSymbol ctx name)))

;; --- Constants & Literals ---
(defn int-const [^Context ctx name] (.mkIntConst ctx (str name)))
(defn bool-const [^Context ctx name] (.mkBoolConst ctx (str name)))
(defn const [^Context ctx name ^Sort sort] (.mkConst ctx (str name) sort))
(defn int-val [^Context ctx val] (.mkInt ctx val))
(defn bool-val [^Context ctx val] (if val (.mkTrue ctx) (.mkFalse ctx)))
(defn str-val [^Context ctx val] (.mkString ctx val))

;; --- Logic & Arithmetic ---
(defn mk-gt [^Context ctx a b] (.mkGt ctx a b))
(defn mk-lt [^Context ctx a b] (.mkLt ctx a b))
(defn mk-ge [^Context ctx a b] (.mkGe ctx a b))
(defn mk-le [^Context ctx a b] (.mkLe ctx a b))
(defn mk-eq [^Context ctx a b] (.mkEq ctx a b))

(defn mk-add [^Context ctx args] (.mkAdd ctx (into-array Expr args)))
(defn mk-sub [^Context ctx args] (.mkSub ctx (into-array Expr args)))
(defn mk-mul [^Context ctx args] (.mkMul ctx (into-array Expr args)))

(defn mk-and [^Context ctx args] (.mkAnd ctx (into-array BoolExpr args)))
(defn mk-or  [^Context ctx args] (.mkOr ctx (into-array BoolExpr args)))
(defn mk-not [^Context ctx arg]  (.mkNot ctx arg))
(defn mk-implies [^Context ctx p q] (.mkImplies ctx p q))
(defn mk-ite [^Context ctx c t e] (.mkITE ctx c t e))

;; --- Arrays/Sequences ---
(defn mk-select [^Context ctx arr idx] (.mkSelect ctx arr idx))
(defn mk-store  [^Context ctx arr idx val] (.mkStore ctx arr idx val))
(defn mk-length [^Context ctx seq] (.mkLength ctx seq))

;; --- Functions ---
(defn mk-app [^Context ctx func args] (.mkApp ctx func (into-array Expr args)))

;; --- Solver Actions ---
(defn solver-add [^Solver solver constraint]
  (.add solver ^"[Lcom.microsoft.z3.BoolExpr;" (into-array BoolExpr [constraint])))

(defn solver-check [^Solver solver] (.check solver))

(defn parse-z3-model [^Model model]
  (into {}
        (for [decl (.getConstDecls model)]
          (let [name (-> decl .getName .toString symbol)
                value (-> model (.getConstInterp decl) (.toString))]
            [name value]))))

(defn ensure-sort [expected-sort-class op-symbol args-info]
  (doseq [{:keys [sort expr source-info]} args-info]
    (when-not (instance? expected-sort-class sort)
      (let [data {:operator op-symbol
                  :expected-type (.getSimpleName expected-sort-class)
                  :actual-type (class sort)
                  :offending-expression (.toString expr)}
            final-data (if source-info (merge data source-info) data)
            loc-str (if source-info
                      (str " (line " (:line source-info) ", col " (:column source-info) ")")
                      "")]
        (throw (ex-info (format "Type Error: Operator '%s' expects arguments of type %s%s."
                                op-symbol
                                (.getSimpleName expected-sort-class)
                                loc-str)
                        final-data))))))

(defn mk-record-sort [^Context ctx field-map]
  (let [field-names (sort (keys field-map))
        field-sorts (map field-map field-names)
        z3-field-names (into-array com.microsoft.z3.Symbol (map #(.mkSymbol ctx (name %)) field-names))
        z3-field-sorts (into-array Sort field-sorts)
        z3-sort-refs   (int-array (count field-names) 0)
        constructor (.mkConstructor ctx
                                    (.mkSymbol ctx "mk-Record")
                                    (.mkSymbol ctx "is-Record")
                                    z3-field-names
                                    z3-field-sorts
                                    z3-sort-refs)]
    (.mkDatatypeSort ctx (.mkSymbol ctx (str "Record_" (hash field-map))) (into-array [constructor]))))

(defn get-record-accessors [^DatatypeSort z3-sort field-map]
  (let [field-names (sort (keys field-map))
        accessors (first (.getAccessors z3-sort))]
    (zipmap field-names accessors)))
