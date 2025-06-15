(ns policy)

(defn deny-empty-app-name
  [input]
  (when (empty? (:APP_NAME input))
    "APP_NAME must be set"))

(defn deny-root-user
  [input]
  (when (= "root" (:MYSQL_USER input))
    "MYSQL_USER should not be root"))

(def allow-declarative-example
  [:map
   [:APP_NAME [:string {:min 1
                        :error/message "APP_NAME must be set"}]]
   [:MYSQL_USER [:not {:error/message "MYSQL_USER should not be root"}
                 [:= "root"]]]])