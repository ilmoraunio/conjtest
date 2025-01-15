(ns cljconf.bb.main
  (:require [babashka.cli :as cli]
            [babashka.tasks :as tasks]
            [clojure.string :as str]
            [cljconf.bb.api]))

(defmacro if-bb-cli
  [then else]
  (if (and (System/getProperty "babashka.version")
           (not (System/getenv "CLJCONF_DEV")))
    then
    else))

(defn show-help
  [spec]
  (cli/format-opts (merge spec {:order (vec (keys (:spec spec)))})))

(defmulti validate-policy type)
(defmethod validate-policy java.lang.String
  [s]
  (not-empty s))
(defmethod validate-policy clojure.lang.PersistentHashSet
  [s]
  (not (boolean
     (some->> s
              (map str/trim)
              (filter (partial = ""))
              (some identity)))))

(def test-cli-spec
  {:spec {:config {:coerce :string
                   :alias :c
                   :desc "Filepath to configuration file"
                   :default nil
                   :default-desc ""
                   :validate {:pred (complement empty?)
                              :ex-msg "--config must be non-empty string"}}
          :fail-on-warn {:coerce :boolean
                         :desc "Produces exit code 1 for any warnings triggered and exit code 2 for any errors triggered"}
          :go-parsers-only {:coerce :boolean
                            :alias :go
                            :desc "Use Go-based parsers only"}
          :help {:coerce :boolean
                 :alias :h}
          :parser {:coerce :string
                   :desc "Use specific parser to parse files. Supported parsers: cue, dockerfile, edn, hcl1, hcl2, ignore, ini, json, jsonnet, properties, spdx, toml, vcl, xml, yaml, dotenv"
                   :validate {:pred (complement empty?)
                              :ex-msg (constantly "--parser must be non-empty string")}}
          :policy {:coerce #{:string}
                   :alias :p
                   :desc "Filepath to look for policy files, supports globs, defaults to current dir"
                   :default #{"*"}
                   :default-desc ""
                   :validate {:pred validate-policy
                              :ex-msg (constantly "--policy must be non-empty string")}}}
   :restrict [:config :fail-on-warn :go-parsers-only :help :parser :policy]})

(def parse-cli-spec
  {:spec {:go-parsers-only {:coerce :boolean
                            :alias :go
                            :desc "Use Go-based parsers only"}
          :help {:coerce :boolean
                 :alias :h}
          :parser {:coerce :string
                   :desc "Use specific parser to parse files. Supported parsers: cue, dockerfile, edn, hcl1, hcl2, ignore, ini, json, jsonnet, properties, spdx, toml, vcl, xml, yaml, dotenv"
                   :validate {:pred (complement empty?)
                              :ex-msg (constantly "--parser must be non-empty string")}}}
   :restrict [:go-parsers-only :help :parser]})

(defn test
  [args]
  (let [{:keys [args opts] :as m} (cli/parse-args args test-cli-spec)]
    (if (or (:help opts) (:h opts))
      (println (show-help test-cli-spec))
      (try (println (:summary-report (cljconf.bb.api/test! args opts)))
           (catch Exception e
             (if-bb-cli
               (do
                 (println (ex-message e))
                 (if (:fail-on-warn opts)
                   (let [{:keys [warnings failures] :as _summary} (ex-data e)]
                     (cond
                       (pos? failures) (System/exit 2)
                       (pos? warnings) (System/exit 1)))
                  (System/exit 1)))
               (throw e)))))))

(defn parse
  [args]
  (let [{:keys [args opts] :as _args} (cli/parse-args args parse-cli-spec)]
    (if (or (:help opts) (:h opts))
      (println (show-help test-cli-spec))
      (try (let [parsed (cljconf.bb.api/parse args opts)]
             (if-bb-cli
               (println (pr-str parsed))
               parsed))
           (catch Exception e
             (if-bb-cli
               (do
                 (println (ex-message e))
                 (System/exit 1))
               (throw e)))))))

(defn -main
  [& args]
  (let [command (keyword (first args))
        args (rest args)]
    (case command
      :test (test args)
      :parse (parse args))))

(comment
  (-main "test"
         "test.yaml"
         "--policy" "test/ilmoraunio/cljconf/example_rules.clj"
         "--config" "cljconf.edn")
  ;; ```
  ;; $ bb test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj --config cljconf.edn
  ;; $ bb --jar target/cljconf.jar test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj --config cljconf.edn
  ;; $ ./cljconf test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj --config cljconf.edn
  ;; $ ./cljconf test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj --config cljconf.edn --fail-on-warn
  ;; ```
  )