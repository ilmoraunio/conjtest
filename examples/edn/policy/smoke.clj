(ns smoke)

(defn allow-incorrect-log-level-development
  [input]
  (if (and (= :development (:env input))
           (= :debug (:log input)))
    true
    "Applications in the development environment should have debug logging"))

(defn deny-incorrect-log-level-production
  [input]
  (when (and (= :production (:env input))
             (= :error (:log input)))
        "Applications in the production environment should have error only logging"))