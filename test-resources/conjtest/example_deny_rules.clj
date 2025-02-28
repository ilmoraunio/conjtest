(ns conjtest.example-deny-rules)

(defn ^{:rule/type :deny
        :rule/message "port should be 80"}
      deny-my-rule
  [input]
  (and (= "v1" (:apiVersion input))
       (= "Service" (:kind input))
       (not= 80 (-> input :spec :ports first :port))))

(defn ^{:rule/type :deny
        :rule/message "port should be 80"}
      differently-named-deny-rule
  [input]
  (and (= "v1" (:apiVersion input))
       (= "Service" (:kind input))
       (not= 80 (-> input :spec :ports first :port))))

(defn deny-my-bare-rule
  [input]
  (if (and (= "v1" (:apiVersion input))
           (= "Service" (:kind input))
           (not= 80 (-> input :spec :ports first :port)))
    "port should be 80"
    false))

(defn deny-my-absolute-bare-rule
  [input]
  (and (= "v1" (:apiVersion input))
       (= "Service" (:kind input))
       (not= 80 (-> input :spec :ports first :port))))

(def deny-malli-rule
  {:type :deny
   :name "deny-malli-rule"
   :message "port should be 80"
   :rule [:map
          [:apiVersion [:= "v1"]]
          [:kind [:= "Service"]]
          [:spec [:map [:ports [:+ [:map [:port [:not= 80]]]]]]]]})
