(ns kids-checkin.handler
  (:require [compojure.core :refer [GET POST defroutes routes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [kids-checkin.api :as api]
            [selmer.parser :refer [render-file]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [with-channel on-close send!]]))

(defn ws
  [req]
  (with-channel req con
    (swap! api/clients assoc con true)
    (println con " connected")
    (on-close con (fn [status]
                    (swap! api/clients dissoc con)
                    (println con " disconnected. status: " status)))))

(defroutes socket-routes
  (GET "/message" [] ws))

(defroutes site-routes
(GET "/message-j" [] ws)
  (GET "/" [] (render-file "templates/index.html" {:dev (env :dev?)}))
  (GET "/checkins" [] {:status 200
                       :body (api/create-list-of-checkin-count-by-group {:dev (env :dev?)})})
  (POST "/newcheckin" request {:status 200
                               :body (api/register-checkin request {:dev (env :dev?)})})
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [site-defaults-no-anti-forgery (assoc-in site-defaults [:security :anti-forgery] false)]
    (routes
      (-> socket-routes
          (wrap-defaults site-defaults-no-anti-forgery))
      (-> site-routes
          (wrap-restful-format :formats [:transit-json])))))

;;  (let [site-defaults-no-anti-forgery (assoc-in site-defaults [:security :anti-forgery] false)
;;        site-handler (wrap-defaults routes site-defaults-no-anti-forgery)
;;        handler (wrap-restful-format site-handler :formats [:transit-json])
;;        socket-handler (wrap-defaults socket-routes site-defaults)]
;;    (if (env :dev?) (wrap-exceptions (routes socket-handler handler)) (routes socket-handler handler)))
