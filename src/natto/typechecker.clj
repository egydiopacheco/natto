(ns natto.typechecker)


(def ^:dynamic *enable-static-check* false)

(defn toggle-static-checking []
  (if *enable-static-check*
    (do
      (alter-var-root #'*enable-static-check* (constantly false))
      (println "Static checking is now disabled."))
    (do
      (alter-var-root #'*enable-static-check* (constantly true))
      (println "Static checking is now enabled."))))
