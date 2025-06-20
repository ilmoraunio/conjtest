(ns policy)

(defn deny-incorrect-log-level-development
  [input]
  (when (and (= :development (:env input))
             (not= :debug (:log input)))
    "Applications in the development environment should have debug logging"))

(defn deny-incorrect-log-level-production
  [input]
  (when (and (= :production (:env input))
             (not= :error (:log input)))
    "Applications in the production environment should have error only logging"))

(def allow-declarative-example
  [:and
   [:map
    [:db
     [:map
      [:user :string]
      [:pwd :string]
      [:host :string]
      [:db :string]
      [:port :int]]]
    [:myapp [:map
             [:port :int]
             [:features [:set
                         [:enum :admin-panel :keyboard-shortcuts]]]
             [:foo
              [:map
               [:hostname :string]
               [:api-keys [:vector :string]]
               [:recheck-frequency :string]]]
             [:forever-date inst?]
             [:process-pool :int]]]
    [:log :keyword]
    [:env [:enum :production]]]
   [:fn {:error/message "Applications in the production environment should have error only logging"}
    (fn [input]
      (and (= :production (:env input))
           (= :error (:log input))))]])