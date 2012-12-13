(ns me.raynes.laser
  (:require [hickory.core :as hickory]
            [hickory.zip :refer [hickory-zip]]
            [clojure.zip :as zip]
            [me.raynes.laser.zip :as lzip]
            [clojure.string :as string]))

(defn parse-html
  "If s is a string, parse the document from the string. Otherwise,
   assume it is a reader and read from it."
  [s]
  (-> (if (string? s)
        s
        (slurp s))
      (hickory/parse)
      (hickory/as-hickory)
      (hickory-zip)
      (lzip/leftmost-descendant)))

(defn zip-to-html
  "Convert a hickory zip back to html."
  [z]
  (-> z zip/root hickory/hickory-to-html))

(defn apply-selector [loc [selector transform]]
  (if (selector loc)
    (zip/edit loc transform)
    loc))

(defn traverse-zip [selectors zip]
  (loop [loc zip]
    (let [new-loc (reduce apply-selector loc selectors)]
      (if (zip/end? new-loc)
        new-loc
        (recur (lzip/next new-loc))))))

(defn safe-iterate
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

(defn class=
  "A selector that matches the node's class."
  [class] (attr= :class class))

(defn id=
  "A selector that matches the node's id."
  [id] (attr= :id id))

;; Selector combinators

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

(defn transform
  "Transform an HTML string."
  [s & fns]
  (let [pairs (partition 2 fns)]
    (zip-to-html
     (traverse-zip pairs (parse-html s)))))