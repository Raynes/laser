(ns me.raynes.laser.zip
  (:refer-clojure :exclude [next remove])
  (:require [clojure.zip :as zip]
            [hickory.zip :refer [hickory-zip]]))

;; leftmost-descendant and next were both written by David Santiago and pulled
;; from his excellent Tinsel library. Names have been changed to protect the
;; innocent.

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

;; END DAVIDNESS

(defn remove
  "Same as clojure.zip/remove, but moves on to the next loc in a post order walk."
  [loc]
  (zip/next (zip/remove loc)))

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
    (cond
     (and (sequential? result) (zip/up l))
     (-> (reduce zip/insert-left l result)
         (zip/replace "")
         (zip/left))
     (sequential? result) (merge-left result)
     :else (zip/replace l result))))

(defn ^:private apply-selector
  "If the selector matches, run transformation on the loc."
  [loc [selector transform]]
  (if (and (selector loc) (map? (zip/node loc)))
    (edit loc transform)
    loc))

(defn ^:private loc-seq [loc selectors]
  (reduce (fn [[head & tail] acc]
            (let [result (apply-selector (zip head) acc)]
              (if (merge? result)
                (into tail result)
                (cons result tail))))
          (list loc)
          selectors))

(defn ^:private apply-selectors [loc selectors]
  (let [[head & tail :as all] (reverse (loc-seq loc selectors))]
    (if (seq tail)
      (merge-left all)
      (zip head))))

(defn traverse-zip
  "Iterate through an HTML zipper, running selectors and relevajnt transformations
   on each node."
  [selectors zip]
  (loop [loc zip]
    (cond
     (merge? loc) (map #(if (zipper? %) (zip/root %) %) loc)
     (zip/end? loc) (zip/root loc)
     :else (let [new-loc (apply-selectors loc selectors)]
             (recur (if (merge? new-loc)
                      new-loc
                      (next new-loc)))))))
