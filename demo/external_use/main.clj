(ns main
  (:require [ilmoraunio.cljconf.core :as cljconf]
            [pod-ilmoraunio-conftest-clj.api :as parser]
            [policy]))

(defn test
  [& args]
  (let [inputs (apply parser/parse args)]
    (clojure.pprint/pprint
      (cljconf/test inputs
                    #'policy/deny-incorrect-log-level-development
                    #'policy/deny-incorrect-log-level-production))))