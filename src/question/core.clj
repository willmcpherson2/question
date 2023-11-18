(ns question.core)

(deftype Any [coll]
  clojure.lang.IPersistentList
  (seq [this] (seq coll)))

(defn any
  "Same as list, but creates an Any, which is just a wrapper of
  IPersistentList. As a pattern, matches any seqable and will compare
  elements only."
  [& items]
  (Any. (apply list items)))

(def _
  "The symbol _, which is a wildcard in patterns."
  '_)

(def &
  "The symbol &, which is a rest argument in patterns."
  '&)

(declare ?branch)

(defmacro ?seq
  "Single branch of ?, taking sequences for the argument and
  pattern. Only the elements will be pattern matched, so the sequence
  types don't matter."
  [args pats body else]
  (if (seq pats)
    (let* [pat (first pats)]
          (if (= pat '&)
            (let* [pats (rest pats)]
                  (if (seq pats)
                    (let* [pat (first pats)]
                          (if (seq (rest pats))
                            (throw (IllegalArgumentException. "too many arguments after &"))
                            `(?branch ~args ~pat ~body ~else)))
                    (throw (IllegalArgumentException. "missing argument after &"))))
            (let* [a (gensym "arg")
                   as (gensym "args")]
                  `(let* [~a (first ~args)
                          ~as (rest ~args)]
                         (?branch ~a ~pat (?seq ~as ~(rest pats) ~body ~else) ~else)))))
    `(if (seq ~args)
       ~else
       ~body)))

(defmacro ?branch
  "Single branch of ?."
  [arg pat body else]
  (if (symbol? pat)
    (if (= pat '_)
      body
      `(let* [~(symbol (name pat)) ~arg] ~body))
    (if (seqable? pat)
      (let* [a (gensym "arg")
             as (gensym "args")]
            `(let* [~a ~arg]
                   ~(if (= (type pat) Any)
                      `(let* [~as (seq ~a)]
                             (?seq ~as ~pat ~body ~else))
                      `(if (= (type ~a) ~(type pat))
                         (let* [~as (seq ~a)]
                               (?seq ~as ~pat ~body ~else))
                         ~else))))
      `(if (= ~arg ~pat)
         ~body
         ~else))))

(defmacro ?
  "Takes an argument and a set of pattern/body pairs.

  A pattern can be any of the following:

  - The symbol _, which just returns the body.
  - A symbol, which is bound to the argument in the body.
  - A seqable, where each element will be pattern matched with the
  corresponding elements in the argument. The seqable types must
  match, unless the pattern has type Any.
  - The symbol & within a seqable, which must be followed by a single
  pattern which will be pattern matched with the rest of the sequence.

  Any other pattern will be tested for equality with the argument. If
  false, the next pattern is tested. If no patterns match, nil is
  returned.

  Patterns are evaluated at compile-time.

  Examples: https://github.com/willmcpherson2/question/blob/main/README.md#examples"
  [arg & clauses]
  (if (seq clauses)
    (let* [pat (first clauses)
           clauses (rest clauses)]
          (if (seq clauses)
            (let* [body (first clauses)
                   clauses (rest clauses)]
                  `(?branch ~arg ~(eval pat) ~body (? ~arg ~@clauses)))
            (throw (IllegalArgumentException. "expected body after pattern"))))
    nil))
