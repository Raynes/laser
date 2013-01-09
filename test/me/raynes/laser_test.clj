(ns me.raynes.laser-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [me.raynes.laser :as l]
            [clojure.zip :as zip]
            [hickory.core :as hickory]
            [hickory.zip :as hzip]))

(defn nexts [n html]
  (reduce #(%2 %) html (repeat n zip/next)))

;; Selectors

(def html (-> "<div class=\"a b c\" id=\"hi\"><p><a>hi</a><b>foo</b><c><span id=\"deep\"></span></c></p></div>"
              hickory/parse-fragment
              first
              hickory/as-hickory
              hzip/hickory-zip))

(def node {:type :element
           :tag :a
           :attrs nil
           :content nil})

(facts "about element="
  ((l/element= :div) html) => true?
  ((l/element= :pre) html) => false?)

(facts "about attr="
  ((l/attr= :class "a b c") html) => true?
  ((l/attr= :foo "bar") html) => false?)

(facts "about attr?"
  ((l/attr? :class) html) => true?
  ((l/attr? :foo) html) => false?)

(facts "about re-attr"
  ((l/re-attr :href #"^/foo") (hzip/hickory-zip (l/node :a :attrs {:href "/foo/bar"}))) => truthy
  ((l/re-attr :href #"^/foo") (hzip/hickory-zip (l/node :a :attrs {:href "/bar/foo"}))) => falsey)

(facts "about class="
  ((l/class= "a") html) => true?
  ((l/class= "a" "b" "c") html) => true?
  ((l/class= "d") html) => false?)

(facts "about id="
  ((l/id= "hi") html) => true?
  ((l/id= "bye") html) => false?)

(fact "about any"
  ((l/any) html) => true?)

;; Combinators

(facts "about negate"
  ((l/negate (l/element= :div)) html) => false?
  ((l/negate (l/element= :pre)) html) => true?)

(facts "about and"
  ((l/and (l/element= :div) (l/attr? :class)) html) => true?
  ((l/and (l/element= :pre) (l/attr? :class)) html) => false?)

(facts "about or"
  ((l/or (l/element= :div) (l/attr? :class)) html) => true?
  ((l/or (l/element= :pre) (l/attr? :class)) html) => true?
  ((l/or (l/element= :pre) (l/attr? :foo)) html) => false?)

(facts "about decendant-of"
  (let [html (nexts 2 html)]
    ((l/descendant-of (l/element= :div)
                      (l/element= :a))
     html) => true?
    ((l/descendant-of (l/element= :div)
                      (l/element= :pre))
     html) => false?
    ((l/descendant-of (l/element= :div)
                      (l/element= :p)
                      (l/element= :a))
     html) => true?
    ((l/descendant-of (l/element= :div)
                      (l/element= :p)
                      (l/id= "deep"))
     (nexts 5 html)) => true?
    ((l/descendant-of (l/element= :div)
                      (l/element= :p)
                      (l/element= :random)
                      (l/id= "deep"))
     (nexts 5 html)) => false?))

(facts "about child-of"
  ((l/child-of (l/element= :div) (l/element= :p)) (nexts 1 html)) => true?
  ((l/child-of (l/element= :div) (l/element= :a)) (zip/next (zip/next html))) => false?
  ((l/child-of (l/element= :div) (l/element= :p) (l/id= "deep")) (nexts 7 html)) => false?
  ((l/child-of (l/element= :div) (l/element= :p) (l/element= :c) (l/id= "deep"))
   (nexts 7 html)) => true?)

(facts "about adjacent-to"
  ((l/adjacent-to (l/element= :a) (l/element= :b)) (nexts 4 html)) => true?
  ((l/adjacent-to (l/element= :b) (l/element= :div)) (nexts 4 html)) => false?
  ((l/adjacent-to (l/element= :a) (l/element= :b) (l/element= :c)) (nexts 6 html)) => true?
  ((l/adjacent-to (l/element= :a) (l/element= :e) (l/element= :c)) (nexts 6 html)) => false?)

;; Transformers

(facts "about content"
  ((l/content "hi") node) => (assoc node :content ["hi"])
  (hickory/hickory-to-html ((l/content "h&i") node)) => "<a>h&amp;i</a>")

(fact "about html-content"
  ((l/html-content "<a></a>") node) => (assoc node :content [node]))

(fact "about attr"
  ((l/attr :class "a") node) => (assoc node :attrs {:class "a"}))

(fact "about classes"
  ((l/classes "a b") node) => (assoc node :attrs {:class "a b"}))

(fact "about id"
  ((l/id "hi") node) => (assoc node :attrs {:id "hi"}))

(fact "about add-class"
  ((l/add-class "b") (assoc node :attrs {:class "a"})) => (assoc node :attrs {:class "a b"}))

(fact "about remove-class"
  ((l/remove-class "b") (assoc node :attrs {:class "a b"})) => (assoc node :attrs {:class "a"}))

(fact "about wrap"
  ((l/wrap :div {:class "hi"}) node) => {:type :element
                                         :tag :div
                                         :attrs {:class "hi"}
                                         :content [node]})

(fact "about remove"
  ((l/remove) node) => nil?)

(fact "about replace"
 ((l/replace {:foo :a}) {:foo :bar}) => {:foo :a})

;; Fragments and Documents

(defn element [tag]
  {:type :element
   :attrs nil
   :tag tag
   :content nil})

(fact "about parse-fragment"
  (l/parse-fragment "<a></a><a></a>") => '([{:type :element,
                                             :attrs nil,
                                             :tag :a,
                                             :content nil} nil]
                                             [{:type :element,
                                               :attrs nil,
                                               :tag :a,
                                               :content nil} nil]))

(facts "about fragment"
  (l/fragment (l/parse-fragment "<a></a><a></a>")
              (l/element= :a) (fn [node] node)) => (repeat 2 (element :a))
  (l/fragment
   (l/parse-fragment "<a></a><a></a>")
   (l/element= :a) (fn [node] [(element :span) node])) => [(element :span) (element :a)
                                                           (element :span) (element :a)])

(fact "nils are treated as removals and replaced as empty strings."
  (l/fragment-to-html
             (l/fragment (l/parse-fragment "<a></a>")
                         (l/any) (constantly nil))) => "")

(fact "about document"
  (l/document
   (l/parse "<a></a>")
   (l/element= :a) (l/content "hi")) => "<html><head></head><body><a>hi</a></body></html>")

(fact "about at"
  (l/to-html (l/at {:type :element :tag :a}
                   (l/element= :a) (l/content "hi"))) => "<a>hi</a>")


;; Doesn't matter what we use for this test and at is the most easily accessible.
(fact "If a transformer contains a seq of a single node, handle it properly"
  (l/to-html (l/at {:type :element :tag :a}
                   (l/element= :a) (fn [node] (list node)))) => "<a></a>")

(fact "Adding nodes that the selector matches does not result in an infinite loop."
  (l/to-html
   (l/at (first (l/parse-fragment "<table><tr><td></td></tr></table>"))
         (l/descendant-of (l/element= :table)
                          (l/element= :tr))
         (fn [node] (list node node)))) => "<table><tbody><tr><td></td></tr><tr><td></td></tr></tbody></table>")

;; Screen scraping

(fact "about select-locs"
  (let [parsed (l/parse "<div></div>")]
    (first (l/select-locs parsed (l/element= :div))) => (nexts 4 parsed)))

(fact "about select"
  (l/select (l/parse "<div><div>hi</div></div>") (l/element= :div))
  => '({:type :element,
        :attrs nil,
        :tag :div,
        :content [{:type :element, :attrs nil, :tag :div, :content ["hi"]}]}
       {:type :element,
        :attrs nil,
        :tag :div,
        :content ["hi"]}))

(facts "about text"
  (l/text (l/node :a :content "hi")) => "hi"
  (l/text (l/node :div :content (l/node :div :content (l/node :a :content "hi")))) => "hi")

;; Misc

(facts "about node"
  (l/node :a) => {:type :element
                  :tag :a
                  :content nil
                  :attrs nil}
  (l/node :a :attrs {:href "https://hi.com"} :content "hi") => {:type :element
                                                                :tag :a
                                                                :content ["hi"]
                                                                :attrs {:href "https://hi.com"}})

(facts "about zip"
  (let [parsed (map hzip/hickory-zip (l/parse-fragment* "<div><a></a></div>"))]
    (l/zip (l/parse-fragment* "<div><a></a></div>")) => parsed
    (l/zip (first (l/parse-fragment* "<div><a></a></div>"))) => (first parsed)
    (l/zip parsed) => parsed))
