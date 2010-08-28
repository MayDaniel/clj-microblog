(ns clj-microblog.feeds
  (:use [clj-microblog.constants :only [responses]]
        hiccup.core)
  (:import java.net.URL))

(defn base-feed [title link description & body]
  (html "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
        [:rss {:version "2.0"}
         [:channel
          [:title title]
          [:link link]
          [:description description]]
         body]))

(defn user-feed [user-data]
  (if-not user-data nil
          (let [{:keys [username status-list]} user-data
                description (str username "'s Clj-Microblog updates")
                url ((:user-url responses) username)]
            (base-feed description url description
                       (for [{:keys [time status]} status-list]
                         [:item
                          [:title time]
                          [:url url]
                          [:description status]])))))
