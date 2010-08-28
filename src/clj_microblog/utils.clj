(ns clj-microblog.utils
  (:use [clojure.string :only [join]])
  (:require [clojure.contrib.str-utils :as str]
            [clojure.contrib.str-utils2 :as str2]))

(defn ->title [s]
  (join " " (str/re-split #"_+|-+|\s+" s)))

(defn trim-spaces [s]
  (join " " (str/re-split #"\s+" s)))

(defn remove-first [s]
  (str2/drop s 1))

(defn keywordize [map]
  (into {}
        (for [[k v] map]
          [(keyword k) v])))

(defn swap-vals-in [map old new ks]
  (update-in map ks #(if (= old %) new %)))

(defn parse-int [s]
  (try (Integer/parseInt s)
       (catch Exception _)))

(defmacro with-timeout [ms & body]
  `(let [f# (future ~@body)]
     (.get f# 1000 java.util.concurrent.TimeUnit/MILLISECONDS)))

(defmacro throws-exception? [& body]
  `(try ~@body false
        (catch Exception _# true)))
