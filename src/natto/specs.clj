(ns natto.specs
  (:require [clojure.core :as core]
            [clojure.spec.alpha :as s]))

(s/def ::core-type #(let [type (:type %)
                          anno (:anno %)]
                      (or (isa? type anno)
                          (isa? anno type))))

(s/def ::vec-of-type
  (fn [{:keys [value anno]}]
    (and (vector? value)
         (every? #(isa? (type %) anno) value))))

(s/def ::map-of-type
  (fn [{:keys [value val-type]}]
    (and (map? value)
         (every? #(isa? (type (val %)) val-type) value))))

(s/def ::refined-type 
  (fn [{:keys [value anno]}]
    (case anno
      :positive-integer (and (integer? value) (> value 0))
      :negative-integer (and (integer? value) (< value 0))
      :zero             (and (integer? value) (= value 0))
      false)))

(s/def ::type-check
  (s/or :core     ::core-type
        :seq      ::vec-of-type
        :map      ::map-of-type
        :refined  ::refined-type))
