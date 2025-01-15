(ns ilmoraunio.cljconf.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [ilmoraunio.cljconf.core :as conftest]
            [ilmoraunio.cljconf.example-allow-rules]
            [ilmoraunio.cljconf.example-deny-rules]
            [ilmoraunio.cljconf.example-warn-rules]))

(def test-input-yaml
  {"test-resources/test.yaml" {"apiVersion" "v1"
                               "kind" "Service"
                               "metadata" {"name" "hello-kubernetes"}
                               "spec" {"type" "LoadBalancer"
                                       "ports" [{"port" 80.0 "targetPort" 8080.0}]
                                       "selector" {"app" "hello-kubernetes"}}}})

(def test-invalid-yaml
  (assoc-in test-input-yaml ["test-resources/test.yaml"
                             "spec"
                             "ports"
                             0
                             "port"] 9999.0))

(deftest rules-test
  (testing "allow rules"
    (testing "triggered"
      (is (= {:summary {:total 5, :passed 0, :warnings 0, :failures 5}
              :result {"test-resources/test.yaml" [{:message "port should be 80"
                                                    :name "allow-malli-rule"
                                                    :rule-type :allow
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed
                                                    :name "allow-my-absolute-bare-rule"
                                                    :rule-type :allow
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "allow-my-bare-rule"
                                                    :rule-type :allow
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "allow-my-rule"
                                                    :rule-type :allow
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "differently-named-allow-rule"
                                                    :rule-type :allow
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}]}}
             (-> (conftest/test
                   (assoc-in test-input-yaml ["test-resources/test.yaml"
                                              "spec"
                                              "ports"
                                              0
                                              "port"] 9999.0)
                   #'ilmoraunio.cljconf.example-allow-rules/allow-my-rule
                   #'ilmoraunio.cljconf.example-allow-rules/differently-named-allow-rule
                   #'ilmoraunio.cljconf.example-allow-rules/allow-my-bare-rule
                   #'ilmoraunio.cljconf.example-allow-rules/allow-my-absolute-bare-rule
                   #'ilmoraunio.cljconf.example-allow-rules/allow-malli-rule)
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 5, :passed 5, :warnings 0, :failures 0}
              :result {"test-resources/test.yaml" [{:message nil,
                                                    :name "allow-malli-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-absolute-bare-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-bare-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "differently-named-allow-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}]}}
             (-> (conftest/test
                   test-input-yaml
                   #'ilmoraunio.cljconf.example-allow-rules/allow-my-rule
                   #'ilmoraunio.cljconf.example-allow-rules/differently-named-allow-rule
                   #'ilmoraunio.cljconf.example-allow-rules/allow-my-bare-rule
                   #'ilmoraunio.cljconf.example-allow-rules/allow-my-absolute-bare-rule
                   #'ilmoraunio.cljconf.example-allow-rules/allow-malli-rule)
                 (select-keys [:result :summary]))))))
  (testing "deny rules"
    (testing "triggered"
      (is (= {:summary {:total 5, :passed 0, :warnings 0, :failures 5}
              :result {"test-resources/test.yaml" [{:message "port should be 80"
                                                    :name "deny-malli-rule"
                                                    :rule-type :deny
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed
                                                    :name "deny-my-absolute-bare-rule"
                                                    :rule-type :deny
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "deny-my-bare-rule"
                                                    :rule-type :deny
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "deny-my-rule"
                                                    :rule-type :deny
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "differently-named-deny-rule"
                                                    :rule-type :deny
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}]}}
             (-> (conftest/test
                   (assoc-in test-input-yaml ["test-resources/test.yaml"
                                              "spec"
                                              "ports"
                                              0
                                              "port"] 9999.0)
                   #'ilmoraunio.cljconf.example-deny-rules/deny-my-rule
                   #'ilmoraunio.cljconf.example-deny-rules/differently-named-deny-rule
                   #'ilmoraunio.cljconf.example-deny-rules/deny-my-bare-rule
                   #'ilmoraunio.cljconf.example-deny-rules/deny-my-absolute-bare-rule
                   #'ilmoraunio.cljconf.example-deny-rules/deny-malli-rule)
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 5, :passed 5, :warnings 0, :failures 0}
              :result {"test-resources/test.yaml" [{:message nil,
                                                    :name "deny-malli-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-absolute-bare-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-bare-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "differently-named-deny-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}]}}
             (-> (conftest/test
                   test-input-yaml
                   #'ilmoraunio.cljconf.example-deny-rules/deny-my-rule
                   #'ilmoraunio.cljconf.example-deny-rules/differently-named-deny-rule
                   #'ilmoraunio.cljconf.example-deny-rules/deny-my-bare-rule
                   #'ilmoraunio.cljconf.example-deny-rules/deny-my-absolute-bare-rule
                   #'ilmoraunio.cljconf.example-deny-rules/deny-malli-rule)
                 (select-keys [:result :summary]))))))
  (testing "warn rules"
    (testing "triggered"
      (is (= {:summary {:total 5, :passed 0, :warnings 5, :failures 0}
              :result {"test-resources/test.yaml" [{:message "port should be 80"
                                                    :name "differently-named-warn-rule"
                                                    :rule-type :warn
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "warn-malli-rule"
                                                    :rule-type :warn
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed
                                                    :name "warn-my-absolute-bare-rule"
                                                    :rule-type :warn
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "warn-my-bare-rule"
                                                    :rule-type :warn
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}
                                                   {:message "port should be 80"
                                                    :name "warn-my-rule"
                                                    :rule-type :warn
                                                    :rule-target "test-resources/test.yaml"
                                                    :failure? true}]}}
             (-> (conftest/test
                   (assoc-in test-input-yaml ["test-resources/test.yaml"
                                              "spec"
                                              "ports"
                                              0
                                              "port"] 9999.0)
                   #'ilmoraunio.cljconf.example-warn-rules/warn-my-rule
                   #'ilmoraunio.cljconf.example-warn-rules/differently-named-warn-rule
                   #'ilmoraunio.cljconf.example-warn-rules/warn-my-bare-rule
                   #'ilmoraunio.cljconf.example-warn-rules/warn-my-absolute-bare-rule
                   #'ilmoraunio.cljconf.example-warn-rules/warn-malli-rule)
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 5, :passed 5, :warnings 0, :failures 0}
              :result {"test-resources/test.yaml" [{:message nil,
                                                    :name "differently-named-warn-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-malli-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-absolute-bare-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-bare-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}]}}
             (-> (conftest/test
                   test-input-yaml
                   #'ilmoraunio.cljconf.example-warn-rules/warn-my-rule
                   #'ilmoraunio.cljconf.example-warn-rules/differently-named-warn-rule
                   #'ilmoraunio.cljconf.example-warn-rules/warn-my-bare-rule
                   #'ilmoraunio.cljconf.example-warn-rules/warn-my-absolute-bare-rule
                   #'ilmoraunio.cljconf.example-warn-rules/warn-malli-rule)
                 (select-keys [:result :summary]))))))
  (testing "resolve functions via namespace"
    (testing "triggered"
      (is (= {:summary {:total 15, :passed 0, :warnings 5, :failures 10}
              :result {"test-resources/test.yaml" [{:message "port should be 80",
                                                    :name "allow-malli-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed,
                                                    :name "allow-my-absolute-bare-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "allow-my-bare-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "differently-named-allow-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "deny-malli-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed,
                                                    :name "deny-my-absolute-bare-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "deny-my-bare-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "differently-named-deny-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "differently-named-warn-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "warn-malli-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed,
                                                    :name "warn-my-absolute-bare-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "warn-my-bare-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "warn-my-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}]}}
             (-> (conftest/test
                   (assoc-in test-input-yaml ["test-resources/test.yaml"
                                              "spec"
                                              "ports"
                                              0
                                              "port"] 9999.0)
                   'ilmoraunio.cljconf.example-allow-rules
                   'ilmoraunio.cljconf.example-deny-rules
                   'ilmoraunio.cljconf.example-warn-rules)
                 (select-keys [:result :summary]))
             (-> (conftest/test
                   (assoc-in test-input-yaml ["test-resources/test.yaml"
                                              "spec"
                                              "ports"
                                              0
                                              "port"] 9999.0)
                   (the-ns 'ilmoraunio.cljconf.example-allow-rules)
                   (the-ns 'ilmoraunio.cljconf.example-deny-rules)
                   (the-ns 'ilmoraunio.cljconf.example-warn-rules))
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 15, :passed 15, :warnings 0, :failures 0}
              :result {"test-resources/test.yaml" [{:message nil,
                                                    :name "allow-malli-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-absolute-bare-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-bare-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "differently-named-allow-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-malli-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-absolute-bare-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-bare-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "differently-named-deny-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "differently-named-warn-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-malli-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-absolute-bare-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-bare-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}]}}
             (-> (conftest/test
                   test-input-yaml
                   'ilmoraunio.cljconf.example-allow-rules
                   'ilmoraunio.cljconf.example-deny-rules
                   'ilmoraunio.cljconf.example-warn-rules)
                 (select-keys [:result :summary]))
             (-> (conftest/test
                   test-input-yaml
                   (the-ns 'ilmoraunio.cljconf.example-allow-rules)
                   (the-ns 'ilmoraunio.cljconf.example-deny-rules)
                   (the-ns 'ilmoraunio.cljconf.example-warn-rules))
                 (select-keys [:result :summary]))))))
  (testing "anonymous functions"
    (testing "triggered"
      (is (= {:summary {:total 4, :passed 0, :warnings 1, :failures 3}
              :result {"test-resources/test.yaml" [{:message "port should be 80",
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message :cljconf/rule-validation-failed,
                                                    :name nil,
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "warn-my-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}]}}
             (-> (conftest/test
                   (assoc-in test-input-yaml ["test-resources/test.yaml"
                                              "spec"
                                              "ports"
                                              0
                                              "port"] 9999.0)
                   ^{:rule/type :allow
                     :rule/name :allow-my-rule
                     :rule/message "port should be 80"}
                   (fn [input]
                     (and (= "v1" (get input "apiVersion"))
                          (= "Service" (get input "kind"))
                          (= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

                   ^{:rule/type :deny
                     :rule/name :deny-my-rule
                     :rule/message "port should be 80"}
                   (fn [input]
                     (and (= "v1" (get input "apiVersion"))
                          (= "Service" (get input "kind"))
                          (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

                   ;; "plain" anonymous functions (with no overriding metadata) are implicitly deny rules
                   (fn [input]
                     (and (= "v1" (get input "apiVersion"))
                          (= "Service" (get input "kind"))
                          (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

                   ^{:rule/type :warn
                     :rule/name :warn-my-rule
                     :rule/message "port should be 80"}
                   (fn [input]
                     (and (= "v1" (get input "apiVersion"))
                          (= "Service" (get input "kind"))
                          (not= 80.0 (get-in input ["spec" "ports" 0 "port"])))))
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 4, :passed 4, :warnings 0, :failures 0}
              :result {"test-resources/test.yaml" [{:message nil,
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name nil,
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}
                                                   {:message nil,
                                                    :name "warn-my-rule",
                                                    :rule-type :warn,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? false}]}}
             (-> (conftest/test
                   test-input-yaml
                   ^{:rule/type :allow
                     :rule/name :allow-my-rule
                     :rule/message "port should be 80"}
                   (fn [input]
                     (and (= "v1" (get input "apiVersion"))
                          (= "Service" (get input "kind"))
                          (= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

                   ^{:rule/type :deny
                     :rule/name :deny-my-rule
                     :rule/message "port should be 80"}
                   (fn [input]
                     (and (= "v1" (get input "apiVersion"))
                          (= "Service" (get input "kind"))
                          (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

                   (fn [input]
                     (and (= "v1" (get input "apiVersion"))
                          (= "Service" (get input "kind"))
                          (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))

                   ^{:rule/type :warn
                     :rule/name :warn-my-rule
                     :rule/message "port should be 80"}
                   (fn [input]
                     (and (= "v1" (get input "apiVersion"))
                          (= "Service" (get input "kind"))
                          (not= 80.0 (get-in input ["spec" "ports" 0 "port"])))))
                 (select-keys [:result :summary])))))
    (testing "rule/type affect rule evaluation"
      (testing "anonymous functions are deny rules by default"
        (let [deny-rule (fn [input]
                          (and (= "v1" (get input "apiVersion"))
                               (= "Service" (get input "kind"))
                               (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))]
          (is (= {:summary {:total 1, :passed 0, :warnings 0, :failures 1},
                  :failure-report "FAIL - test-resources/test.yaml - :cljconf/rule-validation-failed\n\n1 tests, 0 passed, 0 warnings, 1 failures\n"
                  :result {"test-resources/test.yaml" [{:message :cljconf/rule-validation-failed,
                                                        :name nil,
                                                        :rule-type :deny,
                                                        :rule-target "test-resources/test.yaml",
                                                        :failure? true}]}}
                 (conftest/test test-invalid-yaml deny-rule)
                 (conftest/test test-invalid-yaml {:rule deny-rule})))))
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
                                                        :rule-target "test-resources/test.yaml",
                                                        :failure? true}]}}
                 (conftest/test test-invalid-yaml allow-rule)
                 (conftest/test test-invalid-yaml {:type :allow :rule allow-rule})))))
      (testing "you can instruct anonymous functions to be warn-based rules"
        (let [warn-rule ^{:rule/type :warn} (fn [input]
                                              (and (= "v1" (get input "apiVersion"))
                                                   (= "Service" (get input "kind"))
                                                   (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))]
          (is (= {:result {"test-resources/test.yaml" [{:failure? true
                                                        :message :cljconf/rule-validation-failed
                                                        :name nil
                                                        :rule-target "test-resources/test.yaml"
                                                        :rule-type :warn}]}
                  :summary {:failures 0 :passed 0 :total 1 :warnings 1}
                  :summary-report "1 tests, 0 passed, 1 warnings, 0 failures\n"}
                 (conftest/test test-invalid-yaml warn-rule)
                 (conftest/test test-invalid-yaml {:type :warn :rule warn-rule}))))))
    (testing "rule/name are shown in reporting for failing tests"
      (let [deny-rule ^{:rule/name "my-deny-rule"} (fn [input]
                                                     (and (= "v1" (get input "apiVersion"))
                                                          (= "Service" (get input "kind"))
                                                          (not= 80.0 (get-in input ["spec" "ports" 0 "port"]))))]
        (is (= {:summary {:total 1, :passed 0, :warnings 0, :failures 1},
                :failure-report "FAIL - test-resources/test.yaml - my-deny-rule - :cljconf/rule-validation-failed\n\n1 tests, 0 passed, 0 warnings, 1 failures\n",
                :result {"test-resources/test.yaml" [{:message :cljconf/rule-validation-failed,
                                                      :name "my-deny-rule",
                                                      :rule-type :deny,
                                                      :rule-target "test-resources/test.yaml",
                                                      :failure? true}]}}
               (conftest/test test-invalid-yaml deny-rule)
               (conftest/test test-invalid-yaml {:name "my-deny-rule" :rule deny-rule})))))
    (testing "rule/message adds top-level error message that is shown by default"
      (is (= {:summary {:total 1, :passed 0, :warnings 0, :failures 1},
              :failure-report "FAIL - test-resources/test.yaml - default top-level message\n\n1 tests, 0 passed, 0 warnings, 1 failures\n",
              :result {"test-resources/test.yaml" [{:message "default top-level message",
                                                    :name nil,
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}]}}
             (conftest/test test-invalid-yaml
                            ^{:rule/message "default top-level message"}
                            (fn [input]
                              (and (= "v1" (get input "apiVersion"))
                                   (= "Service" (get input "kind"))
                                   (not= 80.0 (get-in input ["spec" "ports" 0 "port"])))))))
      (testing "messages returned from function override rule/message"
        (is (= {:summary {:total 1, :passed 0, :warnings 0, :failures 1},
                :failure-report "FAIL - test-resources/test.yaml - overridden local-level message\n\n1 tests, 0 passed, 0 warnings, 1 failures\n",
                :result {"test-resources/test.yaml" [{:message "overridden local-level message",
                                                      :name nil,
                                                      :rule-type :deny,
                                                      :rule-target "test-resources/test.yaml",
                                                      :failure? true}]}}
               (conftest/test test-invalid-yaml
                              ^{:rule/message "default top-level message"}
                              (fn [input]
                                (when (and (= "v1" (get input "apiVersion"))
                                           (= "Service" (get input "kind"))
                                           (not= 80.0 (get-in input ["spec" "ports" 0 "port"])))
                                  "overridden local-level message"))))))))
  (testing "multiple map entries"
    (is (= {:summary {:total 4, :passed 2, :warnings 0, :failures 2}
            :result {"test-resources/test.yaml" [{:message nil,
                                                  :name "allow-my-rule",
                                                  :rule-type :allow,
                                                  :rule-target "test-resources/test.yaml",
                                                  :failure? false}
                                                 {:message nil,
                                                  :name "deny-my-rule",
                                                  :rule-type :deny,
                                                  :rule-target "test-resources/test.yaml",
                                                  :failure? false}],
                     "test-resources/test.2.yaml" [{:message "port should be 80",
                                                    :name "allow-my-rule",
                                                    :rule-type :allow,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}
                                                   {:message "port should be 80",
                                                    :name "deny-my-rule",
                                                    :rule-type :deny,
                                                    :rule-target "test-resources/test.yaml",
                                                    :failure? true}]}}
           (-> (conftest/test (merge test-input-yaml
                                     (assoc-in {"test-resources/test.2.yaml" (first (vals test-input-yaml))}
                                               ["test-resources/test.2.yaml"
                                                "spec"
                                                "ports"
                                                0
                                                "port"] 9999.0))
                              #'ilmoraunio.cljconf.example-deny-rules/deny-my-rule
                              #'ilmoraunio.cljconf.example-allow-rules/allow-my-rule)
               (select-keys [:result :summary])))))
  (testing "vector inputs"
    (is (= {:summary {:total 2, :passed 1, :warnings 0, :failures 1}
            :result [{:message nil,
                      :name "deny-my-rule",
                      :rule-type :deny,
                      :rule-target {"apiVersion" "v1",
                                    "kind" "Service",
                                    "metadata" {"name" "hello-kubernetes"},
                                    "spec" {"type" "LoadBalancer",
                                            "ports" [{"port" 80.0, "targetPort" 8080.0}],
                                            "selector" {"app" "hello-kubernetes"}}},
                      :failure? false}
                     {:message "port should be 80",
                      :name "deny-my-rule",
                      :rule-type :deny,
                      :rule-target {"apiVersion" "v1",
                                    "kind" "Service",
                                    "metadata" {"name" "hello-kubernetes"},
                                    "spec" {"type" "LoadBalancer",
                                            "ports" [{"port" 9999.0, "targetPort" 8080.0}],
                                            "selector" {"app" "hello-kubernetes"}}},
                      :failure? true}]}
           (-> (conftest/test [(first (vals test-input-yaml))
                               (assoc-in (first (vals test-input-yaml))
                                         ["spec"
                                          "ports"
                                          0
                                          "port"] 9999.0)]
                              #'ilmoraunio.cljconf.example-deny-rules/deny-my-rule)
               (select-keys [:summary :result]))))))