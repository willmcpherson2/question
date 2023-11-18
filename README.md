# question

Question is a pattern matching library for Clojure.

The primary macro is `?`.

```
question.core/?
([arg & clauses])
Macro
  Takes an argument and a set of pattern/body pairs.

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

  Examples: https://github.com/willmcpherson2/question/blob/main/README.md#examples
```

## Examples

```clojure
(ns examples.core
  (:require [question.core :refer [? _ & any]]))

;; No patterns, so always nil
(? 1)
nil

;; Argument is ignored, so always :something
(? 1
   _ :something)
:something

;; (= 1 1), so :one
(? 1
   1 :one)
:one

;; First branch fails, second succeeds
(? 2
   1 :one
   2 :two)
:two

(? [1 2]
   (list 1 2) :list-1-2 ;; Sequence types must match
   [1] :vec-1           ;; Every element must be present
   [1 3] :vec-1-3       ;; Every element must be equal
   [1 2 3] :vec-1-2-3   ;; No excess elements
   [1 2] :vec-1-2)
:vec-1-2

;; Use rest syntax if length doesn't matter
(? [1 2 3]
   [1 & _] :starts-1)
:starts-1

;; The Any type matches any seqable
(? [1 2 3]
   (any 1 2 3) :seqable)
:seqable

;; Symbols are bound in the body
(? [1 2 3]
   ['x 'y 3] (+ x y))
3

;; Quoting the whole pattern can be easier
(? [1 2 3]
   '[x y 3] (+ x y))
3

;; Syntax-quote works too
(? [1 2 3]
   `[x y 3] (+ x y))
3

;; Splitting a sequence
(? [1 2 3]
   '[x & xs] {:first x, :rest xs})
{:first 1, :rest '(2 3)}

;; Patterns are evaluated at compile-time
(? [1 2 3]
   [1 2 (+ 1 2)] :ok)
:ok

;; def variables are available at compile-time
(def three 3)
(? [1 2 3]
   [1 2 three] :ok)
:ok

;; You can even apply functions to patterns
(? '(1 2 3)
   (reverse '(x 2 1)) x)
3

;; If a pattern fails, its body will not be evaluated
(? 2
   1 (throw (Exception. "evaluated!"))
   2 :ok
   3 (throw (Exception. "evaluated!")))
:ok
```
