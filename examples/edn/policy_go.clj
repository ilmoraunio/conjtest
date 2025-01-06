(ns policy-go)

(defn deny-incorrect-log-level-development
  [input]
  (when (and (= ":development" (get input ":env"))
             (not= ":debug" (get input ":log")))
    "Applications in the development environment should have debug logging"))

(defn deny-incorrect-log-level-production
  [input]
  (when (and (= ":production" (get input ":env"))
             (not= ":error" (get input ":log")))
    "Applications in the production environment should have error only logging"))