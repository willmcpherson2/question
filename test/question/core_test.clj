(ns question.core-test
  (:require [clojure.test :refer :all]
            [question.core :refer :all]))

(deftest question-test
  (is (= (? 1)
         nil))

  (is (= (? 1
            _ :something)
         :something))

  (is (= (? 1
            1 :one)
         :one))

  (is (= (? 2
            1 :one
            2 :two)
         :two))

  (is (= (? []
            () :yes)
         nil))

  (is (= (? [1 2 :nope]
            [1 2 3] :123)
         nil))

  (is (= (? [1 2]
            [1 2 3] :123)
         nil))

  (is (= (? [1 2 3 4]
            [1 2 3] :123)
         :123))

  (is (= (? [1 2 3 4]
            [1 2 3 & nil] :123)
         nil))

  (is (= (? [1 2 3]
            [x & xs] {:first x, :rest xs})
         {:first 1, :rest '(2 3)}))

  (is (= (? '(1 2 3)
            [& _] :vector
            (& _) :list)
         :list))

  (is (= (? 2
            1 (throw (Exception. "evaluated!"))
            2 :ok
            3 (throw (Exception. "evaluated!")))
         :ok)))
