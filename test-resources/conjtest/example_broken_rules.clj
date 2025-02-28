(ns conjtest.example-broken-rules)

(defn deny-will-trigger-with-exception
  [input]
  (clojure.string/starts-with? (:nilly-field input) "foo"))

(defn deny-will-trigger-cleanly
  [input]
  true)

(defn deny-will-not-trigger
  [input]
  false)

(def deny-broken-malli-rule
  {:type :deny
   :name "deny-broken-malli-rule"
   :message "port should be 80"
   :rule [:mapxxx
          ["apiVersion" [:= "v1"]]
          ["kind" [:= "Service"]]
          ["spec" [:map ["ports" [:+ [:map ["port" [:not= 80.0]]]]]]]]})
