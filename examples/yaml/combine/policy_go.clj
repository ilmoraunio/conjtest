(ns policy-go)

(defn- deployment-matches-service-name?
  [service app]
  (= app (get-in service ["spec" "selector" "app"])))

(defn deny-deployments-with-no-matching-service
  [inputs]
  (let [{services "Service" deployments "Deployment"} (group-by #(get % "kind") inputs)]
    (for [service services
          deployment deployments
          :when (not (deployment-matches-service-name?
                       service
                       (get-in deployment ["spec"
                                           "selector"
                                           "matchLabels"
                                           "app"])))]
      (format "Deployment '%s' has no matching service" (get-in deployment ["metadata" "name"])))))