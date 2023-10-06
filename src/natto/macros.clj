(ns natto.macros
  (:require [clojure.spec.alpha :as s]
            [natto.specs :as sp]))


(defmacro ta-def
  [name & body]
  (let [type-symb (if (= :- (first body))
                    :-
                    (throw (IllegalArgumentException. (str "Wrong type annotation symbol."))))
        type-anno  (resolve (second body))
        value      (last body)
        value-type (type value)
        type-map   {:type value-type
                    :anno type-anno}]
    `(let [~'value# ~value
           ~'type-map# ~type-map]
       (if (s/valid? ::sp/typeCheck ~'type-map#)
         (def ~name ~'value#)
         (throw (IllegalArgumentException. (str "The given value type  " (:type ~'type-map#)" do not match the type annotation " (:anno ~'type-map#))))))))
