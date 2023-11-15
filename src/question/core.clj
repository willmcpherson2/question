(ns question.core
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.walk :refer [macroexpand-all]]))

(defn dbg [x]
  (println x)
  x)

(declare ?)

(defmacro ?seq [args pats body]
  (if pats
    (let* [pat (first pats)]
          (if (= pat '&)
            (let* [pats (next pats)]
                  (if pats
                    (let* [pat (first pats)]
                          (if (next pats)
                            (throw (IllegalArgumentException. "too many arguments after &"))
                            `(? ~args ~pat ~body)))
                    (throw (IllegalArgumentException. "missing argument after &"))))
            (let* [a (gensym "arg")
                   as (gensym "args")]
                  `(let* [~a (first ~args)
                          ~as (next ~args)]
                         (? ~a ~pat (?seq ~as ~(next pats) ~body))))))
    body))

(defmacro ? [arg pat body]
  (if (symbol? pat)
    (if (= pat '_)
      body
      `(let* [~pat ~arg] ~body))
    (if (seqable? pat)
      (let* [a (gensym "arg")
             as (gensym "args")]
            `(let* [~a ~arg]
                   (if (= (type ~a) ~(type pat))
                     (let* [~as (seq ~a)]
                           (?seq ~as ~pat ~body)))))
      `(if (= ~arg ~pat)
         ~body))))

(defn main []
  (let [e '(? [1 2 3 4]
              [1 _ & xs] xs)]
    (pprint e)
    (pprint (eval e))
    (pprint (macroexpand-all e))))
