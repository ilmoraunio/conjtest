(ns policy)

(defn- deployment-matches-service-name?
  [service app]
  (= app (-> service :spec :selector :app)))

(defn deny-deployments-with-no-matching-service
  [inputs]
  (let [{services "Service" deployments "Deployment"} (group-by :kind inputs)]
    (for [service services
          deployment deployments
          :when (not (deployment-matches-service-name?
                       service
                       (get-in deployment [:spec
                                           :selector
                                           :matchLabels
                                           :app])))]
      (format "Deployment '%s' has no matching service" (-> deployment :metadata :name)))))