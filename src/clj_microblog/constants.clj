(ns clj-microblog.constants
  (:use [clojure.string :only [lower-case]]))

(def responses
     {:success
      "Success."
      :failure
      "Failure."
      :login-successful
      "You have successfully logged in."
      :registration-successful
      "Registration successful. You must now validate your email address before logging in."
      :invalid-creds
      "Username and password must be 3 to 12 alphanumeric or underscore characters."
      :no-search-results
      "No search results."
      :target-not-found
      "Not all of the users were found. (one missing can cause this error)"
      :username-exists
      "Username already exists."
      :incorrect-password
      "Incorrect password."
      :username-not-found
      "Username not found."
      :illegal-update-length
      "Updates must be between 3 and 90 characters."
      :validation-success
      "Validation successful."
      :validation-failed
      "Error validating the account."
      :no-messages
      "There are no messages in your inbox."
      :account-not-validated
      "You must validate your account before you can log in."
      :invalid-email
      "Invalid email address."
      :just-registered
      "You've already registered once this session. Please check your emails to confirm the registration."
      :log-in-required
      "Log in required to fully view this page."
      :confirm-account-id
      (fn [id] (str "To confirm your account, visit http://localhost:8080/users/confirmation/" id))
      :edit-logged-in-as
      (fn [username] (str "You must be logged in as " username " to edit this profile."))
      :update-wait
      (fn [seconds] (str "You must wait " seconds " seconds before your next update."))
      :user-url
      (fn [username] (str "http://localhost:8080/users/" (lower-case username)))})

(def pages
     ["Login"
      "Register"
      "Users"
      "Tags"
      "Search"
      "Messages"])

(def regexps
     {:email #"^[^@]{1,64}@[^@]{1,255}$"
      :user #"^[a-zA-Z0-9_]{3,12}$"
      :update #"^.{3,90}$"})
