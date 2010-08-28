(ns clj-microblog.core
  (:use [clj-microblog.confirmation :only [send-val-msg]]
        [clj-microblog.constants :only [responses regexps pages]]
        [clj-microblog.feeds :only [user-feed]]
        [clj-microblog.date :only [date-str time-difference-secs]]
        [clj-microblog.utils :only [trim-spaces remove-first keywordize swap-vals-in parse-int with-timeout throws-exception?]]
        [clj-microblog.html :only [redirect-to simple-table tr th td tab-list]]
        [hiccup core page-helpers form-helpers]
        stupiddb.core
        [ring.middleware params file session stacktrace]
        net.cgrand.moustache
        [clojure.string :only [join lower-case capitalize blank? replace-first]]
        [clojure.contrib.str-utils :only [re-split]]
        [clojure.contrib.str-utils2 :only [grep]])
  (:require [clojure.contrib.str-utils2 :as str])
  (:import java.util.Date))

(def user-db (db-init ".users.db" 60))
(def update-db (db-init ".updates.db" 60))
(def validation-db (db-init ".validation.db" 60))

(defn val-regexp [re-key coll]
  (every? #(re-find (re-key regexps) %) coll))

(defn user-data [key-name]
  (db-get-in user-db [:users key-name]))

(defn user-exists? [key]
  (contains? (db-get user-db :users)
             (lower-case key)))

(defn mk-msg [user content]
  {:from user
   :content content
   :date (date-str)})

(defn extract-targets [content]
  (->> content
       (re-split #"\s+")
       (filter #(.startsWith % "@"))
       (map remove-first)
       (map lower-case)))

(defn try-direct-msg [user content]
  (let [msg-targets (extract-targets content)
        msg (mk-msg (-> user user-data :username) content)]
    (if-not (every? user-exists? msg-targets)
      false
      (doseq [target (remove (partial = user) msg-targets)]
        (db-update-in user-db [:users target :messages] conj msg)))))

(defn can-update? [user]
  (if-let [last-update-time (:last-update-time user)]
    (#(or (neg? %)
          ((:update-wait responses) %))
     (- 60 (time-difference-secs last-update-time)))
    true))

(defn trending-tags []
  (->> (db-get update-db :status-list)
       (map :status)
       (join " ")
       (re-split #"\s+")
       (grep #"^#")
       (map remove-first)
       (map capitalize)
       (frequencies)
       (sort)))

(defn gen-id [upper]
  (let [id-map (:data @validation-db)]
    (->> (repeatedly #(rand-int upper))
         (drop-while #(id-map %))
         (first))))

(defn highlight-update [update-str]
  (letfn [(drop-1 [s] (str/drop s 1))]
    (for [word (interpose " " (re-split #"\s+" update-str))]
      (condp #(.startsWith %2 %1) word
        "#" [:tag (link-to (str "/tags/" (drop-1 word)) word)]
        "@" [:user (link-to (str "/users/" (drop-1 word)) word)]
        [:word word]))))

(defn search [search-term coll]
  (grep (re-pattern (str "(?i)" search-term)) coll))

(defn search-users [search-term]
  (->> :users
       (db-get user-db)
       (vals)
       (map :username)
       (search search-term)))

(defn search-pages [search-term]
  (search search-term pages))

(defn search-tags [search-term]
  (->> (trending-tags)
       (keys)
       (search search-term)))

(defn search-all [search-term]
  (zipmap [:users :pages :tags]
          ((juxt search-users search-pages search-tags)
           search-term)))

(defn render-result-table [base-url results]
  (html
   [:table {:border "0" :width "100%"}
    (for [result results]
      (td (link-to (str base-url (lower-case result)) result)))]))

(defn mk-update [username update-str]
  {:time (date-str)
   :status update-str
   :user username})

(defn check-response [session]
  (when-let [response (session :response)]
    [:div#dialog {:title "Response"}
     [:p response]]))

(defn update-status [session status]
  (let [key-name (:logged-in-as session)
        user (user-data key-name)
        update (mk-update (:username user) status)
        status-list (conj (:status-list user) update)]
    (db-update-in user-db [:users key-name :status-list] conj (dissoc update :user))
    (db-assoc-in user-db [:users key-name :last-update-time] (date-str))
    (db-update-in update-db [:status-list] conj update)))

(defn update-profile [old-name new-name data]
  (let [old-data (db-get-in user-db [:users old-name])]
    (db-dissoc-in user-db [:users] old-name)
    (db-assoc-in user-db [:users new-name] (merge old-data data))))

(defn mk-user [username password email]
  (db-assoc-in
   user-db [:users (lower-case username)]
   {:username username
    :password password
    :email email
    :joined (date-str)
    :validated? false
    :messages ()}) nil)

(defn register-login [username password email]
  (let [key (lower-case username)]
    (cond (user-exists? key)
          (:username-exists responses)
          (not (val-regexp :user [username password]))
          (:user-cred-failure responses)
          (not (val-regexp :email [email]))
          (:invalid-email responses)
          :else (do (.start (Thread.
                             #(let [id (-> 10000 gen-id str)]
                                (send-val-msg key email id)
                                (db-assoc validation-db id key))))
                    (mk-user username password email)))))

(defn check-login [username password]
  (if-let [user (user-data (lower-case username))]
    (if-not (:validated? user)
      (:account-not-validated responses)
      (if (= password (:password user))
        (:success responses) (:incorrect-password responses)))
    (:username-not-found responses)))

(defn render-links [session]
  (let [name (:logged-in-as session)]
    (if-let [user (user-data name)]
      (let [msg-count (str "(" (count (:messages user)) ")")]
        (html (link-to "/" "Home")
              (link-to "/users" "Users")
              (link-to "/tags" "Tags")
              (link-to "/messages" (str "Messages" msg-count))
              (link-to "/search" "Search")
              (link-to (str "/users/" name "/edit") "Settings")
              (link-to "/logout" "Log out")))
      (html (link-to "/" "Home")
            (link-to "/login" "Log in")
            (link-to "/register" "Register")
            (link-to "/users" "Users")
            (link-to "/tags" "Tags")
            (link-to "/search" "Search")))))

(defn head-html [session title & body]
  (html
   (:html4 doctype)
   [:head [:title title]
    (include-css "/css/styles.css"
                 "/css/jquery-ui-1.8.2.css")
    (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js"
                "/javascript/clj-microblog.js"
                "http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.2/jquery-ui.min.js")]
   [:center [:header [:header.title title [:br]]
             (render-links session)]]
   [:body body]))

(defn invalid-url-html [session]
  (head-html
   session "Invalid URL"
   [:center
    [:img {:src "/images/page-not-found.png"}]]))

  ;; Adapted from Brian Carper's cow blog.
(defn tag-cloud-html [session]
  (head-html
   session "Tags"
   [:center
    (let [min 25, max 70, weight 5.0
          check-fn #(if (> max %) max %)
          weight-fn #(check-fn (+ min (* weight %)))]
      [:span (for [tag (trending-tags)]
               [:a {:href (str "/tags/" (lower-case (key tag)))
                    :style (str "font-size: " (weight-fn (val tag)) "px")}
                (key tag) " "])])]))

(defn home-html [session]
  (let [{:keys [logged-in-as num-updates update-value]} session
        num (or (parse-int num-updates) 10)]
    (head-html
     session "Home"
     [:center
      (when logged-in-as
        (html [:h1 "Update your status"]
              (form-to [:post "/"]
                       [:status (text-field "update" update-value)]
                       [:br] [:br] (submit-button "Submit") [:br] [:hr])))
      [:div#slider]
      (form-to [:post "/"]
               [:input {:type "hidden" :id "hidden-num" :value num}]
               [:input.button {:type "submit" :name "num-updates" :id "slider-val"}])
      [:table.small {:width "100%"}
       [:h1 [:b "Latest updates"]]
       (th "Username" "Time" "Status")
       (for [{:keys [time status user]}
             (take num (db-get update-db :status-list))]
         (tr [:td {:width "200px"} (link-to (str "/users/" user) user)]
             [:td {:width "300px"} time]
             [:td (highlight-update status)]))]
      (check-response session)])))

(defn search-html [session]
  (head-html
   session "Search"
   (form-to [:post "/search"] [:br]
            [:search (text-field "search" (:search session))] [:br] [:br]
            [:submit-search (submit-button "Search")]) [:br]
            (if-let [{:keys [users pages tags]} (:results session)]
              (html
               [:center [:h.large "Results"]] [:br]
               [:div {:id "search-tabs"}
                (tab-list "users" "pages" "tags")
                [:div {:id "users"}
                 (render-result-table "/users/" users)]
                [:div {:id "pages"}
                 (render-result-table "/" pages)]
                [:div {:id "tags"}
                 (render-result-table "/tags/" tags)]])
              (check-response session))))

(defn login-html [session]
  (if (:logged-in-as session)
    (redirect-to "/")
    (head-html
     session "Log in"
     (form-to [:post "/login"]
              [:h.small "Username"] [:br]
              (text-field "username" "Username") [:br]
              [:h.small "Password"] [:br]
              (password-field "password" "Password") [:br] [:br]
              (submit-button "Log in"))
     (check-response session))))

(defn register-html [session]
  (head-html
   session "Register"
   (cond (:registered session)
         (check-response {:response (:just-registered responses)})
         (:just-registered session)
         (check-response session)
         :else (html
                (form-to [:post "/register"]
                         [:h.small "Username"] [:br]
                         (text-field "username" "Username") [:br]
                         [:h.small "Password"] [:br]
                         (password-field "password" "Password") [:br]
                         [:h.small "Email"] [:br]
                         (text-field "email" "Email") [:br] [:br]
                         [:input.register {:type "Submit" :value "Register"}])
                (check-response session)))))

(defn user-page-html [name session]
  (let [{:keys [logged-in-as]} session
        {:keys [username joined status-list]} (user-data name)]
    (if-not username
      (invalid-url-html session)
      (head-html
       session username
       [:center (when (= name logged-in-as)
                  (link-to (str "/users/" username "/edit") "Edit my profile"))
        (simple-table {:th ["Username" "Joined"]
                       :td [username joined]}) [:hr]
        [:feed (link-to (str "/users/" name "/feed")
                        "Subscribe to this users RSS feed")]
        (when status-list
          [:table.small {:width "80%"}
           (th "Date" "Update")
           (for [{:keys [time status]} status-list]
             (td time (highlight-update status)))])]))))

(defn user-list-html [session]
  (head-html
   session "Users"
   [:table.small {:width "100%"}]
   (th "Username" "Joined")
   (for [{:keys [username joined]} (vals (db-get user-db :users))]
     (td (link-to (str "/users/" username) username)
         joined))))

(defn profile-edit-html [name session]
  (let [{:keys [username password email]} (user-data name)]
    (if-not (= (:logged-in-as session) name)
      (head-html
       session "Error"
       (check-response {:response ((:edit-logged-in-as responses) username)}))
      (head-html
       session (str "Editing profile: " username)
       (form-to [:post (str "/users/" name "/edit")]
                [:h.small "Username"] [:br]
                (text-field "username" username) [:br]
                [:h.small "Password"] [:br]
                (password-field "password" password) [:br]
                [:h.small "Email"] [:br]
                (text-field "email" email) [:br] [:br]
                (submit-button "Confirm"))
       (check-response session)))))

(defn messages-html [session]
  (head-html
   session "Messages"
   (if-let [messages (-> :logged-in-as session user-data :messages)]
     (if (empty? messages)
       (check-response {:response (:no-messages responses)})
       [:center
        [:table.small
         (th "From" "Time" "Message")
         (for [{:keys [from content date]} messages]
           (td (link-to (str "/users/" (lower-case from)) from)
               date
               (highlight-update content)))]])
     (check-response {:response (:log-in-required responses)}))))

(defn messages-handler [{session :session}]
  {:status 200
   :session session
   :headers {"Content-Type" "text/html"}
   :body (messages-html session)})

(defn profile-edit-get-handler [{session :session} & username]
  {:status 200
   :session (dissoc session :response)
   :headers {"Content-Type" "text/html"}
   :body (profile-edit-html (or (:in-as session) (first username)) session)})

(defn profile-edit-post-handler [{session :session params :params}]
  (let [{:strs [username password email]} params
        key (lower-case username)
        in-as (:logged-in-as session)]
    (#(profile-edit-get-handler {:session (merge session %)})
     (cond (not (val-regexp :user [username password]))
           {:response (:invalid-creds responses) :in-as in-as}
           (not (val-regexp :email [email]))
           {:response (:invalid-email responses) :in-as in-as}
           (and (not= key in-as) (user-exists? key))
           {:response (:username-exists responses) :in-as in-as}
           :else (let [name (-> in-as user-data :username)]
                   (db-update-in update-db [:status-list]
                                 #(map swap-vals-in %) name username [:user])
                   (update-profile in-as key (keywordize params))
                   {:logged-in-as key :in-as key})))))

(defn user-page-handler [{session :session} username]
  {:status 200
   :session session
   :headers {"Content-Type" "text/html"}
   :body (user-page-html username session)})

(defn user-list-handler [{session :session}]
  {:status 200
   :session session
   :headers {"Content-Type" "text/html"}
   :body (user-list-html session)})

(defn home-get-handler [{session :session}]
  {:status 200
   :session (dissoc session :response :update-value)
   :headers {"Content-Type" "text/html"}
   :body (home-html session)})

(defn home-post-handler [{session :session params :params}]
  (let [{:strs [num-updates update]} params
        update-str (when-not (empty? update) (trim-spaces update))
        logged-in-as (:logged-in-as session)
        num-str (str/drop num-updates 19)
        can-update-res (-> logged-in-as user-data can-update?)]
    (#(home-get-handler {:session (merge session {:num-updates "10"} %)})
     (cond (-> num-str empty? not)
           {:num-updates num-str}
           (->> [update-str] (val-regexp :update) not)
           {:response (:illegal-update-length responses)}
           (-> can-update-res true? not)
           {:response can-update-res :update-value update-str}
           (try-direct-msg logged-in-as update-str)
           {:response (:target-not-found responses)}
           :else (update-status session update-str)))))

(defn search-get-handler [{session :session}]
  {:status 200
   :session (dissoc session :results :response :search)
   :headers {"Content-Type" "text/html"}
   :body (search-html session)})

(defn search-post-handler [{session :session params :params}]
  (let [{:strs [search]} params
        results (search-all search)]
    (#(search-get-handler {:session (merge session % {:search search})})
     (if (every? empty? (vals results))
       {:response (:no-search-results responses)}
       {:results results}))))

(defn login-get-handler [{session :session}]
  {:status 200
   :session (dissoc session :response)
   :headers {"Content-Type" "text/html"}
   :body (login-html session)})

(defn login-post-handler [{session :session params :params}]
  (let [{:strs [username password]} params
        response (check-login username password)]
    (#(login-get-handler {:session (merge session %)})
     (if (= response (:success responses))
       {:logged-in-as (lower-case username)}
       {:response response}))))

(defn register-get-handler [{session :session}]
  {:status 200
   :session (if (:just-registered session)
              (assoc (dissoc session :response) :registered true)
              (dissoc session :response))
   :headers {"Content-type" "text/html"}
   :body (register-html session)})

(defn register-post-handler [{session :session params :params}]
  (let [{:strs [username password email]} params
        response (register-login username password email)]
    (#(register-get-handler {:session (merge session %)})
     (if-not response
       {:response (:registration-successful responses)
        :just-registered (:just-registered responses)}
       {:response response}))))

(defn invalid-url-handler [{session :session}]
  {:status 200
   :session session
   :headers {"Content-Type" "text/html"}
   :body (invalid-url-html session)})

(defn logout-handler [{session :session}]
  {:status 200
   :session {}
   :headers {"Content-Type" "text/html"}
   :body (redirect-to "/")})

(defn tag-handler [{session :session}]
  {:status 200
   :session session
   :headers {"Content-Type" "text/html"}
   :body (tag-cloud-html session)})

(defn confirmation-handler [{session :session} id]
  (let [response (if-let [user (db-get validation-db id)]
                   (do (db-assoc-in user-db [:users user :validated?] true)
                       (db-dissoc validation-db id)
                       (:validation-success responses))
                   (:validation-failed responses))]
    {:status 200
     :session session
     :headers {"Content-Type" "text/html"}
     :body (head-html
            session "Confirmation"
            (check-response {:response response}))}))

(defn user-rss-handler [{session :session} user]
  {:status 200
   :session session
   :headers {"Content-Type" "text/xml; charset=utf-8"}
   :body (or (-> user user-data user-feed)
             (invalid-url-handler session))})

(def routes
     (app
      (wrap-session)
      (wrap-file "resources/public")
      (wrap-params)
      (wrap-stacktrace)
      [""] {:get home-get-handler
            :post home-post-handler}
      ["search"] {:get search-get-handler
                  :post search-post-handler}
      ["login"] {:get login-get-handler
                 :post login-post-handler}
      ["register"] {:get register-get-handler
                    :post register-post-handler}
      ["logout"] logout-handler
      ["users"] user-list-handler
      ["users" username] #(user-page-handler % (lower-case username))
      ["users" username "edit"] {:get #(profile-edit-get-handler % (lower-case username))
                                 :post profile-edit-post-handler}
      ["users" user "feed"] #(user-rss-handler % (lower-case user))
      ["tags"] tag-handler
      ["tags" tag] tag-handler
      ["messages"] messages-handler
      ["users" "confirmation" id] {:get #(confirmation-handler % id)}
      [&] invalid-url-handler))
