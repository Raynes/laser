(defproject me.raynes/laser "1.1.1"
  :description "An HTML transformation library similar to Enlive/Tinsel."
  :url "https://github.com/Raynes/laser"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/hickory "0.4.2"]
                 [org.flatland/useful "0.9.3"]]
  :profiles {:dev {:dependencies [[midje "1.5-alpha8"]]}}
  :plugins [[lein-midje "3.0-alpha4"]]
  :codox {:src-dir-uri "https://github.com/Raynes/laser/tree/e1beb765cf40564a789fa5d2d5f795e9df724530"
          :src-linenum-anchor-prefix "L"})
