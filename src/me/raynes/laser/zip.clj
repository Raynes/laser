(ns me.raynes.laser.zip
  (:refer-clojure :exclude [next])
  (:require [clojure.zip :as zip]
            [hickory.zip :refer [hickory-zip]]))

;; leftmost-descendant and next written by David Santiago in his tinsel library.

(defn leftmost-descendant
  "Given a zipper loc, returns its leftmost descendent (ie, down repeatedly)."
  [loc]
  (if (and (zip/branch? loc) (zip/down loc))
    (recur (zip/down loc))
    loc))

(defn next
  "Moves to the next loc in the hierarchy in postorder traversal. Behaves like
   clojure.zip/next otherwise. Note that unlike with a pre-order walk, the root
   is NOT the first element in the walk order, so be sure to take that into
   account in your algorithm if it matters (ie, call csleftmost-descendant first
   thing before processing a node)."
  [loc]
  (if (= :end (loc 1)) ;; If it's the end, return the end.
    loc
    (if (nil? (zip/up loc))
      [(zip/node loc) :end]
      (or (and (zip/right loc) (leftmost-descendant (zip/right loc)))
          (zip/up loc)))))

(defn zipper?
  "Checks to see if the object has zip/make-node metadata on it (confirming it
   to be a zipper."
  [obj]
  (contains? (meta obj) :zip/make-node))

(defn zip
  "Get a zipper suitable for passing to fragment, document, or select, from
   a hickory node or a sequence of hickory nodes."
  [n]
  (cond
   (zipper? n) n
   (sequential? n) (map zip n)
   :else (hickory-zip n)))

(defn ^:private merge? [loc]
  (:merge-left (meta loc)))

(defn ^:private merge-left [locs]
  (with-meta locs {:merge-left true}))

(defn ^:private edit [l f & args]
  (let [result (apply f (zip/node l) args)]
    (if (sequential? result)
      (merge-left (for [node result] (or node "")))
      (zip/replace l (or result "")))))

(defn ^:private apply-selector
  "If the selector matches, run transformation on the loc."
  [loc [selector transform]]
  (if (and (selector loc) (map? (zip/node loc)))
    (let [edited (edit loc transform)]
      (if (merge? edited)
        edited
        [edited]))
    [loc]))

(defn ^:private apply-selectors [loc selectors]
  (let [result (reduce (fn [locs selector]
                         (mapcat #(apply-selector (zip %) selector) locs))
                       [loc]
                       selectors)]
    (if (> (count result) 1)
      (if (zip/up loc)
        (zip/remove (reduce #(zip/insert-left % %2) loc result))
        (merge-left result))
      (first result))))

(defn traverse-zip
  "Iterate through an HTML zipper, running selectors and relevant transformations
   on each node."
  [selectors zip]
  (loop [loc zip]
    (cond
     (merge? loc) loc
     (zip/end? loc) (zip/root loc)
     :else (let [new-loc (apply-selectors loc selectors)]
             (recur (if (merge? new-loc)
                      new-loc
                      (next new-loc)))))))
