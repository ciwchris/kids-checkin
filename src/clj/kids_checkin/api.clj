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

(defn retrieve-checkins []
  (let [url (str "https://api.onthecity.org/checkins")
        headers (create-headers url)]
    (client/get url {:headers headers})))

(defn count-checkins [id checkins]
  (count (filter #(= id (get-in % [:group :id])) checkins)))
(defn test-checkins []
    {:144673 {:count 1, :name "Preschool # 2 (Green)"}, :108119 {:count 1, :name "Toddlers (Orange)"}, :108117 {:count 1, :name "Nursery (Red)"}, :108123 {:count 1, :name "Primary (Blue)"}, :89515 {:count 1, :name "Elementary (Purple)"}, :108120 {:count 1, :name "Preschool #1 (Yellow)"}})

(defn create-checkin-map [env]
  (if (true? (:dev env))
    (test-checkins)
    (let [body (:body (retrieve-checkins))
          checkins (:checkins (json/read-str body :key-fn keyword))]
      {
       :144673 {:count (count-checkins 144673 checkins) :name "Preschool # 2 (Green)"}
       :108119 {:count (count-checkins 108119 checkins) :name "Toddlers (Orange)"}
       :108117 {:count (count-checkins 108117 checkins) :name "Nursery (Red)"}
       :108123 {:count (count-checkins 108123 checkins) :name "Primary (Blue)"}
       :89515 {:count (count-checkins 89515 checkins) :name "Elementary (Purple)"}
       :108120 {:count (count-checkins 108120 checkins) :name "Preschool #1 (Yellow)"}})))
