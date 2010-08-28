(ns clj-microblog.date
  (:import java.util.Date))

(defn date []
  (Date.))

(defn date-str []
  ((comp str date)))

(defn sys-time []
  (System/currentTimeMillis))

(defmulti time-difference-secs class)

(defmethod time-difference-secs java.util.Date [time]
  (-> (.getTime (Date.))
      (- (.getTime time))
      (/ 1000.0)
      (int)))

(defmethod time-difference-secs java.lang.Long [time]
  (-> (System/currentTimeMillis)
      (- time)
      (/ 1000.0)
      (int)))

(defn n-days-ago? [time n]
  (> (time-difference-secs time)
     (* 86400 n)))

(defn after? [x y]
  (= 1 (compare x y)))

(defn before? [x y]
  (= -1 (compare x y)))
