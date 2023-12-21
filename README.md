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

## Comparison with [`core.match`](https://github.com/clojure/core.match)

This section compares `match` from `core.match` with `?` from `question`.

### No matching clause

If no clause matches the argument, `match` will throw an `IllegalArgumentException`. `?` will return `nil`.

### `:else`

`match` uses wildcards `_` and `:else`. `?` just has wildcards.

### Binding

`match` allows binding with `x`. `?` uses the quoted form `'x`.

### Evaluation in patterns

`?` will evaluate patterns at compile time. `x` is evaluated which is why you need `'x` to bind.

### Locals

`match` will actually resolve `x` if it's defined locally. Otherwise, it binds. In this example, `String` is a bind, so it always matches:

```clojure
(match (type 1)
       String :string
       Long :long) ;=> :string
```

But here, `string` is resolved to the value `java.lang.String`, so it doesn't match:

```clojure
(let [string String
      long Long]
  (match (type 1)
         string :string
         long :long)) ;=> :long
```

But if the definition is not local, it's still a bind:

```clojure
(def string String)
(def long Long)
(match (type 1)
       string :string
       long :long) ;=> :string
```

This can be confusing if you intended to bind but accidentally used something in scope.

`?` will simply always evaluate unquoted symbols:

```clojure
(? (type 1)
   String :string
   Long :long) ;=> :long
```

Which will result in an `UnsupportedOperationException` if you try to use a local variable:

```clojure
(let [string String
      long Long]
  (? (type 1)
     string :string
     long :long))
;; Unexpected error (UnsupportedOperationException) macroexpanding ?
;; Can't eval locals
```

This is because locals exist at run time and can't be evaluated at compile time.

https://github.com/clojure/core.match/wiki/Basic-usage#locals

### Variable shadowing

Variable shadowing doesn't really work with `match` since locals will be resolved:

```clojure
(let [x 1]
  (match 3
         0 "zero"
         x (str x)))
;; Execution error (IllegalArgumentException)
;; No matching clause: 3
```

Variable shadowing works as expected with `?`:

```clojure
(let [x 1]
  (? 3
     0 "zero"
     'x (str x))) ;=> "3"
```

https://clojure.atlassian.net/browse/MATCH-126

### Qualified names

`?` allows class names and qualified names.

```clojure
(? (type 1)
   java.lang.Short :short
   java.lang.Long :long) ;=> :long

(? 2147483647
   Short/MAX_VALUE :short
   Integer/MAX_VALUE :long) ;=> :long
```

https://clojure.atlassian.net/browse/MATCH-130

### Vector arguments

`match` will treat a vector as multiple arguments. For example, these are fine:

```clojure
(match [1]
       [1] :vector) ;=> :vector

(match {:a 1}
       {:a 1} :map) ;=> :map
```

But this is a syntax error:

```clojure
(match [1]
       [1] :vector
       {:a 1} :map)
;; Unexpected error (AssertionError) macroexpanding match
;; Pattern row 2: Pattern rows must be wrapped in []. Try changing {:a 1} to [{:a 1}].
```

But if the vector is in a variable, it's valid:

```clojure
(let [x [1]]
  (match x
       [1] :vector
       {:a 1} :map)) ;=> vector
```

`?` has no special case for vectors:

```clojure
(? [1]
   [1] :vector
   {:a 1} :map) ;=> vector
```

### Lists

To match a list using `match`, you need `:seq` combined with `:guard`:

```clojure
(match (list 1 2 3)
  [1 2 3] :vector
  (([1 2 3] :seq) :guard #(list? %)) :list) ;=> :list
```

With `?`, a list is a valid pattern:

```clojure
(? (list 1 2 3)
   [1 2 3] :vector
   (list 1 2 3) :list) ;=> :list
```

Quote syntax is also valid:

```clojure
(? (list 1 2 3)
   [1 2 3] :vector
   '(1 2 3) :list) ;=> :list
```

https://github.com/clojure/core.match/wiki/Basic-usage#sequential-types

https://github.com/clojure/core.match/wiki/Basic-usage#guards

https://clojure.atlassian.net/browse/MATCH-103

### Maps

`match` treats map patterns as subsets (unless the `:only` modifier is present):

```clojure
(match {:a 1, :b 2}
       {:a 1} :ok) ;=> :ok
```

`?` doesn't give maps special treatment:

```clojure
(? {:a 1, :b 2}
   {:a 1} :ok) ;=> nil
```

https://github.com/clojure/core.match/wiki/Basic-usage#map-patterns

### Map keys

`match` can't handle certain key types, including: `Boolean`, `Long`, `PersistentVector`, `Symbol`.

```clojure
(match {1 :b}
       {1 :b} :ok)
;; Unexpected error (ClassCastException) macroexpanding match
;; class java.lang.Long cannot be cast to class clojure.lang.Named
```

https://stackoverflow.com/questions/72150186/clojure-core-match-on-nested-map

https://clojure.atlassian.net/browse/MATCH-107

### Pattern abstraction

Because patterns are evaluated, `?` lets you abstract over patterns:

```clojure
(defn pair [x]
  [x x])

(? [1 1]
   (pair 1) :ok) ;=> :ok
```

`match` can be extended by other means: [Advanced usage](https://github.com/clojure/core.match/wiki/Advanced-usage)

### Optimisations

`match` implements some advanced optimisations, whereas `?` is more basic.

https://github.com/clojure/core.match/wiki/Understanding-the-algorithm

### Features

`match` has additional features like `:or`.

https://github.com/clojure/core.match/wiki/Basic-usage#or-patterns
