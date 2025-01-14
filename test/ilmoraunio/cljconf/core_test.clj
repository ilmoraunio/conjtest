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
  (testing "pass functions directly"
    (testing "triggered"
      (is (= {:summary {:total 3, :passed 0, :warnings 1, :failures 2}
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

                   ^{:rule/type :warn
                     :rule/name :warn-my-rule
                     :rule/message "port should be 80"}
                   (fn [input]
                     (and (= "v1" (get input "apiVersion"))
                          (= "Service" (get input "kind"))
                          (not= 80.0 (get-in input ["spec" "ports" 0 "port"])))))
                 (select-keys [:result :summary])))))
    (testing "not triggered"
      (is (= {:summary {:total 3, :passed 3, :warnings 0, :failures 0}
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

                   ^{:rule/type :warn
                     :rule/name :warn-my-rule
                     :rule/message "port should be 80"}
                   (fn [input]
                     (and (= "v1" (get input "apiVersion"))
                          (= "Service" (get input "kind"))
                          (not= 80.0 (get-in input ["spec" "ports" 0 "port"])))))
                 (select-keys [:result :summary]))))))
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