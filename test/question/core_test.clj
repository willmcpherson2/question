(ns question.core-test
  (:require [clojure.test :refer [deftest is]]
            [question.core :refer [? _ & any]]))

(def three 3)

(deftest question-test
  ;; No patterns, so always nil
  (is (= (? 1)
         nil))

  ;; Argument is ignored, so always :something
  (is (= (? 1
            _ :something)
         :something))

  ;; (= 1 1), so :one
  (is (= (? 1
            1 :one)
         :one))

  ;; First branch fails, second succeeds
  (is (= (? 2
            1 :one
            2 :two)
         :two))

  ;; Sequence types must match
  (is (= (? []
            () :yes)
         nil))

  ;; Each element must match
  (is (= (? [1 2 :nope]
            [1 2 3] :123)
         nil))

  ;; Every element must be present
  (is (= (? [1 2]
            [1 2 3] :123)
         nil))

  ;; Excess elements are allowed
  (is (= (? [1 2 3 4]
            [1 2 3] :123)
         :123))

  ;; Use rest syntax to match end of sequence
  (is (= (? [1 2 3 4]
            [1 2 3 & nil] :123)
         nil))

  ;; Ignoring the elements and checking the sequence type
  (is (= (? [1 2 3]
            [& _] :vector)
         :vector))

  ;; The Any type matches any seqable
  (is (= (? [1 2 3]
            (any 1 2 3) :seqable)
         :seqable))

  ;; Symbols are bound in the body
  (is (= (? [1 2 3]
            ['x 'y 3] (+ x y))
         3))

  ;; Quoting the whole pattern can be easier
  (is (= (? [1 2 3]
            '[x y 3] (+ x y))
         3))

  ;; Syntax-quote works too
  (is (= (? [1 2 3]
            `[x y 3] (+ x y))
         3))

  ;; Splitting a sequence
  (is (= (? [1 2 3]
            '[x & xs] {:first x, :rest xs})
         {:first 1, :rest '(2 3)}))

  ;; Patterns are evaluated at compile-time
  (is (= (? [1 2 3]
            [1 2 (+ 1 2)] :ok)
         :ok))

  ;; def variables are available at compile-time
  (is (= (? [1 2 3]
            [1 2 three] :ok)
         :ok))

  ;; You can even apply functions to patterns
  (is (= (? '(1 2 3)
            (reverse '(x 2 1)) x)
         3))

  ;; If a pattern fails, its body will not be evaluated
  (is (= (? 2
            1 (throw (Exception. "evaluated!"))
            2 :ok
            3 (throw (Exception. "evaluated!")))
         :ok)))
