(ns cljconf.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [flatland.ordered.map :refer [ordered-map]]
            [cljconf.core :as cljconf]
            [cljconf.example-allow-rules]
            [cljconf.example-deny-rules]
            [cljconf.example-warn-rules]))

(set! *data-readers* {'ordered/map #'flatland.ordered.map/ordered-map})

(def valid-yaml
  {"test-resources/test.yaml" #ordered/map([:apiVersion "v1"]
                                           [:kind "Service"]
                                           [:metadata #ordered/map([:name "hello-kubernetes"])]
                                           [:spec
                                            #ordered/map([:type "LoadBalancer"]
                                                         [:ports (#ordered/map([:port 80] [:targetPort 8080]))]
                                                         [:selector #ordered/map([:app "hello-kubernetes"])])])})

(def invalid-yaml
  {"test-resources/test.yaml" #ordered/map([:apiVersion "v1"]
                                           [:kind "Service"]
                                           [:metadata #ordered/map([:name "hello-kubernetes"])]
                                           [:spec
                                            #ordered/map([:type "LoadBalancer"]
                                                         [:ports (#ordered/map([:port 9999] [:targetPort 8080]))]
                                                         [:selector #ordered/map([:app "hello-kubernetes"])])])})

(deftest rules-test
  (testing "allow rules"
    (testing "triggered"
      (is (= {:summary {:total 5, :passed 0, :warnings 0, :failures 5}
              :result {"test-resources/test.yaml" [{:message "port should be 80"
                                                    :name "allow-malli-rule"
                                                    :rule-type :allow
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed
                                                    :name "allow-my-absolute-bare-rule"
                                                    :rule-type :allow
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "allow-my-bare-rule"
                                                    :rule-type :allow
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "allow-my-rule"
                                                    :rule-type :allow
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "differently-named-allow-rule"
                                                    :rule-type :allow
                                                    :failure? true}]}}
             (-> (cljconf/test invalid-yaml
                               #'cljconf.example-allow-rules/allow-my-rule
                               #'cljconf.example-allow-rules/differently-named-allow-rule
                               #'cljconf.example-allow-rules/allow-my-bare-rule
                               #'cljconf.example-allow-rules/allow-my-absolute-bare-rule
                               #'cljconf.example-allow-rules/allow-malli-rule)
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 5, :passed 5, :warnings 0, :failures 0}
              :result {"test-resources/test.yaml" [{:message nil,
                                                    :name "allow-malli-rule",
                                                    :rule-type :allow,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-absolute-bare-rule",
                                                    :rule-type :allow,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-bare-rule",
                                                    :rule-type :allow,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "differently-named-allow-rule",
                                                    :rule-type :allow,
                                                    :failure? false}]}}
             (-> (cljconf/test valid-yaml
                               #'cljconf.example-allow-rules/allow-my-rule
                               #'cljconf.example-allow-rules/differently-named-allow-rule
                               #'cljconf.example-allow-rules/allow-my-bare-rule
                               #'cljconf.example-allow-rules/allow-my-absolute-bare-rule
                               #'cljconf.example-allow-rules/allow-malli-rule)
                 (select-keys [:result :summary]))))))
  (testing "deny rules"
    (testing "triggered"
      (is (= {:summary {:total 5, :passed 0, :warnings 0, :failures 5}
              :result {"test-resources/test.yaml" [{:message "port should be 80"
                                                    :name "deny-malli-rule"
                                                    :rule-type :deny
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed
                                                    :name "deny-my-absolute-bare-rule"
                                                    :rule-type :deny
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "deny-my-bare-rule"
                                                    :rule-type :deny
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "deny-my-rule"
                                                    :rule-type :deny
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "differently-named-deny-rule"
                                                    :rule-type :deny
                                                    :failure? true}]}}
             (-> (cljconf/test invalid-yaml
                               #'cljconf.example-deny-rules/deny-my-rule
                               #'cljconf.example-deny-rules/differently-named-deny-rule
                               #'cljconf.example-deny-rules/deny-my-bare-rule
                               #'cljconf.example-deny-rules/deny-my-absolute-bare-rule
                               #'cljconf.example-deny-rules/deny-malli-rule)
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 5, :passed 5, :warnings 0, :failures 0}
              :result {"test-resources/test.yaml" [{:message nil,
                                                    :name "deny-malli-rule",
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-absolute-bare-rule",
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-bare-rule",
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "differently-named-deny-rule",
                                                    :rule-type :deny,
                                                    :failure? false}]}}
             (-> (cljconf/test valid-yaml
                               #'cljconf.example-deny-rules/deny-my-rule
                               #'cljconf.example-deny-rules/differently-named-deny-rule
                               #'cljconf.example-deny-rules/deny-my-bare-rule
                               #'cljconf.example-deny-rules/deny-my-absolute-bare-rule
                               #'cljconf.example-deny-rules/deny-malli-rule)
                 (select-keys [:result :summary]))))))
  (testing "warn rules"
    (testing "triggered"
      (is (= {:summary {:total 5, :passed 0, :warnings 5, :failures 0}
              :result {"test-resources/test.yaml" [{:message "port should be 80"
                                                    :name "differently-named-warn-rule"
                                                    :rule-type :warn
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "warn-malli-rule"
                                                    :rule-type :warn
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed
                                                    :name "warn-my-absolute-bare-rule"
                                                    :rule-type :warn
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "warn-my-bare-rule"
                                                    :rule-type :warn
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "warn-my-rule"
                                                    :rule-type :warn
                                                    :failure? true}]}}
             (-> (cljconf/test invalid-yaml
                               #'cljconf.example-warn-rules/warn-my-rule
                               #'cljconf.example-warn-rules/differently-named-warn-rule
                               #'cljconf.example-warn-rules/warn-my-bare-rule
                               #'cljconf.example-warn-rules/warn-my-absolute-bare-rule
                               #'cljconf.example-warn-rules/warn-malli-rule)
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 5, :passed 5, :warnings 0, :failures 0}
              :result {"test-resources/test.yaml" [{:message nil,
                                                    :name "differently-named-warn-rule",
                                                    :rule-type :warn,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-malli-rule",
                                                    :rule-type :warn,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-absolute-bare-rule",
                                                    :rule-type :warn,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-bare-rule",
                                                    :rule-type :warn,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-rule",
                                                    :rule-type :warn,
                                                    :failure? false}]}}
             (-> (cljconf/test valid-yaml
                               #'cljconf.example-warn-rules/warn-my-rule
                               #'cljconf.example-warn-rules/differently-named-warn-rule
                               #'cljconf.example-warn-rules/warn-my-bare-rule
                               #'cljconf.example-warn-rules/warn-my-absolute-bare-rule
                               #'cljconf.example-warn-rules/warn-malli-rule)
                 (select-keys [:result :summary]))))))
  (testing "resolve functions via namespace"
    (testing "triggered"
      (is (= {:summary {:total 15, :passed 0, :warnings 5, :failures 10}
              :result {"test-resources/test.yaml" [{:message "port should be 80",
                                                    :name "allow-malli-rule",
                                                    :rule-type :allow,
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed,
                                                    :name "allow-my-absolute-bare-rule",
                                                    :rule-type :allow,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "allow-my-bare-rule",
                                                    :rule-type :allow,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "differently-named-allow-rule",
                                                    :rule-type :allow,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "deny-malli-rule",
                                                    :rule-type :deny,
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed,
                                                    :name "deny-my-absolute-bare-rule",
                                                    :rule-type :deny,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "deny-my-bare-rule",
                                                    :rule-type :deny,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "differently-named-deny-rule",
                                                    :rule-type :deny,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "differently-named-warn-rule",
                                                    :rule-type :warn,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "warn-malli-rule",
                                                    :rule-type :warn,
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed,
                                                    :name "warn-my-absolute-bare-rule",
                                                    :rule-type :warn,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "warn-my-bare-rule",
                                                    :rule-type :warn,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "warn-my-rule",
                                                    :rule-type :warn,
                                                    :failure? true}]}}
             (-> (cljconf/test invalid-yaml
                               'cljconf.example-allow-rules
                               'cljconf.example-deny-rules
                               'cljconf.example-warn-rules)
                 (select-keys [:result :summary]))
             (-> (cljconf/test invalid-yaml
                               (the-ns 'cljconf.example-allow-rules)
                               (the-ns 'cljconf.example-deny-rules)
                               (the-ns 'cljconf.example-warn-rules))
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 15, :passed 15, :warnings 0, :failures 0}
              :result {"test-resources/test.yaml" [{:message nil,
                                                    :name "allow-malli-rule",
                                                    :rule-type :allow,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-absolute-bare-rule",
                                                    :rule-type :allow,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-bare-rule",
                                                    :rule-type :allow,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "differently-named-allow-rule",
                                                    :rule-type :allow,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-malli-rule",
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-absolute-bare-rule",
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-bare-rule",
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "differently-named-deny-rule",
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "differently-named-warn-rule",
                                                    :rule-type :warn,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-malli-rule",
                                                    :rule-type :warn,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-absolute-bare-rule",
                                                    :rule-type :warn,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-bare-rule",
                                                    :rule-type :warn,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-rule",
                                                    :rule-type :warn,
                                                    :failure? false}]}}
             (-> (cljconf/test valid-yaml
                               'cljconf.example-allow-rules
                               'cljconf.example-deny-rules
                               'cljconf.example-warn-rules)
                 (select-keys [:result :summary]))
             (-> (cljconf/test valid-yaml
                               (the-ns 'cljconf.example-allow-rules)
                               (the-ns 'cljconf.example-deny-rules)
                               (the-ns 'cljconf.example-warn-rules))
                 (select-keys [:result :summary]))))))
  (testing "anonymous functions"
    (testing "triggered"
      (is (= {:summary {:total 4, :passed 0, :warnings 1, :failures 3}
              :result {"test-resources/test.yaml" [{:message "port should be 80",
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed,
                                                    :name nil,
                                                    :rule-type :deny,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "warn-my-rule",
                                                    :rule-type :warn,
                                                    :failure? true}]}}
             (-> (cljconf/test invalid-yaml
                               ^{:rule/type :allow
                                 :rule/name :allow-my-rule
                                 :rule/message "port should be 80"}
                               (fn [input]
                                 (and (= "v1" (:apiVersion input))
                                      (= "Service" (:kind input))
                                      (= 80 (-> input :spec :ports first :port))))

                               ^{:rule/type :deny
                                 :rule/name :deny-my-rule
                                 :rule/message "port should be 80"}
                               (fn [input]
                                 (and (= "v1" (:apiVersion input))
                                      (= "Service" (:kind input))
                                      (not= 80 (-> input :spec :ports first :port))))

                               ;; "plain" anonymous functions (with no overriding metadata) are implicitly deny rules
                               (fn [input]
                                 (and (= "v1" (:apiVersion input))
                                      (= "Service" (:kind input))
                                      (not= 80 (-> input :spec :ports first :port))))

                               ^{:rule/type :warn
                                 :rule/name :warn-my-rule
                                 :rule/message "port should be 80"}
                               (fn [input]
                                 (and (= "v1" (:apiVersion input))
                                      (= "Service" (:kind input))
                                      (not= 80 (-> input :spec :ports first :port)))))
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 4, :passed 4, :warnings 0, :failures 0}
              :result {"test-resources/test.yaml" [{:message nil,
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name nil,
                                                    :rule-type :deny,
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-rule",
                                                    :rule-type :warn,
                                                    :failure? false}]}}
             (-> (cljconf/test valid-yaml
                               ^{:rule/type :allow
                                 :rule/name :allow-my-rule
                                 :rule/message "port should be 80"}
                               (fn [input]
                                 (and (= "v1" (:apiVersion input))
                                      (= "Service" (:kind input))
                                      (= 80 (-> input :spec :ports first :port))))

                               ^{:rule/type :deny
                                 :rule/name :deny-my-rule
                                 :rule/message "port should be 80"}
                               (fn [input]
                                 (and (= "v1" (:apiVersion input))
                                      (= "Service" (:kind input))
                                      (not= 80 (-> input :spec :ports first :port))))

                               (fn [input]
                                 (and (= "v1" (:apiVersion input))
                                      (= "Service" (:kind input))
                                      (not= 80 (-> input :spec :ports first :port))))

                               ^{:rule/type :warn
                                 :rule/name :warn-my-rule
                                 :rule/message "port should be 80"}
                               (fn [input]
                                 (and (= "v1" (:apiVersion input))
                                      (= "Service" (:kind input))
                                      (not= 80 (-> input :spec :ports first :port)))))
                 (select-keys [:result :summary])))))
    (testing "rule/type affect rule evaluation"
      (testing "anonymous functions are deny rules by default"
        (let [deny-rule (fn [input]
                          (and (= "v1" (:apiVersion input))
                               (= "Service" (:kind input))
                               (not= 80 (-> input :spec :ports first :port))))]
          (is (= {:summary {:total 1, :passed 0, :warnings 0, :failures 1},
                  :failure-report "FAIL - test-resources/test.yaml - :cljconf/rule-validation-failed\n\n1 tests, 0 passed, 0 warnings, 1 failures\n"
                  :result {"test-resources/test.yaml" [{:message :cljconf/rule-validation-failed,
                                                        :name nil,
                                                        :rule-type :deny,
                                                        :failure? true}]}}
                 (cljconf/test invalid-yaml deny-rule)
                 (cljconf/test invalid-yaml {:rule deny-rule})))))
      (testing "you can instruct anonymous functions to be allow-based rules"
        (let [allow-rule ^{:rule/type :allow} (fn [input]
                                                (and (= "v1" (get input "apiVersion"))
                                                     (= "Service" (get input "kind"))
                                                     (= 80.0 (get-in input ["spec" "ports" 0 "port"]))))]
          (is (= {:summary {:total 1, :passed 0, :warnings 0, :failures 1},
                  :failure-report "FAIL - test-resources/test.yaml - :cljconf/rule-validation-failed\n\n1 tests, 0 passed, 0 warnings, 1 failures\n"
                  :result {"test-resources/test.yaml" [{:message :cljconf/rule-validation-failed,
                                                        :name nil,
                                                        :rule-type :allow,
                                                        :failure? true}]}}
                 (cljconf/test invalid-yaml allow-rule)
                 (cljconf/test invalid-yaml {:type :allow :rule allow-rule})))))
      (testing "you can instruct anonymous functions to be warn-based rules"
        (let [warn-rule ^{:rule/type :warn} (fn [input]
                                              (and (= "v1" (:apiVersion input))
                                                   (= "Service" (:kind input))
                                                   (not= 80 (-> input :spec :ports first :port))))]
          (is (= {:result {"test-resources/test.yaml" [{:failure? true
                                                        :message :cljconf/rule-validation-failed
                                                        :name nil
                                                        :rule-type :warn}]}
                  :summary {:failures 0 :passed 0 :total 1 :warnings 1}
                  :summary-report "1 tests, 0 passed, 1 warnings, 0 failures\n"}
                 (cljconf/test invalid-yaml warn-rule)
                 (cljconf/test invalid-yaml {:type :warn :rule warn-rule}))))))
    (testing "rule/name are shown in reporting for failing tests"
      (let [deny-rule ^{:rule/name "my-deny-rule"} (fn [input]
                                                     (and (= "v1" (:apiVersion input))
                                                          (= "Service" (:kind input))
                                                          (not= 80 (-> input :spec :ports first :port))))]
        (is (= {:summary {:total 1, :passed 0, :warnings 0, :failures 1},
                :failure-report "FAIL - test-resources/test.yaml - my-deny-rule - :cljconf/rule-validation-failed\n\n1 tests, 0 passed, 0 warnings, 1 failures\n",
                :result {"test-resources/test.yaml" [{:message :cljconf/rule-validation-failed,
                                                      :name "my-deny-rule",
                                                      :rule-type :deny,
                                                      :failure? true}]}}
               (cljconf/test invalid-yaml deny-rule)
               (cljconf/test invalid-yaml {:name "my-deny-rule" :rule deny-rule})))))
    (testing "rule/message adds top-level error message that is shown by default"
      (is (= {:summary {:total 1, :passed 0, :warnings 0, :failures 1},
              :failure-report "FAIL - test-resources/test.yaml - default top-level message\n\n1 tests, 0 passed, 0 warnings, 1 failures\n",
              :result {"test-resources/test.yaml" [{:message "default top-level message",
                                                    :name nil,
                                                    :rule-type :deny,
                                                    :failure? true}]}}
             (cljconf/test invalid-yaml
                           ^{:rule/message "default top-level message"}
                           (fn [input]
                             (and (= "v1" (:apiVersion input))
                                  (= "Service" (:kind input))
                                  (not= 80 (-> input :spec :ports first :port)))))))
      (testing "messages returned from function override rule/message"
        (is (= {:summary {:total 1, :passed 0, :warnings 0, :failures 1},
                :failure-report "FAIL - test-resources/test.yaml - overridden local-level message\n\n1 tests, 0 passed, 0 warnings, 1 failures\n",
                :result {"test-resources/test.yaml" [{:message "overridden local-level message",
                                                      :name nil,
                                                      :rule-type :deny,
                                                      :failure? true}]}}
               (cljconf/test invalid-yaml
                             ^{:rule/message "default top-level message"}
                             (fn [input]
                               (when (and (= "v1" (:apiVersion input))
                                          (= "Service" (:kind input))
                                          (not= 80 (-> input :spec :ports first :port)))
                                 "overridden local-level message"))))))))
  (testing "multiple map entries"
    (is (= {:summary {:total 4, :passed 2, :warnings 0, :failures 2}
            :result {"test-resources/test.yaml" [{:message nil,
                                                  :name "allow-my-rule",
                                                  :rule-type :allow,
                                                  :failure? false}
                                                 {:message nil,
                                                  :name "deny-my-rule",
                                                  :rule-type :deny,
                                                  :failure? false}],
                     "test-resources/test.2.yaml" [{:message "port should be 80",
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :failure? true}]}}
           (-> (cljconf/test (merge valid-yaml (zipmap ["test-resources/test.2.yaml"] (vals invalid-yaml)))
                             #'cljconf.example-deny-rules/deny-my-rule
                             #'cljconf.example-allow-rules/allow-my-rule)
               (select-keys [:result :summary])))))
  (testing "vector inputs"
    (is (= {:summary {:total 2, :passed 1, :warnings 0, :failures 1}
            :result [{:message nil,
                      :name "deny-my-rule",
                      :rule-type :deny,
                      :failure? false}
                     {:message "port should be 80",
                      :name "deny-my-rule",
                      :rule-type :deny,
                      :failure? true}]}
           (-> (cljconf/test [(first (vals valid-yaml))
                              (first (vals invalid-yaml))]
                             #'cljconf.example-deny-rules/deny-my-rule)
               (select-keys [:summary :result])))))
  (testing "trace report is shown when trace flag is given"
    (testing "single rule traced"
      (testing "when rule is triggered"
        (is (string?
               (:trace-report
                 (cljconf/test-with-opts
                   invalid-yaml
                   [#'cljconf.example-deny-rules/deny-my-rule]
                   {:trace true})))))
      (testing "when rule is not triggered"
        (is (string?
              (:trace-report
                (cljconf/test-with-opts
                  valid-yaml
                  [#'cljconf.example-deny-rules/deny-my-rule]
                  {:trace true}))))))
    (testing "multiple rules traced"
      (testing "when rule is triggered"
        (is (string?
              (:trace-report
                (cljconf/test-with-opts
                  invalid-yaml
                  [#'cljconf.example-deny-rules/deny-my-rule
                   #'cljconf.example-deny-rules/differently-named-deny-rule
                   #'cljconf.example-deny-rules/deny-my-bare-rule
                   #'cljconf.example-deny-rules/deny-my-absolute-bare-rule
                   #'cljconf.example-deny-rules/deny-malli-rule]
                  {:trace true})))))
      (testing "when rule is not triggered"
        (is (string?
              (:trace-report
                (cljconf/test-with-opts
                  valid-yaml
                  [#'cljconf.example-deny-rules/deny-my-rule
                   #'cljconf.example-deny-rules/differently-named-deny-rule
                   #'cljconf.example-deny-rules/deny-my-bare-rule
                   #'cljconf.example-deny-rules/deny-my-absolute-bare-rule
                   #'cljconf.example-deny-rules/deny-malli-rule]
                  {:trace true}))))))
    (testing "vector inputs"
      (testing "when rule is trigerred"
        (is (string?
              (:trace-report
                (cljconf/test-with-opts [(first (vals invalid-yaml))]
                                        [#'cljconf.example-deny-rules/deny-my-rule]
                                        {:trace true})))))
      (testing "when rule is not triggered"
        (is (string?
              (:trace-report
                (cljconf/test-with-opts [(first (vals valid-yaml))]
                                        [#'cljconf.example-deny-rules/deny-my-rule]
                                        {:trace true})))))))
  (testing "trace report is not shown when trace flag is not given"
    (is (nil?
          (:trace-report
            (cljconf/test [(first (vals valid-yaml))]
                          [#'cljconf.example-deny-rules/deny-my-rule]))))
    (is (nil?
          (:trace-report
            (cljconf/test invalid-yaml
                          [#'cljconf.example-deny-rules/deny-my-rule])))))
  (testing "exceptions bubble up to reporting and all rules are evaluated"
    (testing "by default, don't expose the stack trace"
      (let [report (cljconf/test valid-yaml
                                 ^{:rule/name "deny-will-trigger-with-exception"}
                                 #(clojure.string/starts-with? (:nilly-field %) "foo")
                                 ^{:rule/name "deny-will-trigger-cleanly"}
                                 (fn [_] true)
                                 ^{:rule/name "deny-will-not-trigger"}
                                 (fn [_] false))]
        (is (clojure.string/starts-with?
              (get-in report [:result "test-resources/test.yaml" 0 :message])
              "java.lang.NullPointerException"))
        (is (nil? (re-find #"cljconf\.core\$_test\.invoke"
                           (get-in (cljconf/test valid-yaml
                                                 ^{:rule/name "deny-will-trigger-with-exception"}
                                                 #(clojure.string/starts-with? (:nilly-field %) "foo")
                                                 ^{:rule/name "deny-will-trigger-cleanly"}
                                                 (fn [_] true)
                                                 ^{:rule/name "deny-will-not-trigger"}
                                                 (fn [_] false)) [:result "test-resources/test.yaml" 0 :message]))))
        (is (= {:total 3, :passed 1, :warnings 0, :failures 2}
               (:summary report)))))
    (testing "enabling `trace` allows you to see the stack trace"
      (let [report (cljconf/test-with-opts valid-yaml
                                           [^{:rule/name "deny-will-trigger-with-exception"}
                                            #(clojure.string/starts-with? (:nilly-field %) "foo")
                                            ^{:rule/name "deny-will-trigger-cleanly"}
                                            (fn [_] true)
                                            ^{:rule/name "deny-will-not-trigger"}
                                            (fn [_] false)]
                                           {:trace true})]
        (is (clojure.string/starts-with?
              (get-in report [:result "test-resources/test.yaml" 0 :message])
              "java.lang.NullPointerException"))
        (is (clojure.string/starts-with?
              (get-in report [:result "test-resources/test.yaml" 0 :result])
              "java.lang.NullPointerException"))
        (is (some? (re-find #"cljconf\.core\$_test\.invoke"
                            (get-in report [:result "test-resources/test.yaml" 0 :message]))))
        (is (some? (re-find #"cljconf\.core\$_test\.invoke"
                            (get-in report [:result "test-resources/test.yaml" 0 :result]))))
        (is (= {:total 3, :passed 1, :warnings 0, :failures 2}
               (:summary report)))))))