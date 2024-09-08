(ns ilmoraunio.cljconf.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [ilmoraunio.cljconf.core :as conftest]
            [ilmoraunio.cljconf.example-rules]))

(def test-input-yaml
  {"test-resources/test.yaml" {"apiVersion" "v1"
                               "kind" "Service"
                               "metadata" {"name" "hello-kubernetes"}
                               "spec" {"type" "LoadBalancer"
                                       "ports" [{"port" 80.0 "targetPort" 8080.0}]
                                       "selector" {"app" "hello-kubernetes"}}}})

(def test-input-full
  {"test-resources/test.json" {:hello [1 2 4]}
   "test-resources/test.edn" {:foo :bar}
   "test-resources/test.yaml" {"apiVersion" "v1"
                               "kind" "Service"
                               "metadata" {"name" "hello-kubernetes"}
                               "spec" {"type" "LoadBalancer"
                                       "ports" [{"port" 80.0 "targetPort" 8080.0}]
                                       "selector" {"app" "hello-kubernetes"}}}})

(deftest rules-test
  (testing "allow rules"
    (testing "triggered"
      (is (= {"test-resources/test.yaml" [{:message :cljconf/rule-validation-failed
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
               #'ilmoraunio.cljconf.example-rules/allow-my-absolute-bare-rule))))
    (testing "not triggered"
      (is (= {} (conftest/test
                  test-input-yaml
                  #'ilmoraunio.cljconf.example-rules/allow-my-rule
                  #'ilmoraunio.cljconf.example-rules/differently-named-allow-rule
                  #'ilmoraunio.cljconf.example-rules/allow-my-bare-rule
                  #'ilmoraunio.cljconf.example-rules/allow-my-absolute-bare-rule)))))
  (testing "deny rules"
    (testing "triggered"
      (is (= {"test-resources/test.yaml" [{:message :cljconf/rule-validation-failed
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
               #'ilmoraunio.cljconf.example-rules/deny-my-absolute-bare-rule))))
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
               #'ilmoraunio.cljconf.example-rules/deny-my-absolute-bare-rule)))))
  (testing "resolve functions via namespace"
    (is (= {"test-resources/test.yaml" [{:message :cljconf/rule-validation-failed
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
    (is (= {"test-resources/test.yaml" [{:message :cljconf/rule-validation-failed
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
  (testing "multiple inputs"
    ; TODO
    )
  (testing "exceptions"
    ; TODO
    ))