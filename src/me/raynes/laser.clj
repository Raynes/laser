(ns me.raynes.laser
  (:require [hickory.core :as hickory]
            [hickory.zip :refer [hickory-zip]]
            [clojure.zip :as zip]
            [me.raynes.laser.zip :as lzip]
            [clojure.string :as string]))

(defn parse
  "Parses an HTML document. This is for top-level full documents,
   complete with <body>, <head>, and <html> tags. If they are not
   present, they will be added to the final result. s can be a string
   in which case it will be treated as a string of HTML or it can be
   something that can be slurped (reader, file, etc)."
  [s]
  (-> (if (string? s)
        s
        (slurp s))
      (hickory/parse)
      (hickory/as-hickory)
      (hickory-zip)
      (lzip/leftmost-descendant)))

(defn parse-fragment
  "Parses an HTML fragment. s can be a string in which case it will be treated
   as a string of HTML or it can be something than can be slurped (reader, file,
   etc)."
  [s]
  (map (comp lzip/leftmost-descendant
             hickory-zip
             hickory/as-hickory)
       (hickory/parse-fragment
        (if (string? s)
          s
          (slurp s)))))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. ^String (as-str text)
    (replace "&"  "&amp;")
    (replace "<"  "&lt;")
    (replace ">"  "&gt;")
    (replace "\"" "&quot;")))

(defn to-html
  "Convert a hickory zip back to html."
  [z] 
  (-> (if (map? z)
        z
        (zip/root z))
      hickory/hickory-to-html))

(defn fragment-to-html
  "Takes a parsed fragment and converts it back to HTML."
  [z]
  (string/join (map to-html z)))

(defn ^:private apply-selector
  "If the selector matches, run transformation on the loc."
  [loc [selector transform]]
  (if (selector loc)
    (zip/edit loc transform)
    loc))

(defn ^:private traverse-zip
  "Iterate through an HTML zipper, running selectors and relevant transformations
   on each node."
  [selectors zip]
  (loop [loc zip]
    (if (zip/end? loc)
      loc
      (let [new-loc (reduce apply-selector loc selectors)]      
        (recur (lzip/next new-loc))))))

(defn ^:private safe-iterate
  "Just like iterate, but stops at the first nil value."
  [f x] (take-while identity (iterate f x)))

;; Selectors

(defn element=
  "A selector that matches an element with this name."
  [element]
  (fn [loc] (= element (-> loc zip/node :tag))))

(defn attr=
  "A selector that checks to see if attr exists and has the value."
  [attr value]
  (fn [loc] (= value (get-in (zip/node loc) [:attrs attr]))))

(defn attr?
  "A selector that matches any element that has the attribute,
   regardless of value."
  [attr]
  (fn [loc]
    (-> (zip/node loc)
        (:attr)
        (contains? attr))))

(defn class=
  "A selector that matches the node's class."
  [class] (attr= :class class))

(defn id=
  "A selector that matches the node's id."
  [id] (attr= :id id))

;; Selector combinators

(defn negate
  "Negates a selector. Like clojure.core/not."
  [selector]
  (fn [loc] (not (selector loc))))

(defn select-and
  "Like and, but for selectors. Returns true iff all selectors match."
  [& selectors]
  (fn [loc] (every? identity (map #(% loc) selectors))))

(defn select-or
  "Like or, but for selectors. Returns true iff at least one selector matches.
   Like 'foo,bar' in css."
  [& selectors]
  (fn [loc] (some (comp not identity) (map #(% loc) selectors))))

(defn descendant-of
  "A selector that matches iff child selector matches, and
   parent-selector matches for some parent node. Like
   'foo bar' in css."
  [parent-selector child-selector]
  (fn [loc]
    (and (child-selector loc)
         (some parent-selector (safe-iterate zip/up loc)))))

(defn ajacent-to
  "A selector that matches iff target selector matches AND
   left selector matches the element immediately preceding it."
  [target left]
  (fn [loc]
    (and (target loc)
         (when-let [loc (zip/left loc)]
           (left loc)))))

(defn child-of
  "A selector that matches iff child selector matches
   and parent-selector matches for the immediate parent node.
   This is like 'foo > bar' in css."
  [parent-selector child-selector]
  (fn [loc]
    (and (child-selector loc)
         (parent-selector (zip/up loc)))))

;; Transformers

(defn content
  "Set content of node to s."
  [s]
  (fn [node] (assoc node :content [s])))

(defn attr
  "Set attribute attr to value."
  [attr value]
  (fn [node] (assoc-in node [:attrs attr] value)))

(defn classes
  "Set the node's class attribute to the string."
  [value]
  (attr :class value))

(defn id
  "Set the node's id to the string."
  [value]
  (attr :id value))

(defn add-class
  "Add a class to the node. Does not replace existng classes."
  [class]
  (fn [node]
    (update-in node [:attrs :class]
               #(str % (when (seq %) " ") class))))

(defn remove-class
  "Remove a class from a node. Does not touch other classes."
  [class]
  (fn [node]
    (update-in node [:attrs :class]
               #(string/join " " (remove #{class} (string/split % #" "))))))

(defn document
  "Transform an HTML document. Use this for any top-level transformation.
   It expects a full HTML document (complete with <html> and <head>) and
   makes it one if it doesn't get one. Takes HTML parsed by the parse-html
   function."
  [s & fns]
  (to-html (traverse-zip (partition 2 fns) s)))

(defn fragment
  "Transform an HTML fragment. Use document for transforming full HTML
   documents. This function does not return HTML, but instead instead
   returns a sequence of zippers of the transformed HTML. This is to make
   composing fragments faster. You can call to-html on the output to get
   HTML."
  [s & fns]
  (let [pairs (partition 2 fns)]
    (map #(zip/root (traverse-zip pairs %)) s)))

(defmacro defragment
  "Define a function that transforms a fragment of HTML."
  [name s args & transformations]
  `(let [html# (parse-fragment ~s)]
     (defn ~name ~args
       (fragment html# ~@transformations))))

(defmacro defdocument
  "Define a function that transforms an HTML document."
  [name s args & transformations]
  `(let [html# (parse ~s)]
     (defn ~name ~args
       (document html# ~@transformations))))