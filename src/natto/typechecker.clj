(ns natto.typechecker)


(def ^:dynamic *enable-static-check* false)


(def ^:private Γ 
  "This defines Γ, which is the type environment"
  (atom {}))

(defn static-status
  "Returns the status of the static checking system"
  []
  *enable-static-check*)

(defn extend-env
  "Extends the type environment with a pair of symbol/type

  Example: (extend-env some-symbol {:ret ret-type
                                    :args args-type})
  "
  [symbol type]
  (swap! Γ assoc symbol type))

(defn lookup-type
  "Look up for a symbol in the type environment"
  [symbol]
  (get @Γ symbol))

(defn toggle-static-checking
  "Call this function to toggle static checking on type annotated function/constant"
  []
  (if *enable-static-check*
    (do
      (alter-var-root #'*enable-static-check* (constantly false))
      (println "Static checking is now disabled."))
    (do
      (alter-var-root #'*enable-static-check* (constantly true))
      (println "Static checking is now enabled."))))

(defn- unquote
  "Unquote an expression.
  Example: '(my-func x y) => (my-func)"
  [expr]
  (if (and (seq? expr)
           (= 'quote (first expr)))
    (second expr)
    expr))

(defn- fn-call?
  [expr]
  (and (seq? expr)
       (symbol? (first expr))))

(defn- fn-name-from-call
  [expr]
  (first expr))

(defn- fn-args-from-call
  [expr]
  (rest expr))

(defn extract-meta
  [expr]
  (when (seq? expr)
    (meta (first expr))))

(defmacro typechecker
  [expr]
  (letfn [(analyze [expr]
            (let [unquoted-expr (unquote expr)]
              (if (fn-call? unquoted-expr)
                (let [fn-name (fn-name-from-call unquoted-expr)
                      fn-type (lookup-type fn-name)
                      arg-expressions (fn-args-from-call unquoted-expr)]
                  (when (nil? fn-type)
                    (throw (Exception. (str "This function was not defined nor annotated: '" fn-name "'"))))
                  (doseq [[arg-type arg-expr] (map vector (:args fn-type) arg-expressions)]
                    (let [actual-type (analyze arg-expr)]
                      (when (and actual-type (not (= arg-type actual-type)))
                        (throw (Exception. (str "Type mismatch in call to " fn-name 
                                                ", expected: " arg-type 
                                                ", got: " actual-type))))))
                  (:ret fn-type))
                nil)))]
    (analyze expr)))

