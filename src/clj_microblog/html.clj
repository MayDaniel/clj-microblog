(ns clj-microblog.html
  (:use [hiccup.core :only [html]]
        [hiccup.page-helpers :only [unordered-list link-to]]
        [clojure.string :only [lower-case capitalize]]))

(defn redirect-to [url]
  (html
   [:meta
    {:http-equiv "Refresh"
     :content (str "0;url=" url)}]))

(defn simple-table [{:keys [name display th td]
                     :or {name :table display {:width "100%" :border "0"}}}]
  (html [name display
         [:tr (for [th th] [:th th])]
         [:tr (for [td td] [:td td])]]))

(defn tab-list [& tab-names]
  (html
   (unordered-list
    (for [s tab-names]
      (link-to (str "#" (lower-case s))
               (capitalize s))))))

(defn tr [& ts]
  [:tr ts])

(defn th [& ths]
  [:tr (for [th ths] [:th th])])

(defn td [& tds]
  [:tr (for [td tds] [:td td])])
