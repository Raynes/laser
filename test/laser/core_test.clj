(ns laser.core-test
  (:require [clojure.test :refer :all]
            [me.raynes.laser :as l]
            [clojure.zip :as zip]))

;; Selectors

(def html [{:type :element, :attrs {:class "a b c", :id "hi"}, :tag :p, :content ["hi"]}])

(deftest element=-test
  (is (true? ((l/element= :p) html)))
  (is (false? ((l/element= :pre) html))))

(deftest attr=-test
  (is (true? ((l/attr= :class "a b c") html)))
  (is (false? ((l/attr= :foo "bar") html))))

(deftest attr?-test
  (is (true? ((l/attr? :class) html)))
  (is (false? ((l/attr? :foo) html))))

(deftest class=-test
  (is (false? ((l/class= "a") html)))
  (is (true? ((l/class= "a" "b" "c") html)))
  (is (false? ((l/class= "d") html))))

(deftest id=-test
  (is (true? ((l/id= "hi") html)))
  (is (false? ((l/id= "bye") html))))

(deftest any-test
  (is (true? ((l/any) html))))

;; Combinators

(deftest negate-test
  (is (false? ((l/negate (l/element= :p)) html))
      (true? ((l/negate (l/element= :pre)) html))))

(deftest select-and-test
  (is (true? ((l/select-and (l/element= :p) (l/attr? :class)) html)))
  (is (false? ((l/select-and (l/element= :pre) (l/attr? :class)) html))))

(deftest select-or-test
  (is (true? ((l/select-or (l/element= :p) (l/attr? :class)) html)))
  (is (true? ((l/select-or (l/element= :pre) (l/attr? :class)) html)))
  (is (false? ((l/select-or (l/element= :pre) (l/attr? :foo)) html))))
