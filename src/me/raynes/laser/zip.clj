(ns me.raynes.laser.zip
  (:refer-clojure :exclude [next remove])
  (:require [clojure.zip :as zip]))

;; This is all written by David Santiago and is pulled from his excellent
;; Tinsel library. Names have been changed to protect the innocent.

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

(defn remove
  "Same as clojure.zip/remove, but moves on to the next loc in a post order walk."
  [loc]
  (zip/next (zip/remove loc)))