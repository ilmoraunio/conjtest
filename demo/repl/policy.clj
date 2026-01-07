(ns policy)

(defn deny-my-policy
  [input]
  (when ((into #{} (:paths input)) "evil-dir")
    "evil-dir found!"))

(comment
  ;; helper fns for debugging
  (require '[conjtest.core :as conjtest])
  (require '[pod-ilmoraunio-conjtest.api :as api])

  ;; function call passes
  (deny-my-policy (first (vals (api/parse "deps.edn"))))
  ; => nil

  ;; function call fails (when deps.edn contains a bad value)
  (deny-my-policy (update (first (vals (api/parse "deps.edn")))
                          :paths
                          conj
                          "evil-dir"))
  ; => "evil-dir found!"

  ;; conjtest runner fails (non-throwing)
  (conjtest/test [(update (first (vals (api/parse "deps.edn")))
                          :paths
                          conj
                          "evil-dir")] #'deny-my-policy)
  #_{:summary {:total 1, :passed 0, :warnings 0, :failures 1},
     :failure-report "FAIL - null - deny-my-policy - evil-dir found!\n\n1 tests, 0 passed, 0 warnings, 1 failures\n",
     :result ({:message "evil-dir found!", :name "deny-my-policy", :rule-type :deny, :failure? true})}

  ;; conjtest runner fails (exception-throwing => this is what the CLI uses)
  (conjtest/test! [(update (first (vals (api/parse "deps.edn")))
                           :paths
                           conj
                           "evil-dir")] #'deny-my-policy)
  ; clojure.lang.ExceptionInfo: FAIL - null - deny-my-policy - evil-dir found!
  ;
  ; 1 tests, 0 passed, 0 warnings, 1 failures
  )

