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

(defn load-config []
  (edn/read-string (slurp "src/clj/config.edn")))

;; (require '[base64-clj.core :as base64])
(defn secretKeyInst [key mac]
  (SecretKeySpec. (.getBytes key) (.getAlgorithm mac)))

(defn sign [key string]
  "Returns the signature of a string with a given
    key, using a SHA-256 HMAC."
  (let [mac (Mac/getInstance "HMACSHA256")
        secretKey (secretKeyInst key mac)]
    (-> (doto mac
          (.init secretKey)
          (.update (.getBytes string)))
        .doFinal)))

(defn formatted-sig [text-to-sign key]
  (->> text-to-sign
       (sign key)
       (r/base64-encode)
       (r/form-encode)))

(defn create-headers [url]
  (let [config (load-config)
        unix-time (tc/to-long (time/now))
        string-to-sign (str unix-time "GET" url)
        signed-string (formatted-sig string-to-sign (:key config))]
        {"X-City-Sig" signed-string
         "X-City-User-Token" (:user-token config)
         "X-City-Time" (str unix-time)
         "Accept" "application/vnd.thecity.admin.v1+json"
         }))

(defn retrieve-checkins-page [page-number]
  (let [url (str "https://api.onthecity.org/checkins?page=" page-number)
         headers (create-headers url)]
     (client/get url {:headers headers})))

(defn get-starting-date [checkin]
  (apply str (take 10 (get-in checkin [:event :starting_at]))))

(defn retrieve-new-checkins [checkins]
  (take-while #(not= (:barcode %) (:barcode (first @checkins-store))) checkins))

(defn retrieve-checkins
  ([] (retrieve-checkins 1 []))
  ([page-number checkins]
   (let [today (.toString (time/today) "MM/dd/yyyy")
         body (:body (retrieve-checkins-page page-number))
         page-checkins (:checkins (json/read-str body :key-fn keyword))
         last-checkin-date (get-starting-date (last page-checkins))
         new-checkins (retrieve-new-checkins page-checkins)]
     (if (and (= 20 (count new-checkins)) (= today last-checkin-date))
       (retrieve-checkins (inc page-number) (into checkins new-checkins))
       (let [complete-checkin-list (into checkins (filter #(= today (get-starting-date %)) new-checkins))]
         (swap! checkins-store into complete-checkin-list)
         @checkins-store)))))

(defn count-checkins [id checkins]
  (count (filter #(= id (get-in % [:group :id])) checkins)))

(defn test-checkins []
  [{:id 108117 :color "red" :count 1 :max 12 :name "Nursery"}
   {:id 108119 :color "orange" :count 1 :max 12 :name "Toddlers"}
   {:id 108120 :color "yellow" :count 1 :max 12 :name "Preschool #1"}
   {:id 144673 :color "green" :count 1 :max 12 :name "Preschool # 2"}
   {:id 108123 :color "blue" :count 1 :max 12 :name "Primary"}
   {:id 89515 :color "purple" :count 1 :max 12 :name "Elementary"}])

(defn create-checkin-map [env]
  (if (true? (:dev env))
    (test-checkins)
    (let [checkins (retrieve-checkins)]
      [{:id 108117 :max 12 :color "red" :count (count-checkins 108117 checkins) :name "Nursery"}
       {:id 108119 :max 12 :color "orange" :count (count-checkins 108119 checkins) :name "Toddlers"}
       {:id 108120 :max 12 :color "yellow" :count (count-checkins 108120 checkins) :name "Preschool #1"}
       {:id 144673 :max 12 :color "green" :count (count-checkins 144673 checkins) :name "Preschool # 2"}
       {:id 108123 :max 12 :color "blue" :count (count-checkins 108123 checkins) :name "Primary"}
       {:id 89515 :max 12 :color "purple" :count (count-checkins 89515 checkins) :name "Elementary"}])))
