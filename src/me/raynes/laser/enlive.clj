(ns me.raynes.laser.enlive
  (:require [me.raynes.laser :as l]))

(defn- parse-selector
  "Accepts a keyword representing a selector, ie. :a.nav-link
   Returns a hashmap containing the element, id, and classes of
   that selector"
  [selector]
  (let [re #"([^\s\.#]*)(?:#([^\s\.#]+))?(?:\.([^\s#]+))?"
        [_ element-string id class-string] (re-matches re (name selector))]
    {:element (when (not-empty element-string) element-string)
     :id id
     :classes (when class-string (clojure.string/replace class-string "." " "))}))

(defn- translate-selector
  "Translates one element of an enlive-style selector into a laser, function-based selector."
  [selector]
  (let [{:keys [element id classes]} (parse-selector selector)
        element-fn (when element (l/element= (keyword element)))
        id-fn (when id (l/id= id))
        class-fn (when classes (l/class= classes))]
    (cond
     (and element id classes) (l/and element-fn id-fn class-fn)
     (and element id) (l/and element-fn id-fn)
     (and element classes) (l/and element-fn class-fn)
     (and id classes) (l/and id-fn class-fn)
     classes class-fn
     id id-fn
     element element-fn
     :else (throw (Exception. (str "Invalid Selector: " selector))))))

(defn translate
  "Translates an enlive-style vector of selectors into an equivalent laser selector function"
  [selectors]
  (let [selector-fns (map translate-selector selectors)]
    (case (count selectors)
      0 (throw (Exception. "Selector vector must contain a selector"))
      1 (first selector-fns)
      (apply l/child-of selector-fns))))
