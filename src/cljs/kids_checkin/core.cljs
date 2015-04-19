(ns kids-checkin.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET]]
            [cljsjs.react :as react]))

;; -------------------------
;; State

(def checkins (atom {}))

(def timer-max 6)

(def timer-count (atom timer-max))

;; -------------------------
;; Views

(defn loader []
  [:div "Loading..."])

(defn list-classes [checkins]
  [:div
   [:ul
    (for [class checkins]
      ^{:key (:id class)} [:li
                           [:div {:style {:background-color (:color class)}}
                            (str (:name class) " " (:count class) "/" (:max class))]])]])

(defn timer-boxes [timer-count boxes]
  [:div
   (for [box boxes]
     ^{:key box} [:div {:class (str "box" (if (< box timer-count) " full-box"))}])])

(defn checkin-page []
  [:div
   [list-classes @checkins]
   [timer-boxes @timer-count (range timer-max)]])

(defn current-page []
  (if (empty? @checkins)
    [loader]
    [checkin-page]))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn set-page-refresh []
  (swap! timer-count (fn [] (mod (inc @timer-count) (+ 1 timer-max))))
  (js/setTimeout #(get-checkins) 10000))

(defn save-checkins [response]
  (swap! checkins (fn [] response))
  (set-page-refresh))

(defn get-checkins []
  (if (<= timer-max @timer-count)
    (GET "/checkins"
         {:handler save-checkins})
    (set-page-refresh)))

(defn init! []
  (get-checkins)
  (mount-root))
