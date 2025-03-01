(ns conjtest.example-allow-rules)

(defn ^{:rule/type :allow
        :rule/message "port should be 80"}
      allow-my-rule
  [input]
  (and (= "v1" (:apiVersion input))
       (= "Service" (:kind input))
       (= 80 (-> input :spec :ports first :port))))

(defn ^{:rule/type :allow
        :rule/message "port should be 80"}
      differently-named-allow-rule
  [input]
  (and (= "v1" (:apiVersion input))
       (= "Service" (:kind input))
       (= 80 (-> input :spec :ports first :port))))

(defn allow-my-bare-rule
  [input]
  (if (and (= "v1" (:apiVersion input))
           (= "Service" (:kind input))
           (= 80 (-> input :spec :ports first :port)))
    true
    "port should be 80"))

(defn allow-my-absolute-bare-rule
  [input]
  (and (= "v1" (:apiVersion input))
       (= "Service" (:kind input))
       (= 80 (-> input :spec :ports first :port))))
