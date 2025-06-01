(ns policy-go)

(defn deny-incorrect-log-level-development
  [input]
  (when (and (= ":development" (:env input))
             (not= ":debug" (:log input)))
    "Applications in the development environment should have debug logging"))

(defn deny-incorrect-log-level-production
  [input]
  (when (and (= ":production" (:env input))
             (not= ":error" (:log input)))
    "Applications in the production environment should have error only logging"))