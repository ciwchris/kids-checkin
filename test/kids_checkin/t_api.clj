(ns kids-checkin.t-api
  (:use midje.sweet)
  (:require [kids-checkin.api :as api]))

(facts "filter by check in date"
  (def all-checkins '({:checked_in_at "06/07/2015 05:57 PM (GMT)"}
                       {:checked_in_at "06/07/2015 05:57 PM (GMT)"}
                       {:checked_in_at "05/31/2015 05:57 PM (GMT)"}))
  (def current-day-checkins '({:checked_in_at "06/07/2015 05:57 PM (GMT)"}
                               {:checked_in_at "06/07/2015 05:57 PM (GMT)"}))
  (fact "it has checkins only for the current day"
    (api/filter-by-date "06/07/2015" all-checkins) => current-day-checkins))

(facts "filter by barcode"
  (def all-checkins {:1 108117})
  (fact "it adds new barcodes"
    (def new-checkins '({:group {:id 108117} :barcode "2"}))
    (def new-all-checkins {:1 108117
                            :2 108117})
    (api/filter-by-barcode all-checkins new-checkins) => new-all-checkins)
  (fact "it does not duplicate barcodes"
    (def new-checkins '({:group {:id 108117} :barcode "1"}))
    (def new-all-checkins '{:1 108117})
    (api/filter-by-barcode all-checkins new-checkins) => new-all-checkins))

(facts "retrieving checkins"
  (def all-checkins {})
  (fact "it retrieves more pages if checkin store increase by checkins per page count"
    (def new-checkins '({:group {:id 108117} :barcode "1" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "2" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "3" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "4" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "5" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "6" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "7" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "8" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "9" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "10" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "11" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "12" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "13" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "14" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "15" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "16" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "17" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "18" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "19" :checked_in_at "06/07/2015 05:57 PM (GMT)"}
                        {:group {:id 108117} :barcode "20" :checked_in_at "06/07/2015 05:57 PM (GMT)"}))
    (def new-all-checkins '{:1 108117 :2 108117 :3 108117 :4 108117 :5 108117 :6 108117 :7 108117 :8 108117 :9 108117 :10 108117
                            :11 108117 :12 108117 :13 108117 :14 108117 :15 108117 :16 108117 :17 108117 :18 108117 :19 108117 :20 108117})
    (api/add-new-checkins-to-checkins-store all-checkins new-checkins "06/07/2015") => new-all-checkins))

;; {:id 108119 :color "orange" :count 1 :max 12 :name "Toddlers"}
(facts "create counts for each group"
  (fact "it increases group count"
    (def groups
      [{:id 108117 :color "red" :count 2 :max 12 :name "Nursery"}
       {:id 108119 :color "orange" :count 0 :max 12 :name "Toddlers"}
       {:id 108120 :color "yellow" :count 0 :max 12 :name "Preschool #1"}
       {:id 144673 :color "green" :count 0 :max 12 :name "Preschool # 2"}
       {:id 108123 :color "blue" :count 0 :max 12 :name "Primary"}
       {:id 89515 :color "purple" :count 1 :max 12 :name "Elementary"}])
    (def checkins {:1 108117 :2 108117 :3 89515})
    (api/create-group-count checkins) => groups))
