(ns kids-checkin.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [kids-checkin.api :as api]
            [selmer.parser :refer [render-file]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]))


(defroutes routes
  (GET "/" [] (render-file "templates/index.html" {:dev (env :dev?)}))
  (GET "/checkins" [] {:status 200
                       :body (api/create-checkin-map {:dev (env :dev?)})})
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-restful-format (wrap-defaults routes site-defaults) :formats [:transit-json])]
    (if (env :dev?) (wrap-exceptions handler) handler)))
