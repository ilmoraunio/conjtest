(ns ilmoraunio.cljconf.example-allow-rules)

(defn ^{:rule/type :allow
        :rule/message "port should be 80"}
      allow-my-rule
  [input]
  (and (= "v1" (get input "apiVersion"))
       (= "Service" (get input "kind"))
       (= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

(defn ^{:rule/type :allow
        :rule/message "port should be 80"}
      differently-named-allow-rule
  [input]
  (and (= "v1" (get input "apiVersion"))
       (= "Service" (get input "kind"))
       (= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

(defn allow-my-bare-rule
  [input]
  (if (and (= "v1" (get input "apiVersion"))
           (= "Service" (get input "kind"))
           (= 80.0 (get-in input ["spec" "ports" 0 "port"])))
    true
    "port should be 80"))

(defn allow-my-absolute-bare-rule
  [input]
  (and (= "v1" (get input "apiVersion"))
       (= "Service" (get input "kind"))
       (= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

(def allow-malli-rule
  {:type :allow
   :name "allow-malli-rule"
   :message "port should be 80"
   :rule [:map
          ["apiVersion" [:= "v1"]]
          ["kind" [:= "Service"]]
          ["spec" [:map ["ports" [:+ [:map ["port" [:= 80.0]]]]]]]]})