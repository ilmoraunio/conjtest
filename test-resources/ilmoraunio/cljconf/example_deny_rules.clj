(ns ilmoraunio.cljconf.example-deny-rules)

(defn ^{:rule/type :deny
        :rule/message "port should be 80"}
      deny-my-rule
  [input]
  (and (= "v1" (get input "apiVersion"))
       (= "Service" (get input "kind"))
       (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

(defn ^{:rule/type :deny
        :rule/message "port should be 80"}
      differently-named-deny-rule
  [input]
  (and (= "v1" (get input "apiVersion"))
       (= "Service" (get input "kind"))
       (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

(defn deny-my-bare-rule
  [input]
  (if (and (= "v1" (get input "apiVersion"))
           (= "Service" (get input "kind"))
           (not= 80.0 (get-in input ["spec" "ports" 0 "port"])))
    "port should be 80"
    false))

(defn deny-my-absolute-bare-rule
  [input]
  (and (= "v1" (get input "apiVersion"))
       (= "Service" (get input "kind"))
       (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

(def deny-malli-rule
  {:type :deny
   :name "deny-malli-rule"
   :message "port should be 80"
   :rule [:map
          ["apiVersion" [:= "v1"]]
          ["kind" [:= "Service"]]
          ["spec" [:map ["ports" [:+ [:map ["port" [:not= 80.0]]]]]]]]})