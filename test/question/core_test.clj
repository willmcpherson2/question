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

  (is (= (? [1 2]
            (list 1 2) :list-1-2 ;; Sequence types must match
            [1] :vec-1           ;; Every element must be present
            [1 3] :vec-1-3       ;; Every element must be equal
            [1 2 3] :vec-1-2-3   ;; No excess elements
            [1 2] :vec-1-2)
         :vec-1-2))

  ;; Use rest syntax if length doesn't matter
  (is (= (? [1 2 3]
            [1 & _] :starts-1)
         :starts-1))

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
         :ok))

  (is (= (? []
            1 :one
            (any) :seqable)
         :seqable))

  (is (= (? [1 '(2 3 4) 5]
            (vector 'x (list 2 'y 4) 'z) (+ x y z))
         9))

  (is (= (? :a
            (any :a) :seqable-a
            :a :just-a)
         :just-a))

  (is (= (? []
            ['x] [:one x]
            [] :empty)
         :empty))

  (is (= (? [1]
            [1 'x] [:one x]
            _ :ok)
         :ok))

  (is (= (? (lazy-seq (cons :a (lazy-seq (cons :b (lazy-seq (cons (throw (Exception. "evaluated!")) nil))))))
            (lazy-seq (cons :x (lazy-seq (cons :y (lazy-seq (cons :z nil)))))) :ok)
         nil))

  ;; Throws an exception. Can we be more lazy?
  #_(is (= (? (lazy-seq (cons :a (lazy-seq (cons (throw (Exception. "evaluated!")) nil))))
              (lazy-seq (cons :x (lazy-seq (cons :y nil)))) :ok)
           nil)))
