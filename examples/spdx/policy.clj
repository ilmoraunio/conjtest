(ns policy)

(defn deny-incorrect-license
  [input]
  (let [expected-data-license "correct-license"
        actual-data-license (:dataLicense input)]
    (when-not (= expected-data-license actual-data-license)
      (format "DataLicense should be '%s', but found '%s'"
              expected-data-license
              actual-data-license))))