(ns policy)

(defn- deployment-matches-service-name?
  [service app]
  (= app (get-in service ["spec" "selector" "app"])))

(defn deny-deployments-with-no-matching-service
  [inputs]
  (let [{[service] "Service" deployments "Deployment"} (group-by #(get % "kind") inputs)]
    (when-let [non-matching-deployments (seq (remove (fn [deployment]
                                                       (deployment-matches-service-name?
                                                         service
                                                         (get-in deployment ["spec"
                                                                             "selector"
                                                                             "matchLabels"
                                                                             "app"])))
                                                     deployments))]
      (format "Deployments [%s] have no matching service"
              (clojure.string/join ", " (map (comp (partial format "'%s'")
                                                   #(get-in % ["metadata" "name"]))
                                             non-matching-deployments))))))