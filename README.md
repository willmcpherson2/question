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
  match.
  - The symbol & within a seqable, which must be followed by a single
  pattern which will be pattern matched with the rest of the sequence.

  Any other pattern will be tested for equality with the argument. If
  false, the next pattern is tested. If no patterns match, nil is
  returned.
```

## Examples

```clojure
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

;; Sequence types must match
(? []
   () :yes)
nil

;; Each element must match
(? [1 2 :nope]
   [1 2 3] :123)
nil

;; Every element must be present
(? [1 2]
   [1 2 3] :123)
nil

;; Excess elements are allowed
(? [1 2 3 4]
   [1 2 3] :123)
:123

;; Use rest syntax to match end of sequence
(? [1 2 3]
   [1 2 3 & nil] :exactly-123)
:exactly-123

;; Splitting a sequence
(? [1 2 3]
   [x & xs] {:first x, :rest xs})
{:first 1, :rest (2 3)}

;; Checking the sequence type only
(? '(1 2 3)
   [& _] :vector
   (& _) :list)
:list

;; If a pattern fails, its body will not be evaluated
(? 2
   1 (throw (Exception. "evaluated!"))
   2 :ok
   3 (throw (Exception. "evaluated!")))
:ok
```
