(ns kids-checkin.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET]]
            [cljsjs.react :as react]
            [kids-checkin.websockets :as ws]))

;; -------------------------
;; State

(defonce checkins (atom {}))

;; -------------------------
;; Views
(defn loader []
  [:div.loader
   [:div {:class "uil-facebook-css blue" :style {:-webkit-transform "scale(0.6)"}}
    [:div]
    [:div]
    [:div]]])


(defn list-classes [checkins]
  [:div
   [:ul
    (for [class checkins]
      ^{:key (:id class)} [:li
                           [:div {:style {:background-color (:color class)}}
                            (str (:name class) " " (:count class) "/" (:max class))]])]])

(defn checkin-page []
  [:div
   [list-classes @checkins]])

(defn current-page []
  (if (empty? @checkins)
    [loader]
    [checkin-page]))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn save-checkins [response]
  (swap! checkins (fn [] response)))

(defn get-checkins []
    (GET "/checkins" {:handler save-checkins}))

(defn update-checkins! [response]
  (swap! checkins (fn [] response)))

(defn init! []
  (get-checkins)
  (ws/make-websocket! "ws://kids-checkin.herokuapp.com/message" update-checkins!)
  (mount-root))
