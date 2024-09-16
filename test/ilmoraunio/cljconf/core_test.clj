(ns ilmoraunio.cljconf.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest testing is]]
            [ilmoraunio.cljconf.core :as conftest]
            [ilmoraunio.cljconf.example-rules]))

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
      (is (= {"test-resources/test.yaml" [{:message "port should be 80"
                                           :name "allow-malli-rule"
                                           :rule-type :allow}
                                          {:message :cljconf/rule-validation-failed
                                           :name "allow-my-absolute-bare-rule"
                                           :rule-type :allow}
                                          {:message "port should be 80"
                                           :name "allow-my-bare-rule"
                                           :rule-type :allow}
                                          {:message "port should be 80"
                                           :name "allow-my-rule"
                                           :rule-type :allow}
                                          {:message "port should be 80"
                                           :name "differently-named-allow-rule"
                                           :rule-type :allow}]}
             (conftest/test
               (assoc-in test-input-yaml ["test-resources/test.yaml"
                                          "spec"
                                          "ports"
                                          0
                                          "port"] 9999.0)
               #'ilmoraunio.cljconf.example-rules/allow-my-rule
               #'ilmoraunio.cljconf.example-rules/differently-named-allow-rule
               #'ilmoraunio.cljconf.example-rules/allow-my-bare-rule
               #'ilmoraunio.cljconf.example-rules/allow-my-absolute-bare-rule
               #'ilmoraunio.cljconf.example-rules/allow-malli-rule))))
    (testing "not triggered"
      (is (= {} (conftest/test
                  test-input-yaml
                  #'ilmoraunio.cljconf.example-rules/allow-my-rule
                  #'ilmoraunio.cljconf.example-rules/differently-named-allow-rule
                  #'ilmoraunio.cljconf.example-rules/allow-my-bare-rule
                  #'ilmoraunio.cljconf.example-rules/allow-my-absolute-bare-rule
                  #'ilmoraunio.cljconf.example-rules/allow-malli-rule)))))
  (testing "deny rules"
    (testing "triggered"
      (is (= {"test-resources/test.yaml" [{:message "port should not be 80"
                                           :name "deny-malli-rule"
                                           :rule-type :deny}
                                          {:message :cljconf/rule-validation-failed
                                           :name "deny-my-absolute-bare-rule"
                                           :rule-type :deny}
                                          {:message "port should not be 80"
                                           :name "deny-my-bare-rule"
                                           :rule-type :deny}
                                          {:message "port should not be 80"
                                           :name "deny-my-rule"
                                           :rule-type :deny}
                                          {:message "port should not be 80"
                                           :name "differently-named-deny-rule"
                                           :rule-type :deny}]}
             (conftest/test
               test-input-yaml
               #'ilmoraunio.cljconf.example-rules/deny-my-rule
               #'ilmoraunio.cljconf.example-rules/differently-named-deny-rule
               #'ilmoraunio.cljconf.example-rules/deny-my-bare-rule
               #'ilmoraunio.cljconf.example-rules/deny-my-absolute-bare-rule
               #'ilmoraunio.cljconf.example-rules/deny-malli-rule))))
    (testing "not triggered"
      (is (= {}
             (conftest/test
               (assoc-in test-input-yaml ["test-resources/test.yaml"
                                          "spec"
                                          "ports"
                                          0
                                          "port"] 9999.0)
               #'ilmoraunio.cljconf.example-rules/deny-my-rule
               #'ilmoraunio.cljconf.example-rules/differently-named-deny-rule
               #'ilmoraunio.cljconf.example-rules/deny-my-bare-rule
               #'ilmoraunio.cljconf.example-rules/deny-my-absolute-bare-rule
               #'ilmoraunio.cljconf.example-rules/deny-malli-rule)))))
  (testing "resolve functions via namespace"
    (is (= {"test-resources/test.yaml" [{:message "port should not be 80"
                                         :name "deny-malli-rule"
                                         :rule-type :deny}
                                        {:message :cljconf/rule-validation-failed
                                         :name "deny-my-absolute-bare-rule"
                                         :rule-type :deny}
                                        {:message "port should not be 80"
                                         :name "deny-my-bare-rule"
                                         :rule-type :deny}
                                        {:message "port should not be 80"
                                         :name "deny-my-rule"
                                         :rule-type :deny}
                                        {:message "port should not be 80"
                                         :name "differently-named-deny-rule"
                                         :rule-type :deny}]}
           (conftest/test
             test-input-yaml
             'ilmoraunio.cljconf.example-rules)
           (conftest/test
             test-input-yaml
             (the-ns 'ilmoraunio.cljconf.example-rules))))
    (is (= {"test-resources/test.yaml" [{:message "port should be 80"
                                         :name "allow-malli-rule"
                                         :rule-type :allow}
                                        {:message :cljconf/rule-validation-failed
                                         :name "allow-my-absolute-bare-rule"
                                         :rule-type :allow}
                                        {:message "port should be 80"
                                         :name "allow-my-bare-rule"
                                         :rule-type :allow}
                                        {:message "port should be 80"
                                         :name "allow-my-rule"
                                         :rule-type :allow}
                                        {:message "port should be 80"
                                         :name "differently-named-allow-rule"
                                         :rule-type :allow}]}
           (conftest/test
             (assoc-in test-input-yaml ["test-resources/test.yaml"
                                        "spec"
                                        "ports"
                                        0
                                        "port"] 9999.0)
             'ilmoraunio.cljconf.example-rules)
           (conftest/test
             (assoc-in test-input-yaml ["test-resources/test.yaml"
                                        "spec"
                                        "ports"
                                        0
                                        "port"] 9999.0)
             (the-ns 'ilmoraunio.cljconf.example-rules)))))
  (testing "multiple map entries"
    (is (= {"test-resources/test.2.yaml" [{:message "port should be 80", :name "allow-my-rule", :rule-type :allow}],
            "test-resources/test.yaml" [{:message "port should not be 80", :name "deny-my-rule", :rule-type :deny}]}
           (conftest/test (merge test-input-yaml
                                 (assoc-in {"test-resources/test.2.yaml" (first (vals test-input-yaml))}
                                           ["test-resources/test.2.yaml"
                                            "spec"
                                            "ports"
                                            0
                                            "port"] 9999.0))
                          #'ilmoraunio.cljconf.example-rules/deny-my-rule
                          #'ilmoraunio.cljconf.example-rules/allow-my-rule))))
  (testing "vector inputs"
    (is (= [{:message "port should not be 80", :name "deny-my-rule", :rule-type :deny}]
           (conftest/test [(first (vals test-input-yaml))
                           (assoc-in (first (vals test-input-yaml))
                                     ["spec"
                                      "ports"
                                      0
                                      "port"] 9999.0)]
                          #'ilmoraunio.cljconf.example-rules/deny-my-rule)))))