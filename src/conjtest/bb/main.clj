(ns conjtest.bb.main
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [babashka.tasks :as tasks]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [conjtest.bb.api]
            [conjtest.bb.repl :as repl]))

(defmacro if-bb-cli
  [then else]
  (if (and (System/getProperty "babashka.version")
           (not (System/getenv "CONJTEST_DEV")))
    then
    else))

(defn format-table
  "A vendored version of `babashka.cli/format-table` (omits invoking `babashka.cli/pad-cells` for `rows`)."
  [{:keys [rows indent]}]
  (let [fmt-row (fn [leader divider trailer row]
                  (str leader
                       (apply str (interpose divider row))
                       trailer))]
    (->> rows
         (map (fn [row]
                #_(fmt-row "| " " | " " |" row)
                (fmt-row (apply str (repeat indent " ")) " " "" row)))
         (map str/trimr)
         (str/join "\n"))))

(defn show-help
  ([]
   ;; root-level help
   (let [command-rows (cli/pad-cells [["help" " " "Show available commands"]
                                      ["init" " " "Creates a default conjtest.edn configuration file"]
                                      ["test" " " "Tests configuration files against Clojure policies"]
                                      ["parse" " " "Parses configuration files and prints them out as Clojure data structures"]
                                      ["repl" " " "Opens up a nREPL session inside conjtest allowing"]
                                      ["version" " " "Print CLI version"]])
         rows (-> [["Test your configuration files using Clojure!"]
                   []
                   ["Usage:"]
                   ["conjtest [command]"]
                   []
                   ["Available commands:"]]
                  (into command-rows)
                  (into [[]
                         ["See `conjtest [command] --help` for more information about a command."]]))]
     (format-table {:rows rows
                    :indent 2})))
  ([spec]
   (format
     "%s\n\n%s"
     (format-table {:rows (:desc spec)
                    :indent 2})
     (cli/format-opts (merge spec {:order (vec (keys (:spec spec)))})))))

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

(def init-cli-spec
  {:desc [["Creates a default conjtest.edn configuration file"]
          []
          ["Usage:"]
          ["conjtest init"]
          []
          ["Config file defaults:"]
          [":keywordize? true (enables keyworded keys for all parse outputs)"]]
   :spec {:help {:coerce :boolean
                 :alias :h}}
   :restrict [:help]})

(def test-cli-spec
  {:desc [["Tests configuration files against Clojure policies"]
          []
          ["Usage:"]
          ["conjtest test <configuration_file> [configuration_file [...]] [flags]"]
          []
          ["Examples:"]
          ["conjtest test deployment.yaml --policy policy.clj"]
          ["conjtest test deployment.yaml --policy policy.clj --policy another-policy.clj"]
          ["conjtest test deployment.yaml --policy policies/*.clj"]
          ["conjtest test hocon.conf --policy policy.clj --parser hocon"]
          ["conjtest test deployment.yaml --policy policy.clj --config conjtest.edn"]
          []
          ["Config file can be provided to support local file requires via `:paths`. Eg:"]
          ["{:paths [\"util/\"]}"]
          []
          ["For keyworded keys, include `:keywordize?` in config file. Eg:"]
          ["{:keywordize? true}"]]
   :spec {:config {:coerce :string
                   :alias :c
                   :desc "Filepath to configuration file. Defaults to conjtest.edn in current dir."
                   :default "./conjtest.edn"
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
                   :desc "Use specific parser to parse files. Supported parsers: cue, dockerfile, edn, hcl1, hcl2, hocon, ignore, ini, json, jsonnet, properties, spdx, toml, vcl, xml, yaml, dotenv"
                   :validate {:pred (complement empty?)
                              :ex-msg (constantly "--parser must be non-empty string")}}
          :policy {:coerce #{:string}
                   :alias :p
                   :desc "Filepath to look for policy files, supports globs, defaults to current dir. Accepts file and glob pattern."
                   :default #{"*"}
                   :default-desc ""
                   :validate {:pred validate-policy
                              :ex-msg (constantly "--policy must be non-empty string")}}
          :trace {:coerce :boolean
                  :desc "Enable more verbose trace output"
                  :default nil
                  :default-desc ""}}
   :restrict [:config :fail-on-warn :go-parsers-only :help :parser :policy :trace]})

(def parse-cli-spec
  {:desc [["Parses configuration files and prints them out as Clojure data structures"]
          []
          ["Usage:"]
          ["conjtest parse <configuration_file> [configuration_file [...]] [flags]"]
          []
          ["Examples:"]
          ["conjtest parse deployment.yaml"]
          ["conftest parse hocon.conf --parser hocon"]
          ["conjtest parse deps.edn --parser edn --go-parsers-only"]
          ["conjtest parse Dockerfile --config conjtest.edn"]
          []
          ["For keyworded keys, include `:keywordize?` in config file. Eg:"]
          ["{:keywordize? true}"]]
   :spec {:config {:coerce :string
                   :alias :c
                   :desc "Filepath to configuration file. Defaults to conjtest.edn in current dir."
                   :default "./conjtest.edn"
                   :default-desc ""
                   :validate {:pred (complement empty?)
                              :ex-msg "--config must be non-empty string"}}
          :go-parsers-only {:coerce :boolean
                            :alias :go
                            :desc "Use Go-based parsers only"}
          :help {:coerce :boolean
                 :alias :h}
          :parser {:coerce :string
                   :desc "Use specific parser to parse files. Supported parsers: cue, dockerfile, edn, hcl1, hcl2, hocon, ignore, ini, json, jsonnet, properties, spdx, toml, vcl, xml, yaml, dotenv"
                   :validate {:pred (complement empty?)
                              :ex-msg (constantly "--parser must be non-empty string")}}}
   :restrict [:config :go-parsers-only :help :parser]})

(defn init
  [args]
  (let [{:keys [args opts] :as m} (cli/parse-args args init-cli-spec)]
    (if (or (:help opts) (:h opts))
      (println (show-help init-cli-spec))
      (let [filename "conjtest.edn"
            default-conjtest "{:keywordize? true}"]
        (if-not (fs/exists? filename)
          (do
            (println "Creating conjtest.edn...")
            (try
              (spit filename default-conjtest)
              (catch Exception e
                (if-bb-cli
                  (do
                    (println "Something went wrong")
                    (System/exit 1))
                  (throw e))))
            (println "Done!")
            (if-bb-cli
              (System/exit 0)
              nil))
          (do
            (println (format "File '%s' already exists" filename))
            (if-bb-cli
              (System/exit 1)
              (throw (ex-info "existing file" {:filename filename})))))))))

(defn test
  [args]
  (let [{:keys [args opts] :as m} (cli/parse-args args test-cli-spec)]
    (if (or (:help opts) (:h opts) (empty? args))
      (println (show-help test-cli-spec))
      (try (println (:summary-report (conjtest.bb.api/test! args opts)))
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
    (if (or (:help opts) (:h opts) (empty? args))
      (println (show-help parse-cli-spec))
      (try (let [parsed (conjtest.bb.api/parse args opts)]
             (if-bb-cli
               (pprint/pprint
                 (if (= (count parsed) 1)
                   (first (vals parsed))
                   parsed))
               parsed))
           (catch Exception e
             (if-bb-cli
               (do
                 (println (ex-message e))
                 (System/exit 1))
               (throw e)))))))

(defn version
  []
  (let [version (-> (slurp (clojure.java.io/resource "CONJTEST_VERSION"))
                    (str/trim))]
    (println version)
    (if-bb-cli
      (System/exit 0)
      (do))))

(defn -main
  [& args]
  (let [command (keyword (first args))
        args (rest args)]
    (case command
      :help (println (show-help))
      :init (init args)
      :test (test args)
      :parse (parse args)
      :repl (repl/-main)
      :version (version)
      (println (show-help)))))
