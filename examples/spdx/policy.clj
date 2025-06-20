(ns policy)

(defn deny-incorrect-license
  [input]
  (let [expected-data-license "correct-license"
        actual-data-license (:dataLicense input)]
    (when-not (= expected-data-license actual-data-license)
      (format "Data license should be '%s', but found '%s'"
              expected-data-license
              actual-data-license))))

(def allow-declarative-example
  [:map
   [:dataLicense [:= {:error/fn (fn [{:keys [value]} _]
                                  (format "Data license should be 'correct-license', but found '%s'" value))}
                  "correct-license"]]])