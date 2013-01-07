(defproject me.raynes/laser "0.1.14"
  :description "An HTML transformation library similar to Enlive/Tinsel."
  :url "https://github.com/Raynes/laser"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hickory "0.2.2"]]
  :profiles {:dev {:dependencies [[midje "1.5-alpha5"]]}}
  :plugins [[lein-midje "3.0-alpha1"]]
  :codox {:src-dir-uri "https://github.com/Raynes/laser/tree/e1beb765cf40564a789fa5d2d5f795e9df724530"
          :src-linenum-anchor-prefix "L"})
