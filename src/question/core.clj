(ns question.core
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.walk :refer [macroexpand-all]]))

(defn dbg [x]
  (println x)
  x)

(declare ?branch)

(defmacro ?seq [args pats body else]
  (if pats
    (let* [pat (first pats)]
          (if (= pat '&)
            (let* [pats (next pats)]
                  (if pats
                    (let* [pat (first pats)]
                          (if (next pats)
                            (throw (IllegalArgumentException. "too many arguments after &"))
                            `(?branch ~args ~pat ~body ~else)))
                    (throw (IllegalArgumentException. "missing argument after &"))))
            (let* [a (gensym "arg")
                   as (gensym "args")]
                  `(let* [~a (first ~args)
                          ~as (next ~args)]
                         (?branch ~a ~pat (?seq ~as ~(next pats) ~body ~else) ~else)))))
    body))

(defmacro ?branch [arg pat body else]
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
                           (?seq ~as ~pat ~body ~else))
                     ~else)))
      `(if (= ~arg ~pat)
         ~body
         ~else))))

(defmacro ? [arg & clauses]
  (let* [clauses (seq clauses)]
        (if clauses
          (let* [pat (first clauses)
                 clauses (next clauses)]
                (if clauses
                  (let* [body (first clauses)
                         clauses (next clauses)]
                        `(?branch ~arg ~pat ~body (? ~arg ~@clauses)))
                  (throw (IllegalArgumentException. "expected body after pattern"))))
          nil)))

(defn main []
  (let [e '(? [:add 1 2]
              [:mul x y] (* x y)
              [:add x y] (+ x y))]
    (pprint e)
    (pprint (eval e))
    (pprint (macroexpand-all e))))
