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
  (when (not= "https" (get-in input [:server :protocol]))
    "Should use https"))

(defn deny-allow-sign-up
  [input]
  (when (not (false? (get-in input [:users :allow_sign_up])))
    "Users cannot sign up themselves"))

(defn deny-verify-email-disabled
  [input]
  (when (not (true? (get-in input [:users :verify_email_enabled])))
    "Users should verify their e-mail address"))

(def allow-declarative-example
  [:map
   [:alerting [:map [:enabled [:= {:error/message "Alerting should turned on"} true]]]]
   [:auth.basic [:map [:enabled [:= {:error/message "Basic auth should be enabled"} true]]]]
   [:server [:map
             [:http_port [:= {:error/message "Port should be 3000"} 3000.0]]
             [:protocol [:= {:error/message "Should use https"} "https"]]]]
   [:users [:map
            [:allow_sign_up [:= {:error/message "Users cannot sign up themselves"} false]]
            [:verify_email_enabled [:= {:error/message "Users should verify their e-mail address"} false]]]]])