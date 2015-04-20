(ns kids-checkin.api
  (:require
   [clojure.edn :as edn]
   [clojure.data.json :as json]
   [clojure.pprint :as p]
   [clj-http.client :as client]
   [ring.util.codec :only [base64-encode form-encode] :as r]
   (clj-time [core :as time] [coerce :as tc]))
  (:import (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))

(def checkins-store (atom []))

(defn- load-config
  "Load thecity API config"
  []
  (edn/read-string (slurp "src/clj/kids_checkin/config.edn")))

(defn- create-secret-key [key mac]
  (SecretKeySpec. (.getBytes key) (.getAlgorithm mac)))

(defn- sign-text-with-key
  "Returns the signature of a string with a given key, using a SHA-256 HMAC."
  [key string]
  (let [mac (Mac/getInstance "HMACSHA256")
        secretKey (create-secret-key key mac)]
    (-> (doto mac
          (.init secretKey)
          (.update (.getBytes string)))
        .doFinal)))

(defn- sign-and-encode-text [text-to-sign key]
  (->> text-to-sign
       (sign-text-with-key key)
       (r/base64-encode)
       (r/form-encode)))

(defn- create-headers [url]
  (let [config (load-config)
        unix-time (tc/to-long (time/now))
        string-to-sign (str unix-time "GET" url)
        signed-string (sign-and-encode-text string-to-sign (:key config))]
        {"X-City-Sig" signed-string
         "X-City-User-Token" (:user-token config)
         "X-City-Time" (str unix-time)
         "Accept" "application/vnd.thecity.admin.v1+json"
         }))

(defn- get-starting-date
  "Retrieves starting date for checkin and return date portion by taking first 10 chars"
  [checkin]
  (apply str (take 10 (get-in checkin [:event :starting_at]))))

(defn- retrieve-new-checkins
  "Use our application cache to only bring in checkins up to the latest we already
  have by comparing barcodes, which should be unique"
  [checkins]
  (take-while #(not= (:barcode %) (:barcode (first @checkins-store))) checkins))

(defn- retrieve-checkins-for-result-page
  "Grab the specified page of checkins from thecity and return a vector of the ones
  we don't already have"
  [page-number]
  (let [url (str "https://api.onthecity.org/checkins?page=" page-number)
        headers (create-headers url)]
    (-> (client/get url {:headers headers})
        (:body)
        (json/read-str :key-fn keyword)
        (:checkins)
        (retrieve-new-checkins))))

(defn- retrieve-checkins
  "Page through the list of checkins from thecity and add them to a vector which
  we be stored in an application cache, then return it for use"
  ([] (retrieve-checkins 1 []))
  ([page-number checkins]
   (let [today (.toString (time/today) "MM/dd/yyyy")
         new-checkins (retrieve-checkins-for-result-page page-number)
         last-checkin-date (get-starting-date (last new-checkins))]
     (if (and (= 20 (count new-checkins)) (= today last-checkin-date))
       (retrieve-checkins (inc page-number) (into checkins new-checkins))
       (let [complete-checkin-list (into checkins (filter #(= today (get-starting-date %)) new-checkins))]
         (swap! checkins-store into complete-checkin-list)
         @checkins-store)))))

(defn- count-checkins-for-group
  "Count the number of checkins in each checkin group"
  [group-id checkins]
  (count (filter #(= group-id (get-in % [:group :id])) checkins)))

(defn- fake-list-of-checkin-count-by-group
  "Fake checkin results so we don't have to query thecity while working locally"
  []
  [{:id 108117 :color "red" :count 1 :max 12 :name "Nursery"}
   {:id 108119 :color "orange" :count 1 :max 12 :name "Toddlers"}
   {:id 108120 :color "yellow" :count 1 :max 12 :name "Preschool #1"}
   {:id 144673 :color "green" :count 1 :max 12 :name "Preschool # 2"}
   {:id 108123 :color "blue" :count 1 :max 12 :name "Primary"}
   {:id 89515 :color "purple" :count 1 :max 12 :name "Elementary"}])

(defn create-list-of-checkin-count-by-group
  "Creates a count of checkins for each checkin group which has occured today on the city"
  [env]
  (if (false? (:dev env))
    (fake-list-of-checkin-count-by-group)
    (let [checkins (retrieve-checkins)]
      [{:id 108117 :max 12 :color "red" :count (count-checkins-for-group 108117 checkins) :name "Nursery"}
       {:id 108119 :max 12 :color "orange" :count (count-checkins-for-group 108119 checkins) :name "Toddlers"}
       {:id 108120 :max 12 :color "yellow" :count (count-checkins-for-group 108120 checkins) :name "Preschool #1"}
       {:id 144673 :max 12 :color "green" :count (count-checkins-for-group 144673 checkins) :name "Preschool # 2"}
       {:id 108123 :max 12 :color "blue" :count (count-checkins-for-group 108123 checkins) :name "Primary"}
       {:id 89515 :max 12 :color "purple" :count (count-checkins-for-group 89515 checkins) :name "Elementary"}])))

(defn register-checkin
  "Called by thecity when a new checkin occurs"
  [request env]
  (println (str "web hook was called: " request))
  "registered")
