(ns ilmoraunio.cljconf.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [babashka.process :refer [shell process exec]]))


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
        (update :out #(->> (re-find #"(\d+) tests, (\d+) passed, (\d+) warnings, (\d+) failures" %)
                           (rest)
                           (map (fn [x] (Integer/parseInt x)))
                           (zipmap [:tests :passed :warnings :failures])))))

(deftest cli-test
  (testing "allow rule"
    (testing "fails when rule returns false"
      (is (= {:exit 1 :out {:tests 5 :passed 0 :warnings 0 :failures 5}}
             (cljconf-test ["test-resources/invalid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"]))))
    (testing "passes when rule returns true"
      (is (= {:exit 0 :out {:tests 5 :passed 5 :warnings 0 :failures 0}}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"])))))
  (testing "deny rule"
    (testing "fails when rule returns true"
      (is (= {:exit 0 :out {:tests 5 :passed 5 :warnings 0 :failures 0}}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_deny_rules.clj"]))))
    (testing "passes when rule returns false"
      (is (= {:exit 1 :out {:tests 5 :passed 0 :warnings 0 :failures 5}}
             (cljconf-test ["test-resources/invalid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_deny_rules.clj"])))))
  (testing "warn rule"
    (testing "fails when rule returns true and --fail-on-warn flag is provided"
      (is (= {:exit 1 :out {:tests 5 :passed 0 :warnings 5 :failures 0}}
             (cljconf-test ["test-resources/invalid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_warn_rules.clj"]
                           "--fail-on-warn"))))
    (testing "warns when rule returns true and --fail-on-warn flag is not provided"
      (is (= {:exit 0 :out {:tests 5 :passed 0 :warnings 5 :failures 0}}
             (cljconf-test ["test-resources/invalid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_warn_rules.clj"]))))
    (testing "passes when rule returns false"
      (is (= {:exit 0 :out {:tests 5 :passed 5 :warnings 0 :failures 0}}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_warn_rules.clj"])))))
  (testing "combined policies"
    (testing "smoke test"
      (is (= {:exit 0, :out {:tests 15, :passed 15, :warnings 0, :failures 0}}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_allow_rules.clj"
                            "test-resources/ilmoraunio/cljconf/example_warn_rules.clj"
                            "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"]))))
    (testing "duplicates are deduped"
      (is (= {:exit 0, :out {:tests 5, :passed 5, :warnings 0, :failures 0}}
             (cljconf-test ["test-resources/valid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_deny_rules.clj"
                            "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"]))))
    (testing "exit code 2 when deny rule returns true and --fail-on-warn flag is provided"
      (is (= {:exit 2 :out {:tests 10 :passed 0 :warnings 5 :failures 5}}
             (cljconf-test ["test-resources/invalid.yaml"]
                           ["test-resources/ilmoraunio/cljconf/example_warn_rules.clj"
                            "test-resources/ilmoraunio/cljconf/example_deny_rules.clj"]
                           "--fail-on-warn")))))
  (testing "support locally required namespaces"
    (testing "smoke test"
      (testing "pass"
        (is (= {:exit 0, :out {:tests 1, :passed 1, :warnings 0, :failures 0}}
               (cljconf-test ["test-resources/valid.yaml"]
                             ["test-resources/ilmoraunio/cljconf/example_local_require.clj"]
                             "--config" "test.cljconf.edn"))))
      (testing "failure"
        (is (= {:exit 1, :out {:tests 1, :passed 0, :warnings 0, :failures 1}}
               (cljconf-test ["test-resources/invalid.yaml"]
                             ["test-resources/ilmoraunio/cljconf/example_local_require.clj"]
                             "--config" "test.cljconf.edn")))))))

(deftest examples-test
  (testing "AWS SAM framework")
  (testing "Configfile")
  (testing "CUE")
  (testing "Cyclonedx")
  (testing "Docker compose")
  (testing "Dockerfile")
  (testing "Dotenv")
  (testing "EDN"
    (testing "cljconf parser"
      (is (= {:exit 0, :out {:tests 2, :passed 2, :warnings 0, :failures 0}}
             (cljconf-test ["examples/edn/sample_config.edn"]
                           ["examples/edn/policy/smoke.clj"]))))
    (testing "conftest parser"))
  (testing "HCL")
  (testing "HCL 2")
  (testing "HOCON")
  (testing "Ignore"
    (testing ".gitignore")
    (testing ".dockerignore"))
  (testing "INI")
  (testing "Jsonnet")
  (testing "Kubernetes")
  (testing "Kustomize")
  (testing "Properties")
  (testing "Serverless Framework")
  (testing "Spdx")
  (testing "Textproto")
  (testing "Traefik")
  (testing "Typescript")
  (testing "VCL")
  (testing "XML"))