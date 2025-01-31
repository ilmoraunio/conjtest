(ns main
  (:require [cljconf.core :as cljconf]
            [pod-ilmoraunio-cljconf.api :as parser]
            [policy]))

(defn test
  [& args]
  (let [inputs (apply parser/parse args)]
    (let [{:keys [summary-report failure-report]}
          (cljconf/test inputs
                        #'policy/deny-incorrect-log-level-development
                        #'policy/deny-incorrect-log-level-production)]
      (cond
        failure-report (do (println failure-report) (System/exit 1))
        summary-report (do (println summary-report) (System/exit 0))))))
