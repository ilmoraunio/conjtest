(ns main
  (:require [conjtest.core :as conjtest]
            [pod-ilmoraunio-conjtest.api :as parser]
            [policy]))

(defn test
  [& args]
  (let [inputs (apply parser/parse args)]
    (let [{:keys [summary-report failure-report]}
          (conjtest/test inputs 'policy)]
      (cond
        failure-report (do (println failure-report) (System/exit 1))
        summary-report (do (println summary-report) (System/exit 0))))))

(comment
  "bb test sample_config.edn")
