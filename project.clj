(defproject clj-microblog "0.1.0-SNAPSHOT"
  :description "Clj-Microblog is an attempt at writing a microblogging site."
  :dependencies [[org.clojure/clojure "1.2.0"] 
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring/ring-jetty-adapter "0.2.5"]
                 [hiccup "0.2.6"]
                 [stupiddb "0.3.2"]
                 [clj-mail "0.1.0"]
                 [clj-ini "0.1.1"]
                 [net.cgrand/moustache "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
                     [ring/ring-devel "0.2.5"]])
