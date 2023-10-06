(ns dev
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.analyzer.jvm :as ana.jvm]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]
            [clojure.tools.analyzer.ast :as ast]
            [clojure.tools.namespace.repl :refer [refresh]]))


(s/def ::numberT Number)
(s/def ::numberT #(instance? java.lang.Number %))
(s/def ::numberT #(validate-type-annotation % Long))


(s/def ::number number?)
(s/def ::string string?)



;; runtime check
(defmacro t-def
  [name & body]
  (let [type-symb (if (= typ-symb (first body))
                    typ-symb
                    (throw (IllegalArgumentException. (str "Wrong type annotation symbol."))))
        type-anno (second body)
        value (last body)]
    `(let [value# ~value]
       (if (and ~type-anno (not (instance? ~type-anno value#)))
         (throw (IllegalArgumentException. (str "Value does not match specified type: " value# " is not of type " ~type-anno)))
         (def ~name value#)))))


(isa? Number Long)
(supers (class Long))

(defn check-type [param1 target-type]
  (if (= target-type (type param1))
    (println "param1 matches the target type.")
    (if (isa? (type param1) target-type)
      (println "param1 inherits the target type")
      (println "param1 does not match the target type."))))

(type param1)
(def param1 42) ; Example value of param1
(check-type param1 Integer) ; Check if param1 is an Integer
(check-type param1 String)  ; Check if param1 is a String
(check-type param1 Double)  ; Check if param1 is a Number
(check-type param1 Boolean) ; Check if param1 is a Boolean


#_(s/def ::typeAnn (fn [param1 param2]
                   (fn [%] ; % is the argument of the inner function
                     (and (isa? param1 %)
                          (isa? param2 %)))))


;(s/def ::test (and #(string? %) #(number? %)))

;(s/valid? ::test "1" 42) 


;(s/def ::typeAnn (fn [param1 param2]
;                   (or (isa? param1 param2)
;                       (isa? param2 param1))))



(isa? Long java.lang.Long)
(or true false)


;(def xs [Number Long])

;(def fx {:type String
;         :anno Number})

(let [mytype (:type fx)
      myanno (:anno fx)])

(s/valid? ::numberT (type "LOL") (type 42))

(isa? (type "LOL") (type 42))

;; in my my case, int? should be replaced by 
(s/conform (s/and int? pos?) 3)


(or (instance? Long Number) (instance? Number Long))


(s/def ::number number?)
(s/def ::string string?)

(s/valid? ::numberT String)

(s/def ::typeCheck #(let [type (:type %)
                          anno (:anno %)]
                      (or (isa? type anno)
                          (isa? anno type))))

(def typ-symb :-)
;; macro-expansion compile time check
(defmacro ta-def
  [name & body]
  (let [type-symb (if (= typ-symb (first body))
                    typ-symb
                    (throw (IllegalArgumentException. (str "Wrong type annotation symbol."))))
        type-anno  (resolve (second body))
        value      (last body)
        value-type (type value)
        type-map   {:type value-type
                    :anno type-anno}]
    (if (s/valid? ::typeCheck type-map)
      `(def ~name ~value)
      (throw (IllegalArgumentException. (str "The given value type  " value-type " do not match the type annotation " type-anno))))))


(ta-def myvar :- Long 42)

(ta-def myvar2 :- String 42)

;; name should be `myvar` [OK]
;; body should be `:- Number 42` [OK]
;; type-symb should be `:-` [OK]
;; type-anno should be `Number` [OK]
;; value should be `42` [OK]
;; value-type should `java.lang.Long` [OK]
;; then it should check if `type-anno` equals to `value-type` [] 
;; OR check if `type-anno` contains `value-type` or vice versa
;; for example, Number type majors(contains) Long type
;; that means that Number annotation should work for 42
;; because it is a Long and consequently a Number

(ta-def myvar2 :- String 42)





(defn validate-type-annotation [value type-annotation]
  (let [actual-type (class value)]
    (= type-annotation actual-type)))

(println (supers (class 42)))

(def my-var 42)

(if (s/valid? ::numberT my-var)
  (println "my-var has the correct type annotation!")
  (println "my-var does not have the correct type annotation!"))


;(s/valid? ::numberT my-var)

;(s/conform (s/and (s/valid? ::number value) (s/valid? ::numberT type-anno)))

;(s/valid? ::number 42)

;(s/conform ::numberT Number)

;; (s/conform (s/and int? pos?) 3)

(defmacro def-anno
  [name _type & body]
  (let [type (first body)
        value (second body)]
    (if (s/valid? ::number value)
      `(defn ~name []
         (let [result# (do ~@body)]
           (if (number? result#)
             result#
             (throw (IllegalArgumentException. (str "Function result does not match specified type: " result# " is not of type Number"))))))
      (if (s/valid? type ::string)
        `(defn ~name []
           (let [result# (do ~@body)]
             (if (string? result#)
               result#
               (throw (IllegalArgumentException. (str "Function result does not match specified type: " result# " is not of type String"))))))
        (throw (IllegalArgumentException. (str "Unsupported type annotation: " type)))))))


;;(e/emit-form (ana.jvm/analyze '(+ 2 1)))


;;(ana.jvm/analyze-ns 'natto.core)

;;(ana.jvm/analyze+eval '(defmacro x []))

;;(ana.jvm/analyze+eval '(x))
