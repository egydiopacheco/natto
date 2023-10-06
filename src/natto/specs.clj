(ns natto.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::typeCheck #(let [type (:type %)
                          anno (:anno %)]
                      (or (isa? type anno)
                          (isa? anno type))))
