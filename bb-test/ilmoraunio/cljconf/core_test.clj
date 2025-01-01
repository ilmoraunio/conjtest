(ns ilmoraunio.cljconf.core-test
  (:require [babashka.process :refer [shell process exec]]
            [cljconf.bb.api :as api]
            [clojure.test :refer [deftest is testing]]))


(defn cljconf-test
  [inputs policies & extra-args]
  (assert (and (coll? inputs) (not-empty inputs)))
  (assert (and (coll? policies) (not-empty policies)))
  (-> (apply shell (cond-> (-> [{:out :string, :err :string, :continue true}
                                "./cljconf"
                                "test"]
                               (into inputs)
                               (into (interleave (cycle ["--policy"]) policies)))
                     extra-args (into extra-args)))
        (select-keys [:exit :out])
        (update :out (juxt #(->> (re-seq #"(?m)(FAIL|WARN) - (.+) - (.+) - (.+)" %)
                                 (map rest)
                                 (mapv (partial zipmap [:type :file :rule :message])))
                           #(->> (re-find #"(\d+) tests, (\d+) passed, (\d+) warnings, (\d+) failures" %)
                                 (rest)
                                 (map (fn [x] (Integer/parseInt x)))
                                 (zipmap [:tests :passed :warnings :failures]))))))

(deftest api-test
  (testing "smoke test"
    (testing "triggered"
      (let [args {:args ["test-resources/invalid.yaml"]
                  :opts {:policy ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"
                                  "test-resources/ilmoraunio/cljconf/example_warn_rules.clj"
                                  "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"
                                  "test-resources/ilmoraunio/cljconf/example_local_require.clj"]
                         :config "test.cljconf.edn"}}]
        (is (thrown-with-msg? Exception #"16 tests, 0 passed, 5 warnings, 11 failures"
                              (api/test! args)))
        (try
          (api/test! args)
          (catch Exception e
            (is (= {:summary {:total 16, :passed 0, :warnings 5, :failures 11}}
                   (ex-data e)))))))
    (testing "not triggered"
      (is (= {:summary {:total 16, :passed 16, :warnings 0, :failures 0},
              :summary-report "16 tests, 16 passed, 0 warnings, 0 failures\n"}
             (api/test!
               {:args ["test-resources/valid.yaml"]
                :opts {:policy ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"
                                "test-resources/ilmoraunio/cljconf/example_warn_rules.clj"
                                "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"
                                "test-resources/ilmoraunio/cljconf/example_local_require.clj"]
                       :config "test.cljconf.edn"}}))))))

(deftest cli-test
  (testing "allow rule"
    (testing "fails when rule returns false"
      (is (= {:exit 1,
              :out [[{:type "FAIL", :file "test-resources/invalid.yaml", :rule "allow-malli-rule", :message "port should be 80"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "allow-my-absolute-bare-rule", :message ":cljconf/rule-validation-failed"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "allow-my-bare-rule", :message "port should be 80"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "allow-my-rule", :message "port should be 80"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "differently-named-allow-rule", :message "port should be 80"}]
                    {:tests 5, :passed 0, :warnings 0, :failures 5}]}
             (cljconf-test ["test-resources/invalid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"]))))
    (testing "passes when rule returns true"
      (is (= {:exit 0 :out [[] {:tests 5 :passed 5 :warnings 0 :failures 0}]}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"])))))
  (testing "deny rule"
    (testing "fails when rule returns true"
      (is (= {:exit 0 :out [[] {:tests 5 :passed 5 :warnings 0 :failures 0}]}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_deny_rules.clj"]))))
    (testing "passes when rule returns false"
      (is (= {:exit 1,
              :out [[{:type "FAIL", :file "test-resources/invalid.yaml", :rule "deny-malli-rule", :message "port should be 80"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "deny-my-absolute-bare-rule", :message ":cljconf/rule-validation-failed"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "deny-my-bare-rule", :message "port should be 80"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "deny-my-rule", :message "port should be 80"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "differently-named-deny-rule", :message "port should be 80"}]
                    {:tests 5, :passed 0, :warnings 0, :failures 5}]}
             (cljconf-test ["test-resources/invalid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_deny_rules.clj"])))))
  (testing "warn rule"
    (testing "fails when rule returns true and --fail-on-warn flag is provided"
      (is (= {:exit 1,
              :out [[{:type "WARN", :file "test-resources/invalid.yaml", :rule "differently-named-warn-rule", :message "port should be 80"}
                     {:type "WARN", :file "test-resources/invalid.yaml", :rule "warn-malli-rule", :message "port should be 80"}
                     {:type "WARN", :file "test-resources/invalid.yaml", :rule "warn-my-absolute-bare-rule", :message ":cljconf/rule-validation-failed"}
                     {:type "WARN", :file "test-resources/invalid.yaml", :rule "warn-my-bare-rule", :message "port should be 80"}
                     {:type "WARN", :file "test-resources/invalid.yaml", :rule "warn-my-rule", :message "port should be 80"}]
                    {:tests 5, :passed 0, :warnings 5, :failures 0}]}
             (cljconf-test ["test-resources/invalid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_warn_rules.clj"]
                           "--fail-on-warn"))))
    (testing "warns when rule returns true and --fail-on-warn flag is not provided"
      (is (= {:exit 0 :out [[] {:tests 5 :passed 0 :warnings 5 :failures 0}]}
             (cljconf-test ["test-resources/invalid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_warn_rules.clj"]))))
    (testing "passes when rule returns false"
      (is (= {:exit 0 :out [[] {:tests 5 :passed 5 :warnings 0 :failures 0}]}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_warn_rules.clj"])))))
  (testing "combined policies"
    (testing "smoke test"
      (is (= {:exit 0, :out [[] {:tests 15, :passed 15, :warnings 0, :failures 0}]}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"
                            "test-resources/ilmoraunio/cljconf/example_warn_rules.clj"
                            "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"]))))
    (testing "duplicates are deduped"
      (is (= {:exit 0, :out [[] {:tests 5, :passed 5, :warnings 0, :failures 0}]}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_deny_rules.clj"
                            "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"]))))
    (testing "exit code 2 when deny rule returns true and --fail-on-warn flag is provided"
      (is (= {:exit 2,
              :out [[{:type "FAIL", :file "test-resources/invalid.yaml", :rule "deny-malli-rule", :message "port should be 80"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "deny-my-absolute-bare-rule", :message ":cljconf/rule-validation-failed"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "deny-my-bare-rule", :message "port should be 80"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "deny-my-rule", :message "port should be 80"}
                     {:type "FAIL", :file "test-resources/invalid.yaml", :rule "differently-named-deny-rule", :message "port should be 80"}
                     {:type "WARN", :file "test-resources/invalid.yaml", :rule "differently-named-warn-rule", :message "port should be 80"}
                     {:type "WARN", :file "test-resources/invalid.yaml", :rule "warn-malli-rule", :message "port should be 80"}
                     {:type "WARN", :file "test-resources/invalid.yaml", :rule "warn-my-absolute-bare-rule", :message ":cljconf/rule-validation-failed"}
                     {:type "WARN", :file "test-resources/invalid.yaml", :rule "warn-my-bare-rule", :message "port should be 80"}
                     {:type "WARN", :file "test-resources/invalid.yaml", :rule "warn-my-rule", :message "port should be 80"}]
                    {:tests 10, :passed 0, :warnings 5, :failures 5}]}
             (cljconf-test ["test-resources/invalid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_warn_rules.clj"
                            "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"]
                           "--fail-on-warn")))))
  (testing "support locally required namespaces"
    (testing "smoke test"
      (testing "pass"
        (is (= {:exit 0, :out [[] {:tests 1, :passed 1, :warnings 0, :failures 0}]}
               (cljconf-test ["test-resources/valid.yaml"]
                             ["test-resources/ilmoraunio/cljconf/example_local_require.clj"]
                             "--config" "test.cljconf.edn"))))
      (testing "failure"
        (is (= {:exit 1,
                :out [[{:type "FAIL", :file "test-resources/invalid.yaml", :rule "allow-allowlisted-selector-only", :message ":cljconf/rule-validation-failed"}]
                      {:tests 1, :passed 0, :warnings 0, :failures 1}]}
               (cljconf-test ["test-resources/invalid.yaml"]
                             ["test-resources/ilmoraunio/cljconf/example_local_require.clj"]
                             "--config" "test.cljconf.edn")))))))

(deftest examples-test
  (testing "Configfile")
  (testing "CUE"
    (is (= {:exit 1,
         :out [[{:type "FAIL",
                 :file "examples/cue/deployment.cue",
                 :rule "deny-no-images-tagged",
                 :message "No images tagged latest"}
                {:type "FAIL",
                 :file "examples/cue/deployment.cue",
                 :rule "deny-ports-outside-of-8080",
                 :message "The image port should be 8080 in deployment.cue. you have: [8081.0]"}]
               {:tests 4, :passed 2, :warnings 0, :failures 2}]}
        (cljconf-test ["examples/cue/deployment.cue"]
                      ["examples/cue/policy.clj"]))))
  (testing "Dockerfile")
  (testing "Dotenv")
  (testing "EDN"
    (testing "cljconf parser"
      (is (= {:exit 1, :out [[{:type "FAIL"
                               :file "examples/edn/sample_config.edn",
                               :rule "deny-incorrect-log-level-production",
                               :message "Applications in the production environment should have error only logging"}]
                             {:tests 2, :passed 1, :warnings 0, :failures 1}]}
             (cljconf-test ["examples/edn/sample_config.edn"]
                           ["examples/edn/policy.clj"]))))
    (testing "conftest parser"))
  (testing "HCL")
  (testing "HCL 2")
  (testing "HOCON")
  (testing "Ignore"
    (testing ".gitignore")
    (testing ".dockerignore"))
  (testing "INI")
  (testing "JSON"
    (testing "CycloneDX"
      (is (= {:exit 0, :out [[] {:tests 1, :passed 1, :warnings 0, :failures 0}]}
             (cljconf-test ["examples/json/cyclonedx/cyclonedx.json"]
                          ["examples/json/cyclonedx/policy.clj"])))))
  (testing "Jsonnet")
  (testing "Kustomize")
  (testing "Properties")
  (testing "Serverless Framework")
  (testing "Spdx")
  (testing "Textproto")
  (testing "Traefik")
  (testing "Typescript")
  (testing "VCL")
  (testing "XML"
    (testing "CycloneDX"
      (is (= {:exit 0, :out [[] {:tests 1, :passed 1, :warnings 0, :failures 0}]}
             (cljconf-test ["examples/xml/cyclonedx/cyclonedx.xml"]
                           ["examples/xml/cyclonedx/policy.clj"])))))
  (testing "YAML"
    (is (= {:exit 1,
            :out [[{:type "FAIL",
                    :file "examples/yaml/deployment.yaml",
                    :rule "deny-missing-required-deployment-selectors",
                    :message "Deployment \"hello-kubernetes\" must provide app/release labels for pod selectors"}
                   {:type "FAIL",
                    :file "examples/yaml/deployment.yaml",
                    :rule "deny-should-not-run-as-root",
                    :message "Containers must not run as root in Deployment \"hello-kubernetes\""}]
                  {:tests 2, :passed 0, :warnings 0, :failures 2}]}
           (cljconf-test ["examples/yaml/deployment.yaml"]
                         ["examples/yaml/policy.clj"])))
    (testing "combine example"
      (is (= {:exit 1,
              :out [[{:type "FAIL",
                      :file "examples/yaml/combine/combine.yaml",
                      :rule "deny-deployments-with-no-matching-service",
                      :message "Deployments ['goodbye-kubernetes'] have no matching service"}]
                    {:tests 1, :passed 0, :warnings 0, :failures 1}]}
             (cljconf-test ["examples/yaml/combine/combine.yaml"]
                           ["examples/yaml/combine/policy.clj"]))))
    (testing "AWS SAM Framework"
      (is (= {:exit 1,
           :out [[{:type "FAIL",
                   :file "examples/yaml/awssam/lambda.yaml",
                   :rule "deny-denylisted-runtimes",
                   :message "'python2.7' runtime not allowed"}
                  {:type "FAIL",
                   :file "examples/yaml/awssam/lambda.yaml",
                   :rule "deny-excessive-action-permissions",
                   :message "excessive Action permissions not allowed"}
                  {:type "FAIL",
                   :file "examples/yaml/awssam/lambda.yaml",
                   :rule "deny-excessive-resource-permissions",
                   :message "excessive Resource permissions not allowed"}
                  {:type "FAIL",
                   :file "examples/yaml/awssam/lambda.yaml",
                   :rule "deny-python2.7",
                   :message "python2.7 runtime not allowed"}
                  {:type "FAIL",
                   :file "examples/yaml/awssam/lambda.yaml",
                   :rule "deny-sensitive-environment-variables",
                   :message "Sensitive data not allowed in environment variables"}]
                 {:tests 5, :passed 0, :warnings 0, :failures 5}]}
          (cljconf-test ["examples/yaml/awssam/lambda.yaml"]
                        ["examples/yaml/awssam/policy.clj"]))))
    (testing "Docker compose"
      (is (= {:exit 0, :out [[] {:tests 2, :passed 2, :warnings 0, :failures 0}]}
             (cljconf-test ["examples/yaml/dockercompose/docker-compose-valid.yml"]
                           ["examples/yaml/dockercompose/policy.clj"])))
      (is (= {:exit 1,
              :out [[{:type "FAIL",
                      :file "examples/yaml/dockercompose/docker-compose-invalid.yml",
                      :rule "deny-latest-tags",
                      :message "No images tagged latest"}
                     {:type "FAIL",
                      :file "examples/yaml/dockercompose/docker-compose-invalid.yml",
                      :rule "deny-old-compose-versions",
                      :message "Must be using at least version 3.5 of the Compose file format"}]
                    {:tests 2, :passed 0, :warnings 0, :failures 2}]}
             (cljconf-test ["examples/yaml/dockercompose/docker-compose-invalid.yml"]
                           ["examples/yaml/dockercompose/policy.clj"]))))))