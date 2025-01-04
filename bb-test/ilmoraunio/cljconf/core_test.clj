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
  (testing "Dockerfile"
    (is (= {:exit 1,
            :out [[{:type "FAIL",
                    :file "examples/dockerfile/Dockerfile",
                    :rule "deny-unallowed-commands",
                    :message "unallowed commands found [\"apk add --no-cache python3 python3-dev build-base && pip3 install awscli==1.18.1\"]"}
                   {:type "FAIL",
                    :file "examples/dockerfile/Dockerfile",
                    :rule "deny-unallowed-images",
                    :message "unallowed image found [\"openjdk:8-jdk-alpine\"]"}]
                  {:tests 2, :passed 0, :warnings 0, :failures 2}]}
           (cljconf-test ["examples/dockerfile/Dockerfile"]
                         ["examples/dockerfile/policy.clj"]))))
  (testing "Dotenv"
    (is (= {:exit 0, :out [[] {:tests 2, :passed 2, :warnings 0, :failures 0}]}
           (cljconf-test ["examples/dotenv/sample.env"]
                         ["examples/dotenv/policy.clj"]))))
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
  (testing "HCL"
    (is (= {:exit 0, :out [[] {:tests 1, :passed 1, :warnings 0, :failures 0}]}
           (cljconf-test ["examples/hcl1/gke.tf"]
                         ["examples/hcl1/tf_policy.clj"])))
    (is (= {:exit 1,
            :out [[{:type "FAIL",
                    :file "examples/hcl1/gke-show.json",
                    :rule "deny-prohibited-resources",
                    :message "Terraform plan will change prohibited resources in the following namespaces: [\"google_iam\" \"google_container\"]"}]
                  {:tests 1, :passed 0, :warnings 0, :failures 1}]}
           (cljconf-test ["examples/hcl1/gke-show.json"]
                         ["examples/hcl1/json_policy.clj"])))
    (is (= {:exit 0, :out [[] {:tests 2, :passed 2, :warnings 0, :failures 0}]}
           (cljconf-test ["examples/hcl1/gke.tf"
                          "examples/hcl1/gke-show.json"]
                         ["examples/hcl1/combined_policy.clj"]))))
  (testing "HCL 2"
    (is (= {:exit 1,
            :out [[{:type "FAIL",
                    :file "examples/hcl2/terraform.tf",
                    :rule "deny-fully-open-ingress",
                    :message "ASG rules (\"my-rule\") define a fully open ingress"}
                   {:type "FAIL",
                    :file "examples/hcl2/terraform.tf",
                    :rule "deny-http",
                    :message "ALB listeners (\"my-alb-listener\") are using HTTP rather than HTTPS"}
                   {:type "FAIL",
                    :file "examples/hcl2/terraform.tf",
                    :rule "deny-missing-tags",
                    :message "AWS resource: aws_alb_listener named 'my-alb-listener' is missing required tags: #{\"owner\" \"environment\"}, AWS resource: aws_db_security_group named 'my-group' is missing required tags: #{\"owner\" \"environment\"}, AWS resource: aws_security_group_rule named 'my-rule' is missing required tags: #{\"owner\" \"environment\"}"}
                   {:type "FAIL",
                    :file "examples/hcl2/terraform.tf",
                    :rule "deny-unencrypted-azure-disk",
                    :message "Azure disks (\"source\") are not encrypted"}]
                  {:tests 4, :passed 0, :warnings 0, :failures 4}]}
           (cljconf-test ["examples/hcl2/terraform.tf"]
                         ["examples/hcl2/policy.clj"]))))
  (testing "Ignore"
    (testing ".gitignore"
      (is (= {:exit 1,
              :out [[{:type "FAIL",
                      :file "examples/ignore/gitignore/.gitignore",
                      :rule "deny-no-id-rsa-files-ignored",
                      :message "id_rsa files should be ignored"}]
                    {:tests 1, :passed 0, :warnings 0, :failures 1}]}
             (cljconf-test ["examples/ignore/gitignore/.gitignore"]
                           ["examples/ignore/gitignore/policy.clj"]))))
    (testing ".dockerignore"
      (is (= {:exit 1,
              :out [[{:type "FAIL",
                      :file "examples/ignore/dockerignore/.dockerignore",
                      :rule "deny-git-not-ignored",
                      :message ".git directories should be ignored"}]
                    {:tests 1, :passed 0, :warnings 0, :failures 1}]}
             (cljconf-test ["examples/ignore/dockerignore/.dockerignore"]
                           ["examples/ignore/dockerignore/policy.clj"])))))
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