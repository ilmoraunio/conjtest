(ns json-policy)

(def ^:private denylist
  ["google_iam" "google_container"])

(defn deny-prohibited-resources
  [input]
  (let [matches (for [{:keys [type]} (:resource_changes input)
                      prefix denylist
                      :let [match (clojure.string/starts-with? type prefix)]
                      :when (true? match)]
                  type)]
    (when (not-empty matches)
      (format "Terraform plan will change prohibited resources in the following namespaces: %s" denylist))))