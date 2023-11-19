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
  "Alias for symbol _, which is a wildcard in patterns."
  '_)

(def &
  "Alias for symbol &, which is a rest argument in patterns."
  '&)

(declare ?branch)

(defn- ?seq
  "Single branch of ?, taking sequences for the argument and
  pattern. Only the elements will be pattern matched, so the sequence
  types don't matter."
  [args pats body else]
  (if (seq pats)
    (let [pat (first pats)]
      (if (= pat '&)
        (let [pats (rest pats)]
          (if (seq pats)
            (let [pat (first pats)]
              (if (seq (rest pats))
                (throw (IllegalArgumentException. "too many arguments after &"))
                (?branch args pat body else)))
            (throw (IllegalArgumentException. "missing argument after &"))))
        (let [arg-sym (gensym "arg")
              args-sym (gensym "args")]
          `(let [~arg-sym (first ~args)
                 ~args-sym (rest ~args)]
             ~(?branch arg-sym pat (?seq args-sym (rest pats) body else) else)))))
    `(if (seq ~args)
       ~else
       ~body)))

(defn- ?branch
  "Single branch of ?."
  [arg pat body else]
  (if (symbol? pat)
    (if (= pat '_)
      body
      `(let [~(symbol (name pat)) ~arg] ~body))
    (if (seqable? pat)
      (let [arg-sym (gensym "arg")
            args-sym (gensym "args")]
        `(let [~arg-sym ~arg]
           ~(if (= (type pat) Any)
              `(let [~args-sym (seq ~arg-sym)]
                 ~(?seq args-sym pat body else))
              `(if (= (type ~arg-sym) ~(type pat))
                 (let [~args-sym (seq ~arg-sym)]
                   ~(?seq args-sym pat body else))
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
  (let [num-clauses (count clauses)]
    (if (even? num-clauses)
      (if (> num-clauses 0)
        (let [arg-sym (gensym "arg")
              names (repeatedly (/ num-clauses 2) (partial gensym "branch"))
              elses (concat (rest names) [nil])
              branches (->> clauses
                            (partition 2)
                            (map vector names)
                            (map vector elses)
                            (map (fn [[else [name [pat body]]]]
                                   [name `(fn []
                                            ~(?branch arg-sym
                                                      (eval pat)
                                                      body
                                                      (if else `(~else) nil)))]))
                            reverse
                            (apply concat)
                            vec)
              entry (first names)]
          `(let [~arg-sym ~arg ~@branches] (~entry)))
        nil)
      (throw (IllegalArgumentException. "? requires even number of forms")))))
