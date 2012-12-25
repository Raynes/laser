(ns me.raynes.laser-test
  (:require [clojure.test :refer :all]
            [me.raynes.laser :as l]
            [clojure.zip :as zip]
            [hickory.core :as hickory]
            [hickory.zip :as hzip]))

;; Selectors

(def html (-> "<div class=\"a b c\" id=\"hi\"><p><a>hi</a><b>foo</b></p></div>"
              hickory/parse-fragment
              first
              hickory/as-hickory
              hzip/hickory-zip))

(def node {:type :element
           :tag :a
           :attrs nil
           :content nil})

(deftest element=-test
  (is (true? ((l/element= :div) html)))
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
  (is (false? ((l/negate (l/element= :div)) html))
      (true? ((l/negate (l/element= :pre)) html))))

(deftest select-and-test
  (is (true? ((l/select-and (l/element= :div) (l/attr? :class)) html)))
  (is (false? ((l/select-and (l/element= :pre) (l/attr? :class)) html))))

(deftest select-or-test
  (is (true? ((l/select-or (l/element= :div) (l/attr? :class)) html)))
  (is (true? ((l/select-or (l/element= :pre) (l/attr? :class)) html)))
  (is (false? ((l/select-or (l/element= :pre) (l/attr? :foo)) html))))

(deftest descendant-of-test
  (is (true? ((l/descendant-of (l/element= :div) (l/element= :a))
              (zip/next (zip/next html)))))
  (is (false? ((l/descendant-of (l/element= :div) (l/element= :pre))
               (zip/next (zip/next html))))))

(deftest child-of-test
  (is (true? ((l/child-of (l/element= :div) (l/element= :p)) (zip/next html))))
  (is (false? ((l/child-of (l/element= :div) (l/element= :a)) (zip/next (zip/next html))))))

(deftest ajacent-to-test
  (is (true? ((l/ajacent-to (l/element= :b) (l/element= :a))
              (-> html zip/next zip/next zip/next zip/next)))
      (false? ((l/ajacent-to (l/element= :b) (l/element= :div))
               (-> html zip/next zip/next zip/next zip/next)))))

;; Transformers

(deftest content-test
  (is (= (assoc node :content ["hi"]) ((l/content "hi") node)))
  (testing "Gets escaped in the end."
    (is (= "<a>h&amp;i</a>"
           (hickory/hickory-to-html ((l/content "h&i") node))))))

(deftest html-content-test
  (is (= (assoc node :content [node]) ((l/html-content "<a></a>") node))))

(deftest attr-test
  (is (= (assoc node :attrs {:class "a"}) ((l/attr :class "a") node))))

(deftest classes-test
  (is (= (assoc node :attrs {:class "a b"}) ((l/classes "a b") node))))

(deftest id-test
  (is (= (assoc node :attrs {:id "hi"}) ((l/id "hi") node))))

(deftest add-class-test
  (is (= (assoc node :attrs {:class "a b"})
         ((l/add-class "b") (assoc node :attrs {:class "a"})))))

(deftest remove-class-test
  (is (= (assoc node :attrs {:class "a"})
         ((l/remove-class "b") (assoc node :attrs {:class "a b"})))))

(deftest wrap
  (is (= {:type :element :tag :div :attrs {:class "hi"} :content [node]}
         ((l/wrap :div {:class "hi"}) node))))

(deftest remove-test
  (is (= nil ((l/remove) node))))

;; Fragments and Documents

(defn element [tag]
  {:type :element
   :attrs nil
   :tag tag
   :content nil})

(deftest parse-fragment-test
  (is (= '([{:type :element, :attrs nil, :tag :a, :content nil} nil]
             [{:type :element, :attrs nil, :tag :a, :content nil} nil])
         (l/parse-fragment "<a></a><a></a>"))))

(deftest fragment-test
  (is (= (repeat 2 (element :a))
         (l/fragment (l/parse-fragment "<a></a><a></a>")
                     (l/element= :a) (fn [node] node))))
  (testing "top-level added nodes are handled"
    (is (= [(element :span) (element :a)
            (element :span) (element :a)]
           (l/fragment
            (l/parse-fragment "<a></a><a></a>")
            (l/element= :a) (fn [node] [(element :span) node]))))))

(deftest nil-remove-test
  (is (= "" (l/fragment-to-html
             (l/fragment (l/parse-fragment "<a></a>")
                         (l/any) (constantly nil))))))

(deftest document-test
  (is (= "<html><head></head><body><a>hi</a></body></html>"
         (l/document
          (l/parse "<a></a>")
          (l/element= :a) (l/content "hi")))))