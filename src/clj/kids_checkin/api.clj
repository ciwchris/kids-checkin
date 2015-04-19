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

;;{"barcode\":\"6\",\"pager_number\":null,\"checked_in_user_notes\":\"507\",\"special_instructions\":\"\",\"group\":{\"external_id\":\"\",\"name\":\"Preschool #1 (Yellow)\",\"id\":108120},\"checked_in_by_user\":\"N\",\"checked_in_by_user_id\":4,\"event\":{\"title\":\"Sunday Gathering\",\"starting_at\":\"04/12/2015 04:00 PM (GMT)\",\"ending_at\":\"04/12/2015 06:30 PM (GMT)\",\"id\":4},\"parent_group_name\":\"Soma KIDS\",\"parent_group_id\":2603,\"checked_out_by_user\":null,\"checked_in_user_id\":5,\"parent_receipt_barcode\":\"D\",\"callboard_number\":\"141\",\"checked_in_user\":\"A Harden\",\"id\":6,\"checked_out_at\":null,\"checked_in_at\":\"04/12/2015 05:33 PM (GMT)\"},
(defn load-config []
  (edn/read-string (slurp "src/clj/config.edn")))

(def groups {
             :144673 {:count 0 :name "Preschool # 2 (Green)"}
             :108119 {:count 0 :name "Toddlers (Orange)"}
             :108117 {:count 0 :name "Nursery (Red)"}
             :108123 {:count 0 :name "Primary (Blue)"}
             :89515 {:count 0 :name "Elementary (Purple)"}
             :108120 {:count 0 :name "Preschool #1 (Yellow)"}})

;; (require '[base64-clj.core :as base64])
(defn secretKeyInst [key mac]
  (SecretKeySpec. (.getBytes key) (.getAlgorithm mac)))

(defn toHexString [bytes]
  "Convert bytes to a String"
  (apply str (map #(format "%02x" %) bytes)))

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

(defn test-sign [action]
  (let [url (str "https://api.onthecity.org/" action)
        headers (create-headers url)]
    (client/get url {:headers headers})))

(defn kids []
  (let [url (str "https://api.onthecity.org/checkins")
        headers (create-headers url)]
    (client/get url {:headers headers})))

(defn read-kid [kid groups]
  (let [kid-group (:group kid)
        group-id (keyword (str (:id kid-group)))
        group-data (group-id groups)
        current-count (:count group-data)
        ]
    (assoc groups group-id (assoc group-data :count (+ current-count 1)))))

(defn list-kids
  ([] (let [body (:body (kids))
            checkins (:checkins (json/read-str body :key-fn keyword))]
        (list-kids groups checkins)))
  ([groups checkins]
   (if (empty? checkins)
     groups
     (list-kids (read-kid (first checkins) groups) (rest checkins)))))

(defn count-kids [id checkins]
  (count (filter #(= id (get-in % [:group :id])) checkins)))

(defn alt-list-kids []
  (let [body (:body (kids))
        checkins (:checkins (json/read-str body :key-fn keyword))]
    {
     :144673 {:count (count-kids 144673 checkins) :name "Preschool # 2 (Green)"}
     :108119 {:count (count-kids 108119 checkins) :name "Toddlers (Orange)"}
     :108117 {:count (count-kids 108117 checkins) :name "Nursery (Red)"}
     :108123 {:count (count-kids 108123 checkins) :name "Primary (Blue)"}
     :89515 {:count (count-kids 89515 checkins) :name "Elementary (Purple)"}
     :108120 {:count (count-kids 108120 checkins) :name "Preschool #1 (Yellow)"}}))
