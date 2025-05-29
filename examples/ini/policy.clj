(ns policy)

(defn deny-alerting-disabled
  [input]
  (when (not (true? (get-in input [:alerting :enabled])))
    "Alerting should turned on"))

(defn deny-basic-auth-disabled
  [input]
  (when (not (true? (get-in input [:auth.basic :enabled])))
    "Basic auth should be enabled"))

(defn deny-incorrect-port
  [input]
  (when (not= 3000.0 (get-in input [:server :http_port]))
    "Grafana port should be 3000"))

(defn deny-server-protocol
  [input]
  (when (not= "http" (get-in input [:server :protocol]))
    "Grafana should use default http"))

(defn deny-allow-sign-up
  [input]
  (when (not (false? (get-in input [:users :allow_sign_up])))
    "Users cannot sign up themselves"))

(defn deny-verify-email-disabled
  [input]
  (when (not (true? (get-in input [:users :verify_email_enabled])))
    "Users should verify their e-mail address"))