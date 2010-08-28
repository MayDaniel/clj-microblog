(ns run
  (:require [clj-microblog.core :as blog])
  (:use ring.adapter.jetty))

(defonce server (run-jetty #'blog/routes {:port 8080 :join? false}))
