(ns me.raynes.laser
  (:refer-clojure :exclude [remove])
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

(defn parse-fragment*
  "Like parse-fragment, but don't get a zipper over it."
  [s]
  (map hickory/as-hickory
       (hickory/parse-fragment
        (if (string? s)
          s
          (slurp s)))))

(defn parse-fragment
  "Parses an HTML fragment. s can be a string in which case it will be treated
   as a string of HTML or it can be something than can be slurped (reader, file,
   etc)."
  [s]
  (map (comp lzip/leftmost-descendant
             hickory-zip)
       (parse-fragment* s)))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. ^String text
    (string/replace "&"  "&amp;")
    (string/replace "<"  "&lt;")
    (string/replace ">"  "&gt;")
    (string/replace "\"" "&quot;")))

(defn to-html
  "Convert a hickory zip back to html."
  [z]
  (-> (if (sequential? z)
        (zip/root z)
        z)
      hickory/hickory-to-html))

(defn fragment-to-html
  "Takes a parsed fragment and converts it back to HTML."
  [z]
  (string/join (map to-html z)))

(defn ^:private edit [l f & args]
  (let [result (apply f (zip/node l) args)]
    (if (sequential? result)
      (if (zip/up l)
        (zip/replace (reduce #(zip/insert-left % %2) l result) "")
        {:top-left result})
      (zip/replace l result))))

(defn ^:private apply-selector
  "If the selector matches, run transformation on the loc."
  [loc [selector transform]]
  (if (and (selector loc) (map? (zip/node loc)))
    (edit loc transform)
    loc))

(defn ^:private traverse-zip
  "Iterate through an HTML zipper, running selectors and relevant transformations
   on each node."
  [selectors zip]
  (loop [loc zip]
    (cond
     (map? loc) (:top-left loc)
     (zip/end? loc) (zip/root loc)
     :else (let [new-loc (reduce apply-selector loc selectors)]   
             (recur (if (map? new-loc)
                      new-loc
                      (lzip/next new-loc)))))))

(defn ^:private safe-iterate
  "Just like iterate, but stops at the first nil value."
  [f x] (take-while identity (iterate f x)))

(defn nodes
  "Normalizes nodes. If s is a string, parse it as a fragment and get
   a sequence of nodes. If s is sequential already, return it assuming
   it is already a seq of nodes. If it is anything else, wrap it in a
   vector (for example, if it is a map, this will make it a vector of
   maps (nodes)"
  [s]
  (cond
   (string? s) (parse-fragment* s)
   (sequential? s) s
   :else [s]))

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
        (:attrs)
        (contains? attr))))

(defn class=
  "A selector that matches if the node has these classes."
  [& classes]
  (fn [loc]
    (let [node (zip/node loc)]
      (every? (set classes)
              (string/split
               (get-in node [:attrs :class] "") #" ")))))

(defn id=
  "A selector that matches the node's id."
  [id] (attr= :id id))

(defn any
  "A selector that matches any node."
  [] (constantly true))

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
  (fn [loc] (boolean (some identity (map #(% loc) selectors)))))

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
  "Set content of node to the string s. It will be escaped automatically
   by hickory when converting back to html."
  [s]
  (fn [node] (assoc node :content [s])))

(defn html-content
  "Set content of node to s, unescaped. Can take a string of HTML or
   already parsed nodes."
  [s]
  (fn [node] (assoc node :content (nodes s))))

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
               #(string/join " " (clojure.core/remove #{class} (string/split % #" "))))))

(defn wrap
  "Wrap a node around the node. Provide the element name as a key (like :div)
   and optionally a map of attributes."
  [tag & [attrs]]
  (fn [node] {:type :element :tag tag :attrs attrs :content [node]}))

(defn remove
  "Delete a node."
  [] (constantly ""))

(defn document
  "Transform an HTML document. Use this for any top-level transformation.
   It expects a full HTML document (complete with <html> and <head>) and
   makes it one if it doesn't get one. Takes HTML parsed by the parse-html
   function."
  [s & fns]
  (to-html (traverse-zip (partition 2 fns) s)))

(defn fragment
  "Transform an HTML fragment. Use document for transforming full HTML
   documents. This function does not return HTML, but instead returns a
   sequence of zippers of the transformed HTML. This is to make
   composing fragments faster. You can call to-html on the output to get
   HTML."
  [s & fns]
  (let [pairs (partition 2 fns)]
    (reduce #(if (sequential? %2)
               (into % %2)
               (conj % %2))
            []
            (map (partial traverse-zip pairs) s))))

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