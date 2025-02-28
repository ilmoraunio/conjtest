(ns conjtest.example-warn-rules)

(defn ^{:rule/type :warn
        :rule/message "port should be 80"}
      warn-my-rule
  [input]
  (and (= "v1" (:apiVersion input))
       (= "Service" (:kind input))
       (not= 80 (-> input :spec :ports first :port))))

(defn ^{:rule/type :warn
        :rule/message "port should be 80"}
      differently-named-warn-rule
  [input]
  (and (= "v1" (:apiVersion input))
       (= "Service" (:kind input))
       (not= 80 (-> input :spec :ports first :port))))

(defn warn-my-bare-rule
  [input]
  (if (and (= "v1" (:apiVersion input))
           (= "Service" (:kind input))
           (not= 80 (-> input :spec :ports first :port)))
    "port should be 80"
    false))

(defn warn-my-absolute-bare-rule
  [input]
  (and (= "v1" (:apiVersion input))
       (= "Service" (:kind input))
       (not= 80 (-> input :spec :ports first :port))))

(def warn-malli-rule
  {:type :warn
   :name "warn-malli-rule"
   :message "port should be 80"
   :rule [:map
          [:apiVersion [:= "v1"]]
          [:kind [:= "Service"]]
          [:spec [:map [:ports [:+ [:map [:port [:not= 80]]]]]]]]})
