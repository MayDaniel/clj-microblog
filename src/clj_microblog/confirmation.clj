(ns clj-microblog.confirmation
  (:use [clj-microblog.constants :only [responses]]
        [clj-mail.core :only [send-msg rTO]]
        [clj-ini.core :only [read-map]]
        [clojure.set :only [rename-keys]]))

(defn mail-creds
  "Reads the credentials from mail.conf set for the account validation email sender."
  []
  (rename-keys
   (read-map "mail.conf")
   {:email :user
    :ssl? :ssl
    :password :pass}))

(defn send-val-msg
  "Constructs and transports the email."
  [key-name to id]
  (let [subject "Clj-Microblog - Confirm your account"
        {:keys [host user pass port ssl]} (mail-creds)]
    (send-msg :to to :user user :pass pass :port port :ssl ssl 
              :type rTO :subject subject :host host
              :body ((:confirm-account-id responses) id))))
