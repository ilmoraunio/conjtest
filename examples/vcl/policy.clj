(ns policy)

(defn deny-purge-list
  [input]
  (let [allowlist #{"127.0.0.1" "localhost"}
        purges (get-in input [:acl :purge])]
    (for [purge purges
          :when (not (allowlist purge))]
      (format "acl purge should be one of %s, was %s instead" (into [] allowlist) purge))))

(defn deny-incorrect-port
  [input]
  (when (not= "8080" (get-in input [:backend :app :port]))
    "default backend port should be 8080"))

(def allow-declarative-example
  [:map
   [:acl
    [:map
     [:purge [:enum "127.0.0.1" "localhost"]]]]
   [:backend
    [:map
     [:app
      [:map
       [:port [:= "8080"]]]]]]])