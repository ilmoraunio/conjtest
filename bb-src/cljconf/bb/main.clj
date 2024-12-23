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
  {:spec {:parser {:coerce :string
                   :desc "Explicitly select parser to deserialize input files"
                   :validate {:pred (complement empty?)
                              :ex-msg (constantly "--parser must be non-empty string")}}
          :policy {:coerce #{:string}
                   :alias :p
                   :desc "Filepath to look for policy files, supports globs, defaults to current dir"
                   :default #{"*"}
                   :default-desc ""
                   :validate {:pred validate-policy
                              :ex-msg (constantly "--policy must be non-empty string")}}
          :config {:coerce :string
                   :alias :c
                   :desc "Filepath to configuration file"
                   :default nil
                   :default-desc ""
                   :validate {:pred (complement empty?)
                              :ex-msg "--config must be non-empty string"}}
          :help {:coerce :boolean
                 :alias :h}}
   :restrict [:parser :policy :config :help]})

(defn test
  [args]
  (prn "args" args)
  (let [{:keys [#_args opts] :as m} (cli/parse-args args test-cli-spec)]
    (prn ::parse-args m)
    (if (or (:help opts) (:h opts))
      (println (show-help test-cli-spec))
      (try (cljconf.bb.api/test! m)
           (catch Exception e
             (print (ex-message e))
             (if-bb-cli (System/exit 1) nil))))))

(defn -main
  [& args]
  (let [command (keyword (first args))
        args (rest args)]
    (case command
      :test (test args))))

(comment
  (-main "test"
         "test.yaml"
         "--policy" "test/ilmoraunio/cljconf/example_rules.clj"
         "--config" "cljconf.edn")
  ;; ```
  ;; $ bb test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj --config cljconf.edn
  ;; $ bb --jar target/cljconf.jar test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj --config cljconf.edn
  ;; $ ./cljconf test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj --config cljconf.edn
  ;; ```
  )