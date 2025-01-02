(ns combined-policy)

(defn deny-missing-path
  [input]
  (when (not= "instrumenta" (or (get-in input ["provider" "google" "project"])
                                (get-in input [:configuration :provider_config :google :expressions :project :constant_value])))
    "File path index to key value does not exist"))
