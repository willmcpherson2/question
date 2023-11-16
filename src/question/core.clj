(ns question.core)

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

(defmacro ?
  "Takes an argument and a set of pattern/body pairs.

  A pattern can be any of the following:

  - The symbol _, which just returns the body.
  - A symbol, which is bound to the argument in the body.
  - A seqable, where each element will be pattern matched with the
  corresponding elements in the argument.
  - The symbol & within a seqable, which must be followed by a single
  pattern which will be pattern matched with the rest of the sequence.

  Any other pattern will be tested for equality with the argument. If
  false, the next pattern is tested. If no patterns match, nil is
  returned."
  [arg & clauses]
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
