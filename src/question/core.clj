(ns question.core
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.walk :refer [macroexpand-all]]))

(defn dbg [x]
  (println x)
  x)

(declare ?)

(defmacro ?seq [args pats body]
  (if-let [[pat & pats] pats]
    (if (= pat '&)
      (if-let [[pat & pats] pats]
        (if (= pats nil)
          `(? ~args ~pat ~body)
          (throw (IllegalArgumentException. "too many arguments after &")))
        (throw (IllegalArgumentException. "missing argument after &")))
      (let [a (gensym)
            as (gensym)]
        `(if-let [[~a & ~as] ~args]
           (? ~a ~pat (?seq ~as ~pats ~body)))))
    body))

(defmacro ? [arg pat body]
  (cond
    (= pat '_) body
    (symbol? pat) `(let [~pat ~arg] ~body)
    (seqable? pat) `(if (= (type ~arg) ~(type pat))
                      (?seq ~arg ~pat ~body))
    :else `(if (= ~arg ~pat)
             ~body)))

(defn main []
  (let [e '(? [1 2 3]
              [_ & xs] xs)]
    (pprint e)
    (pprint (eval e))
    (pprint (macroexpand-all e))))
