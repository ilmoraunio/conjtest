(ns combined-policy
  (:require [clojure.set]))

(defn deny-missing-path
  [input]
  (let [gke-projects (into #{} (for [{:keys [project]} (get-in input [:provider :google])] project))
        gke-show-project (hash-set (get-in input [:configuration :provider_config :google :expressions :project :constant_value]))]
    (when-not (or (gke-projects "instrumenta")
                  (gke-show-project "instrumenta"))
      "File path index to key value does not exist")))
