(ns ilmoraunio.cljconf.example-warn-rules)

(defn ^{:rule/type :warn
        :rule/message "port should be 80"}
      warn-my-rule
  [input]
  (and (= "v1" (get input "apiVersion"))
       (= "Service" (get input "kind"))
       (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

(defn ^{:rule/type :warn
        :rule/message "port should be 80"}
      differently-named-warn-rule
  [input]
  (and (= "v1" (get input "apiVersion"))
       (= "Service" (get input "kind"))
       (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

(defn warn-my-bare-rule
  [input]
  (if (and (= "v1" (get input "apiVersion"))
           (= "Service" (get input "kind"))
           (not= 80.0 (get-in input ["spec" "ports" 0 "port"])))
    "port should be 80"
    false))

(defn warn-my-absolute-bare-rule
  [input]
  (and (= "v1" (get input "apiVersion"))
       (= "Service" (get input "kind"))
       (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

(def warn-malli-rule
  {:type :warn
   :name "warn-malli-rule"
   :message "port should be 80"
   :rule [:map
          ["apiVersion" [:= "v1"]]
          ["kind" [:= "Service"]]
          ["spec" [:map ["ports" [:+ [:map ["port" [:not= 80.0]]]]]]]]})