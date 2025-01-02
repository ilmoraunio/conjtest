(ns tf-policy)

(defn deny-no-resources
  [input]
  (when (empty? (get input "resource"))
    (format "could not find any resources in: %s" input)))