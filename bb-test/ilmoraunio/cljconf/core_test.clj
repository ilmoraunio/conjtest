(ns ilmoraunio.cljconf.core-test
  (:require [babashka.process :refer [shell process exec]]
            [cljconf.bb.api :as api]
            [clojure.test :refer [deftest is testing]]))

(set! *data-readers* {'ordered/map #'flatland.ordered.map/ordered-map})

(defn copy
  [m k new-k f & args]
  (merge m {new-k (apply f (get m k) args)}))

(defn cljconf-test*
  [inputs policies & extra-args]
  (assert (and (coll? inputs) (not-empty inputs)))
  (assert (and (coll? policies) (not-empty policies)))
  (-> (apply shell (cond-> (-> [{:out :string, :err :string, :continue true}
                                "./cljconf"
                                "test"]
                               (into inputs)
                               (into (interleave (cycle ["--policy"]) policies)))
                     extra-args (into extra-args)))
      (copy :out :out-original identity)
      (update :out (juxt #(->> (re-seq #"(?m)(FAIL|WARN) - (.+) - (.+) - (.+)" %)
                               (map rest)
                               (mapv (partial zipmap [:type :file :rule :message])))
                         #(->> (re-find #"(\d+) tests, (\d+) passed, (\d+) warnings, (\d+) failures" %)
                               (rest)
                               (map (fn [x] (Integer/parseInt x)))
                               (zipmap [:tests :passed :warnings :failures]))))))

(defn cljconf-test
  [inputs policies & extra-args]
  (select-keys (apply cljconf-test* inputs policies extra-args) [:exit :out]))

(defn cljconf-parse
  [inputs & extra-args]
  (assert (and (coll? inputs) (not-empty inputs)))
  (-> (apply shell (cond-> (-> [{:out :string, :err :string, :continue true}
                                "./cljconf"
                                "parse"]
                               (into inputs))
                     extra-args (into extra-args)))
      (select-keys [:exit :out])
      (update :out (partial clojure.edn/read-string {:readers {'ordered/map #'flatland.ordered.map/ordered-map}}))))

(deftest api-test
  (testing "smoke test"
    (testing "triggered"
      (is (thrown-with-msg? Exception #"16 tests, 0 passed, 5 warnings, 11 failures"
                            (api/test! ["test-resources/invalid.yaml"]
                                       {:policy ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"
                                                 "test-resources/ilmoraunio/cljconf/example_warn_rules.clj"
                                                 "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"
                                                 "test-resources/ilmoraunio/cljconf/example_local_require.clj"]
                                        :config "test.cljconf.edn"})))
      (try
        (api/test! ["test-resources/invalid.yaml"]
                   {:policy ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"
                             "test-resources/ilmoraunio/cljconf/example_warn_rules.clj"
                             "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"
                             "test-resources/ilmoraunio/cljconf/example_local_require.clj"]
                    :config "test.cljconf.edn"})
        (catch Exception e
          (is (= {:total 16, :passed 0, :warnings 5, :failures 11}
                 (ex-data e))))))
    (testing "not triggered"
      (is (= {:summary {:total 16, :passed 16, :warnings 0, :failures 0},
              :summary-report "16 tests, 16 passed, 0 warnings, 0 failures\n"}
             (select-keys
              (api/test! ["test-resources/valid.yaml"]
                         {:policy ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"
                                   "test-resources/ilmoraunio/cljconf/example_warn_rules.clj"
                                   "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"
                                   "test-resources/ilmoraunio/cljconf/example_local_require.clj"]
                          :config "test.cljconf.edn"})
              [:summary :summary-report]))))))

(deftest cli-test
  (testing "cljconf test"
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
                               "--config" "test.cljconf.edn"))))))
    (testing "--trace"
      (testing "triggered"
        (is (= ["deny-malli-rule"
                "deny-my-absolute-bare-rule"
                "deny-my-bare-rule"
                "deny-my-rule"
                "differently-named-deny-rule"]
               (map second
                    (re-seq
                      #"(?m)Rule name: (.*)"
                      (:out-original (cljconf-test* ["test-resources/invalid.yaml"]
                                                    ["test-resources/ilmoraunio/cljconf/example_deny_rules.clj"]
                                                    "--trace")))))))
      (testing "not triggered"
        (is (= ["deny-malli-rule"
                "deny-my-absolute-bare-rule"
                "deny-my-bare-rule"
                "deny-my-rule"
                "differently-named-deny-rule"]
               (map second
                    (re-seq
                      #"(?m)Rule name: (.*)"
                      (:out-original (cljconf-test* ["test-resources/valid.yaml"]
                                                    ["test-resources/ilmoraunio/cljconf/example_deny_rules.clj"]
                                                    "--trace")))))))))
  (testing "cljconf parse"
    (testing "clojure parser"
      (is (= {:exit 0,
              :out {"test-resources/deps.edn" {:paths ["src" "resources"],
                                               :deps {'org.clojure/clojure {:mvn/version "1.12.0"}, 'metosin/malli {:mvn/version "0.16.4"}},
                                               :aliases {:build {:deps {'io.github.clojure/tools.build {:mvn/version "0.10.5"},
                                                                        'slipset/deps-deploy {:mvn/version "0.2.2"}},
                                                                 :ns-default 'build},
                                                         :test {:extra-paths ["test" "test-resources" "examples"],
                                                                :extra-deps {'lambdaisland/kaocha {:mvn/version "1.82.1306"},
                                                                             'lambdaisland/kaocha-cljs {:mvn/version "1.4.130"},
                                                                             'lambdaisland/kaocha-junit-xml {:mvn/version "1.17.101"}}}}}}}
             (cljconf-parse ["test-resources/deps.edn"])
             (cljconf-parse ["test-resources/deps.edn"] "--parser" "edn"))))
    (testing "go parser"
      (is (= {:exit 0,
              :out {"test-resources/deps.edn" {":deps" {"org.clojure/clojure" {":mvn/version" "1.12.0"}, "metosin/malli" {":mvn/version" "0.16.4"}},
                                               ":aliases" {":build" {":deps" {"io.github.clojure/tools.build" {":mvn/version" "0.10.5"},
                                                                              "slipset/deps-deploy" {":mvn/version" "0.2.2"}},
                                                                     ":ns-default" "build"},
                                                           ":test" {":extra-paths" ["test" "test-resources" "examples"],
                                                                    ":extra-deps" {"lambdaisland/kaocha-cljs" {":mvn/version" "1.4.130"},
                                                                                   "lambdaisland/kaocha-junit-xml" {":mvn/version" "1.17.101"},
                                                                                   "lambdaisland/kaocha" {":mvn/version" "1.82.1306"}}}},
                                               ":paths" ["src" "resources"]}}}
             (cljconf-parse ["test-resources/deps.edn"] "--go-parsers-only"))))
    (testing "--parser"
      (is (= {:exit 0, :out {"test-resources/test.json" {:hello [1 2 4], "@foo" "bar"}}}
             (cljconf-parse ["test-resources/test.json"])
             (cljconf-parse ["test-resources/test.json"] "--parser" "json")))
      (is (= {:exit 0, :out {"test-resources/test.json" #ordered/map([:hello [1 2 4]] ["@foo" "bar"])}}
             (cljconf-parse ["test-resources/test.json"] "--parser" "yaml"))))
    (testing "multiple arguments"
      (is (= {:exit 0,
              :out {"test-resources/test.json" {:hello [1 2 4], "@foo" "bar"},
                    "test-resources/deps.edn" {:paths ["src" "resources"],
                                               :deps {'org.clojure/clojure {:mvn/version "1.12.0"},
                                                      'metosin/malli {:mvn/version "0.16.4"}},
                                               :aliases {:build {:deps {'io.github.clojure/tools.build {:mvn/version "0.10.5"},
                                                                        'slipset/deps-deploy {:mvn/version "0.2.2"}},
                                                                 :ns-default 'build},
                                                         :test {:extra-paths ["test" "test-resources" "examples"],
                                                                :extra-deps {'lambdaisland/kaocha {:mvn/version "1.82.1306"},
                                                                             'lambdaisland/kaocha-cljs {:mvn/version "1.4.130"},
                                                                             'lambdaisland/kaocha-junit-xml {:mvn/version "1.17.101"}}}}},
                    "test-resources/invalid.yaml" #ordered/map([:apiVersion "v1"]
                                                               [:kind "Service"]
                                                               [:metadata #ordered/map([:name "hello-kubernetes"])]
                                                               [:spec
                                                                #ordered/map([:type "LoadBalancer"]
                                                                             [:ports [#ordered/map([:port 81] [:targetPort 8080])]]
                                                                             [:selector #ordered/map([:app "bad-hello-kubernetes"])])]),
                    "test-resources/valid.yaml" #ordered/map([:apiVersion "v1"]
                                                             [:kind "Service"]
                                                             [:metadata #ordered/map([:name "hello-kubernetes"])]
                                                             [:spec
                                                              #ordered/map([:type "LoadBalancer"]
                                                                           [:ports [#ordered/map([:port 80] [:targetPort 8080])]]
                                                                           [:selector #ordered/map([:app "hello-kubernetes"])])])}}
             (cljconf-parse ["test-resources/test.json" "test-resources/deps.edn" "test-resources/valid.yaml" "test-resources/invalid.yaml"])
             (cljconf-parse ["test-resources/*.{json,edn,yaml}"])))))
  (testing "exception reporting"
    (testing "stack trace is not shown by default"
      (is (= {:exit 1,
              :out [[{:type "FAIL",
                      :file "test-resources/valid.yaml",
                      :rule "deny-broken-malli-rule",
                      :message "clojure.lang.ExceptionInfo: :malli.core/invalid-schema {:type :malli.core/invalid-schema, :message :malli.core/invalid-schema, :data {:schema :mapxxx, :form [:mapxxx [\"apiVersion\" [:= \"v1\"]] [\"kind\" [:= \"Service\"]] [\"spec\" [:map [\"ports\" [:+ [:map [\"port\" [:not= 80.0]]]]]]]]}}"}
                     {:type "FAIL",
                      :file "test-resources/valid.yaml",
                      :rule "deny-will-trigger-cleanly",
                      :message ":cljconf/rule-validation-failed"}
                     {:type "FAIL",
                      :file "test-resources/valid.yaml",
                      :rule "deny-will-trigger-with-exception",
                      :message "java.lang.NullPointerException"}]
                    {:tests 4, :passed 1, :warnings 0, :failures 3}]}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_broken_rules.clj"]))))
    (testing "when --trace is provided, stack trace is shown"
      (is (= {:exit 1,
              :out [[{:type "FAIL",
                      :file "test-resources/valid.yaml",
                      :rule "deny-broken-malli-rule",
                      ;; the full stack trace continues beyond what we assert here
                      :message "clojure.lang.ExceptionInfo: :malli.core/invalid-schema"}
                     {:type "FAIL",
                      :file "test-resources/valid.yaml",
                      :rule "deny-will-trigger-cleanly",
                      :message ":cljconf/rule-validation-failed"}
                     {:type "FAIL",
                      :file "test-resources/valid.yaml",
                      :rule "deny-will-trigger-with-exception",
                      ;; ditto; the full stack trace continues beyond what we assert here
                      :message "java.lang.NullPointerException: null"}]
                    {:tests 4, :passed 1, :warnings 0, :failures 3}]}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_broken_rules.clj"]
                           "--trace"))))))

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
                    :message "unallowed command found 'apk add --no-cache python3 python3-dev build-base && pip3 install awscli==1.18.1'"}
                   {:type "FAIL",
                    :file "examples/dockerfile/Dockerfile",
                    :rule "deny-unallowed-images",
                    :message "unallowed image found 'openjdk:8-jdk-alpine'"}]
                  {:tests 2, :passed 0, :warnings 0, :failures 2}]}
           (cljconf-test ["examples/dockerfile/Dockerfile"]
                         ["examples/dockerfile/policy.clj"]))))
  (testing "Dotenv"
    (is (= {:exit 0, :out [[] {:tests 2, :passed 2, :warnings 0, :failures 0}]}
           (cljconf-test ["examples/dotenv/sample.env"]
                         ["examples/dotenv/policy.clj"]))))
  (testing "EDN"
    (testing "clojure parser"
      (is (= {:exit 1, :out [[{:type "FAIL"
                               :file "examples/edn/sample_config.edn",
                               :rule "deny-incorrect-log-level-production",
                               :message "Applications in the production environment should have error only logging"}]
                             {:tests 2, :passed 1, :warnings 0, :failures 1}]}
             (cljconf-test ["examples/edn/sample_config.edn"]
                           ["examples/edn/policy.clj"])
             (cljconf-test ["examples/edn/sample_config.edn"]
                           ["examples/edn/policy.clj"]
                           "--parser" "edn"))))
    (testing "go parser"
      (is (= {:exit 1,
              :out [[{:type "FAIL",
                      :file "examples/edn/sample_config.edn",
                      :rule "deny-incorrect-log-level-production",
                      :message "Applications in the production environment should have error only logging"}]
                    {:tests 2, :passed 1, :warnings 0, :failures 1}]}
             (cljconf-test ["examples/edn/sample_config.edn"]
                           ["examples/edn/policy_go.clj"]
                           "--go-parsers-only")
             (cljconf-test ["examples/edn/sample_config.edn"]
                           ["examples/edn/policy_go.clj"]
                           "--go-parsers-only"
                           "--parser" "edn")))))
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
                    :message "ASG rule 'my-rule' defines a fully open ingress"}
                   {:type "FAIL",
                    :file "examples/hcl2/terraform.tf",
                    :rule "deny-http",
                    :message "ALB listener 'my-alb-listener' is using HTTP rather than HTTPS"}
                   {:type "FAIL",
                    :file "examples/hcl2/terraform.tf",
                    :rule "deny-missing-tags",
                    :message "AWS resource: aws_alb_listener named 'my-alb-listener' is missing required tags: #{\"owner\" \"environment\"}"}
                   {:type "FAIL",
                    :file "examples/hcl2/terraform.tf",
                    :rule "deny-missing-tags",
                    :message "AWS resource: aws_db_security_group named 'my-group' is missing required tags: #{\"owner\" \"environment\"}"}
                   {:type "FAIL",
                    :file "examples/hcl2/terraform.tf",
                    :rule "deny-missing-tags",
                    :message "AWS resource: aws_security_group_rule named 'my-rule' is missing required tags: #{\"owner\" \"environment\"}"}
                   {:type "FAIL",
                    :file "examples/hcl2/terraform.tf",
                    :rule "deny-unencrypted-azure-disk",
                    :message "Azure disk 'source' is not encrypted"}]
                  {:tests 4, :passed 0, :warnings 0, :failures 4}]}
           (cljconf-test ["examples/hcl2/terraform.tf"]
                         ["examples/hcl2/policy.clj"]))))
  (testing "HOCON"
    (is (= {:exit 1,
            :out [[{:type "FAIL",
                    :file "examples/hocon/hocon.conf",
                    :rule "deny-wrong-port",
                    :message "Play http server port should be 9000"}]
                  {:tests 2, :passed 1, :warnings 0, :failures 1}]}
           (cljconf-test ["examples/hocon/hocon.conf"]
                         ["examples/hocon/policy.clj"]
                         "--parser" "hocon")
           (cljconf-test ["examples/hocon/hocon.conf"]
                         ["examples/hocon/policy.clj"]
                         "--go-parsers-only"
                         "--parser" "hocon"))))
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
  (testing "INI"
    (is (= {:exit 1,
            :out [[{:type "FAIL",
                    :file "examples/ini/grafana.ini",
                    :rule "deny-verify-email-disabled",
                    :message "Users should verify their e-mail address"}]
                  {:tests 6, :passed 5, :warnings 0, :failures 1}]}
           (cljconf-test ["examples/ini/grafana.ini"]
                         ["examples/ini/policy.clj"]))))
  (testing "JSON"
    (testing "package.json"
      (testing "clojure parser"
        (is (= {:exit 1,
                :out [[{:type "FAIL",
                        :file "examples/json/package.json",
                        :rule "deny-caret-ranges",
                        :message "caret ranges not allowed, offending library: [:express \"^4.17.3\"]"}]
                      {:tests 1, :passed 0, :warnings 0, :failures 1}]}
               (cljconf-test ["examples/json/package.json"]
                             ["examples/json/policy.clj"])
               (cljconf-test ["examples/json/package.json"]
                             ["examples/json/policy.clj"]
                             "--parser" "json"))))
      (testing "go parser"
        (is (= {:exit 1,
                :out [[{:type "FAIL",
                        :file "examples/json/package.json",
                        :rule "deny-caret-ranges",
                        :message "caret ranges not allowed, offending library: [\"express\" \"^4.17.3\"]"}]
                      {:tests 1, :passed 0, :warnings 0, :failures 1}]}
               (cljconf-test ["examples/json/package.json"]
                             ["examples/json/policy_go.clj"]
                             "--go-parsers-only")
               (cljconf-test ["examples/json/package.json"]
                             ["examples/json/policy_go.clj"]
                             "--go-parsers-only"
                             "--parser" "json")))))
    (testing "CycloneDX"
      (is (= {:exit 1,
              :out [[{:type "FAIL",
                      :file "examples/json/cyclonedx/cyclonedx.json",
                      :rule "deny-incorrect-sha",
                      :message "current SHA256 sha256:WRONG_VERSION is not equal to expected SHA256 sha256:d7ec60cf8390612b360c857688b383068b580d9a6ab78417c9493170ad3f1616"}]
                    {:tests 1, :passed 0, :warnings 0, :failures 1}]}
             (cljconf-test ["examples/json/cyclonedx/cyclonedx.json"]
                           ["examples/json/cyclonedx/policy.clj"])))))
  (testing "Jsonnet"
    (is (= {:exit 1,
            :out [[{:type "FAIL",
                    :file "examples/jsonnet/arith.jsonnet",
                    :rule "deny-concat-array",
                    :message "Concat array should be less than 3"}]
                  {:tests 2, :passed 1, :warnings 0, :failures 1}]}
           (cljconf-test ["examples/jsonnet/arith.jsonnet"]
                         ["examples/jsonnet/policy.clj"]))))
  (testing "Properties"
    (is (= {:exit 0, :out [[] {:tests 2, :passed 2, :warnings 0, :failures 0}]}
           (cljconf-test ["examples/properties/sample.properties"]
                         ["examples/properties/policy.clj"]))))
  (testing "Spdx"
    (is (= {:exit 1,
            :out [[{:type "FAIL",
                    :file "examples/spdx/sbom.spdx",
                    :rule "deny-incorrect-license",
                    :message "DataLicense should be 'correct-license', but found 'conftest-demo'"}]
                  {:tests 1, :passed 0, :warnings 0, :failures 1}]}
           (cljconf-test ["examples/spdx/sbom.spdx"]
                         ["examples/spdx/policy.clj"]))))
  (testing "Textproto")
  (testing "TOML"
    (testing "Traefik"
      (is (= {:exit 1,
              :out [[{:type "FAIL",
                      :file "examples/toml/traefik/traefik.toml",
                      :rule "deny-disallowed-ciphers",
                      :message "Following ciphers are not allowed: [\"TLS_RSA_WITH_AES_256_GCM_SHA384\"]"}]
                    {:tests 1, :passed 0, :warnings 0, :failures 1}]}
             (cljconf-test ["examples/toml/traefik/traefik.toml"]
                           ["examples/toml/traefik/policy.clj"])))))
  (testing "Typescript")
  (testing "VCL"
    (is (= {:exit 1,
            :out [[{:type "FAIL",
                    :file "examples/vcl/varnish.vcl",
                    :rule "deny-incorrect-port",
                    :message "default backend port should be 8080"}]
                  {:tests 2, :passed 1, :warnings 0, :failures 1}]}
           (cljconf-test ["examples/vcl/varnish.vcl"]
                         ["examples/vcl/policy.clj"]))))
  (testing "XML"
    (is (= {:exit 1,
            :out [[{:type "FAIL",
                    :file "examples/xml/pom.xml",
                    :rule "deny-incorrect-compiler-plugin-version",
                    :message "maven-compiler-plugin must have the following version: 3.6.1"}]
                  {:tests 3, :passed 2, :warnings 0, :failures 1}]}
           (cljconf-test ["examples/xml/pom.xml"]
                         ["examples/xml/policy.clj"])))
    (testing "CycloneDX"
      (is (= {:exit 1,
              :out [[{:type "FAIL",
                      :file "examples/xml/cyclonedx/cyclonedx.xml",
                      :rule "deny-incorrect-sha",
                      :message "current SHA256 sha256:WRONG_VERSION is not equal to expected SHA256 sha256:d7ec60cf8390612b360c857688b383068b580d9a6ab78417c9493170ad3f1616"}]
                    {:tests 1, :passed 0, :warnings 0, :failures 1}]}
             (cljconf-test ["examples/xml/cyclonedx/cyclonedx.xml"]
                           ["examples/xml/cyclonedx/policy.clj"])))))
  (testing "YAML"
    (testing "Kubernetes"
      (testing "clojure parser"
        (is (= {:exit 1,
                :out [[{:type "FAIL",
                        :file "examples/yaml/kubernetes/deployment.yaml",
                        :rule "deny-missing-required-deployment-selectors",
                        :message "Deployment \"hello-kubernetes\" must provide app/release labels for pod selectors"}
                       {:type "FAIL",
                        :file "examples/yaml/kubernetes/deployment.yaml",
                        :rule "deny-should-not-run-as-root",
                        :message "Containers must not run as root in Deployment \"hello-kubernetes\""}]
                      {:tests 2, :passed 0, :warnings 0, :failures 2}]}
               (cljconf-test ["examples/yaml/kubernetes/deployment.yaml"]
                             ["examples/yaml/kubernetes/policy.clj"]))))
      (testing "go parser"
        (is (= {:exit 1,
                :out [[{:type "FAIL",
                        :file "examples/yaml/kubernetes/deployment.yaml",
                        :rule "deny-missing-required-deployment-selectors",
                        :message "Deployment \"hello-kubernetes\" must provide app/release labels for pod selectors"}
                       {:type "FAIL",
                        :file "examples/yaml/kubernetes/deployment.yaml",
                        :rule "deny-should-not-run-as-root",
                        :message "Containers must not run as root in Deployment \"hello-kubernetes\""}]
                      {:tests 2, :passed 0, :warnings 0, :failures 2}]}
               (cljconf-test ["examples/yaml/kubernetes/deployment.yaml"]
                             ["examples/yaml/kubernetes/policy_go.clj"]
                             "--go-parsers-only")))))
    (testing "combine example"
      (testing "clojure parser"
        (is (= {:exit 1,
                :out [[{:type "FAIL",
                        :file "examples/yaml/combine/combine.yaml",
                        :rule "deny-deployments-with-no-matching-service",
                        :message "Deployment 'goodbye-kubernetes' has no matching service"}]
                      {:tests 1, :passed 0, :warnings 0, :failures 1}]}
               (cljconf-test ["examples/yaml/combine/combine.yaml"]
                             ["examples/yaml/combine/policy.clj"]))))
      (testing "go parser"
        (is (= {:exit 1,
                :out [[{:type "FAIL",
                        :file "examples/yaml/combine/combine.yaml",
                        :rule "deny-deployments-with-no-matching-service",
                        :message "Deployment 'goodbye-kubernetes' has no matching service"}]
                      {:tests 1, :passed 0, :warnings 0, :failures 1}]}
               (cljconf-test ["examples/yaml/combine/combine.yaml"]
                             ["examples/yaml/combine/policy_go.clj"]
                             "--go-parsers-only")))))
    (testing "AWS SAM Framework"
      (testing "clojure parser"
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
      (testing "go parser"
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
                             ["examples/yaml/awssam/policy_go.clj"]
                             "--go-parsers-only")))))
    (testing "Docker compose"
      (testing "clojure parser"
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
                             ["examples/yaml/dockercompose/policy.clj"]))))
      (testing "go parser"
        (is (= {:exit 0, :out [[] {:tests 2, :passed 2, :warnings 0, :failures 0}]}
               (cljconf-test ["examples/yaml/dockercompose/docker-compose-valid.yml"]
                             ["examples/yaml/dockercompose/policy_go.clj"]
                             "--go-parsers-only")))
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
                             ["examples/yaml/dockercompose/policy_go.clj"]
                             "--go-parsers-only")))))
    (testing "Serverless framework"
      (testing "clojure parser"
        (is (= {:exit 1,
                :out [[{:type "FAIL",
                        :file "examples/yaml/serverless/serverless.yaml",
                        :rule "deny-functions-python2.7",
                        :message "Python 2.7 cannot be used as the runtime for functions"}
                       {:type "FAIL",
                        :file "examples/yaml/serverless/serverless.yaml",
                        :rule "deny-python2.7",
                        :message "Python 2.7 cannot be the default provider runtime"}]
                      {:tests 3, :passed 1, :warnings 0, :failures 2}]}
               (cljconf-test ["examples/yaml/serverless/serverless.yaml"]
                             ["examples/yaml/serverless/policy.clj"]))))
      (testing "go parser"
        (is (= {:exit 1,
                :out [[{:type "FAIL",
                        :file "examples/yaml/serverless/serverless.yaml",
                        :rule "deny-functions-python2.7",
                        :message "Python 2.7 cannot be used as the runtime for functions"}
                       {:type "FAIL",
                        :file "examples/yaml/serverless/serverless.yaml",
                        :rule "deny-python2.7",
                        :message "Python 2.7 cannot be the default provider runtime"}]
                      {:tests 3, :passed 1, :warnings 0, :failures 2}]}
               (cljconf-test ["examples/yaml/serverless/serverless.yaml"]
                             ["examples/yaml/serverless/policy_go.clj"]
                             "--go-parsers-only")))))))
