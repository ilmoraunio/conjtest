(ns tf-policy)

(defn deny-no-resources
  [input]
  (when (empty? (:resource input))
    (format "could not find any resources in: %s" input)))