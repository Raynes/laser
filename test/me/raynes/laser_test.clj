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
  ((l/element= "div") html) => true?
  ((l/element= :pre) html) => false?)

(facts "about attr="
  ((l/attr= :class "a b c") html) => true?
  ((l/attr= "class" "a b c") html) => true?
  ((l/attr= :class (keyword "a b c")) html) => true?
  ((l/attr= :foo "bar") html) => false?)

(facts "about attr?"
  ((l/attr? :class) html) => true?
  ((l/attr? "class") html) => true?
  ((l/attr? :foo) html) => false?)

(facts "about re-attr"
  ((l/re-attr :href #"^/foo") (hzip/hickory-zip (l/node :a :attrs {:href "/foo/bar"}))) => truthy
  ((l/re-attr :href #"^/foo") (hzip/hickory-zip (l/node :a :attrs {:href "/bar/foo"}))) => falsey)

(facts "about class="
  ((l/class= "a") html) => true?
  ((l/class= "a" "b" "c") html) => true?
  ((l/class= "a" :b :c) html) => true?
  ((l/class= "d") html) => false?)

(facts "about re-class"
  ((l/re-class #"a$") (l/zip {:attrs {:class "b oha c"}})) => truthy
  ((l/re-class #"a$") (l/zip {:attrs {:class "b aoh c"}})) => falsey)

(facts "about id="
  ((l/id= "hi") html) => true?
  ((l/id= :hi) html) => true?
  ((l/id= "bye") html) => false?)

(facts "about re-id"
  ((l/re-id #"a$") (l/zip {:attrs {:id "oha"}})) => truthy
  ((l/re-id #"a$") (l/zip {:attrs {:id "aoh"}})) => falsey)

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
  (hickory/hickory-to-html ((l/content "h&i") node)) => "<a>h&amp;i</a>"
  (hickory/hickory-to-html ((l/content "<script/>") node)) => "<a>&lt;script/&gt;</a>"
  (hickory/hickory-to-html ((l/content (l/unescaped "<script/>")) node)) => "<a><script/></a>")

(facts "about insert"
  ((l/insert :left "hi") (l/node :a)) => ["hi" (l/node :a)]
  ((l/insert :right "hi") (l/node :a)) => [(l/node :a) "hi"]
  ((l/insert :left ["hi" "there"]) (l/node :a)) => ["hi" "there" (l/node :a)]
  ((l/insert :right ["hi" "there"]) (l/node :a)) => [(l/node :a) "hi" "there"])

(fact "about attr"
  ((l/attr :class "a") node) => (assoc node :attrs {:class "a"}))

(fact "about remove-attr"
  (let [node {:type :element,
              :tag :a,
              :attrs {:name "foo" :id "bar"},
              :content nil}]
    ((l/remove-attr :id) node) => (assoc node :attrs {:name "foo"})))

(fact "about classes"
  ((l/classes "a b") node) => (assoc node :attrs {:class "a b"}))

(fact "about id"
  ((l/id "hi") node) => (assoc node :attrs {:id "hi"}))

(fact "about add-class"
  ((l/add-class "b") (assoc node :attrs {:class "a"})) => (assoc node :attrs {:class "a b"}))

(fact "about remove-class"
  ((l/remove-class "b") (assoc node :attrs {:class "a b"})) => (assoc node :attrs {:class "a"}))

(fact "about on"
  (l/on {} (l/attr :foo "baz") (l/content "bar")) => {:attrs {:foo "baz"} :content ["bar"]})

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
              [(l/element= :a) (fn [node] node)]) => (repeat 2 (element :a))
  (l/fragment
   (l/parse-fragment "<a></a><a></a>")
   [(l/element= :a) (fn [node] [(element :span) node])]) => [(element :span) (element :a)
                                                             (element :span) (element :a)])

(fact "fragment supports :select"
  (l/fragment (l/parse-fragment "<div><div><div id=\"foo\"><p>hi</p></div></div></div>")
              :select (l/id= "foo")
              [(l/element= :p) (l/content "psych!")]) => [(l/node :div
                                                                  :attrs {:id "foo"}
                                                                  :content (l/node :p :content "psych!"))])

(fact "document and fragment allow conditionals and nested arrays of filter&transformer pairs"
  (l/fragment
   (l/parse-fragment "<html><div></div></html>")

   ; this one will return nil, which will be filtered out
   (when false
     [(l/element= :div) (l/add-class "not-executed")])

    (l/compose-pews
      [(l/element= :div) (l/add-class "c1")]
      [(l/element= :div) (l/add-class "c2")]
      [(l/element= :div) (l/add-class "c3")])) => [{:attrs {:class "c1 c2 c3"},
                                                     :content nil,
                                                     :tag :div,
                                                     :type :element}])

(fact "nils are treated as removals and replaced as empty strings."
  (l/fragment-to-html
             (l/fragment (l/parse-fragment "<a></a>")
                         [(l/any) (constantly nil)])) => "")

(fact "about document"
  (l/document
   (l/parse "<a></a>")
   [(l/element= :a) (l/content "hi")]) => "<html><head></head><body><a>hi</a></body></html>")

(fact "document supports :select"
  (l/document (l/parse "<div id=\"foo\"><p>hi</p></div>")
              :select (l/id= :foo)
              [(l/element= :p) (l/content "psych!")]) => "<div id=\"foo\"><p>psych!</p></div>")

(fact "about at"
  (l/to-html (l/at {:type :element :tag :a}
                   [(l/element= :a) (l/content "hi")])) => "<a>hi</a>")


;; Doesn't matter what we use for this test and at is the most easily accessible.
(fact "If a transformer contains a seq of a single node, handle it properly"
  (l/to-html (l/at {:type :element :tag :a}
                   [(l/element= :a) (fn [node] (list node))])) => "<a></a>")

(fact "Seqs of nodes returned by transformers are inserted and
       the next selector is run on the last node in it."
  (l/to-html
   (l/at (l/node :span :content (l/node :div))
         [(l/element= :div) (fn [node] [node node])])) => "<span><div></div><div></div></span>")

(facts "Adding nodes that the selector matches does not result in an infinite loop."
  (l/to-html
   (l/at (first (l/parse-fragment "<table><tr><td></td></tr></table>"))
         [(l/descendant-of (l/element= :table)
                           (l/element= :tr))
         (fn [node] (list node node))])) => "<table><tbody><tr><td></td></tr><tr><td></td></tr></tbody></table>"
  (l/to-html
   (l/at (first (l/parse-fragment "<span><div></div></span>"))
         [(l/element= :div) (fn [_] [(l/node :div :content (l/node :div :content ""))])])))

(fact "Seqs of nodes from transformers are merged into top-level fragments properly"
  (l/fragment-to-html
   (l/at (l/node :a)
         [(l/element= :a) (fn [node] [node "\n" node "\n"])]
         [(l/element= :hi) (fn [_] :doesnt-matter)])) => "<a></a>\n<a></a>\n")

(fact "there is xml support"
  (l/parse "<td></td>" :parser :xml) => [{:type :document :content [(l/node :td)]} nil]
  (l/parse-fragment "<td></td>" :parser :xml) => [[(l/node :td) nil]]
  (binding [l/*parser* :xml]
    (l/parse "<td></td>") => [{:type :document :content [(l/node :td)]} nil]
    (l/parse-fragment "<td></td>") => [[(l/node :td) nil]]))

(fact "knows what resources are"
  (l/parse "simple.html" :resource true) => (l/parse (java.io.File. "resources/simple.html"))
  (l/parse-fragment "simple.html" :resource true) => (l/parse-fragment (java.io.File. "resources/simple.html")))

;; Screen scraping

(fact "about select-locs"
  (let [parsed (l/parse "<div></div>")]
    (first (l/select-locs parsed (l/element= :div))) => (nexts 4 parsed)))

(fact "about select"
  (let [result '({:type :element,
                  :attrs nil,
                  :tag :div,
                  :content [{:type :element, :attrs nil, :tag :div, :content ["hi"]}]}
                 {:type :element,
                  :attrs nil,
                  :tag :div,
                  :content ["hi"]})]
    (l/select (l/parse "<div><div>hi</div></div>") (l/element= :div)) => result))

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
  (let [parsed (map hzip/hickory-zip (l/nodes "<div><a></a></div>"))]
    (l/zip (l/nodes "<div><a></a></div>")) => parsed
    (l/zip (first (l/nodes "<div><a></a></div>"))) => (first parsed)
    (l/zip parsed) => parsed))
