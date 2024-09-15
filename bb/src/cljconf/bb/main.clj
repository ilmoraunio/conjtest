(ns cljconf.bb.main
  (:require [babashka.cli :as cli]
            [clojure.string :as str]
            [cljconf.bb.api]))

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
          :help {:coerce :boolean
                 :alias :h}}
   :restrict [:parser :policy :help]})

(defn test
  [m]
  (let [{:keys [#_args opts] :as m} (cli/parse-args m test-cli-spec)]
    (prn ::parse-args m)
    (if (or (:help opts) (:h opts))
      (println (show-help test-cli-spec))
      (cljconf.bb.api/test m))))

(defn -main
  [command args]
  (prn ::args args)
  (case command
    :test (test args)))