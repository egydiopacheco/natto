(ns natto.typechecker)


(def ^:dynamic *enable-static-check* false)


(def ^:private Γ 
  "This defines Γ, which is the type environment"
  (atom {}))

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

(defn toggle-static-checking []
  (if *enable-static-check*
    (do
      (alter-var-root #'*enable-static-check* (constantly false))
      (println "Static checking is now disabled."))
    (do
      (alter-var-root #'*enable-static-check* (constantly true))
      (println "Static checking is now enabled."))))
