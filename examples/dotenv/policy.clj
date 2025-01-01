(ns policy)

(defn deny-empty-app-name
  [input]
  (when (empty? (get input "APP_NAME"))
    "APP_NAME must be set"))

(defn deny-root-user
  [input]
  (when (= "root" (get input "MYSQL_USER"))
    "MYSQL_USER should not be root"))