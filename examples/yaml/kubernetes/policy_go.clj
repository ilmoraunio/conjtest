(ns policy-go)

(defn- deployment?
  [input]
  (= "Deployment" (:kind input)))

(defn deny-should-not-run-as-root
  [input]
  (let [name (get-in input [:metadata :name])]
    (when (and (deployment? input)
               (not (true? (get-in input [:spec
                                          :template
                                          :spec
                                          :securityContext
                                          :runAsNonRoot]))))
      (format "Containers must not run as root in Deployment \"%s\"" name))))

(defn- contains-required-deployment-selectors?
  [input]
  (let [match-labels (get-in input [:spec :selector :matchLabels])]
    (and (:app match-labels)
         (:release match-labels))))

(defn deny-missing-required-deployment-selectors
  [input]
  (let [name (get-in input [:metadata :name])]
    (when (and (deployment? input)
               (not (contains-required-deployment-selectors? input)))
      (format "Deployment \"%s\" must provide app/release labels for pod selectors" name))))