(ns policy)

(defn deny-incorrect-compiler-plugin-version
  [input]
  (let [expected-version "3.6.1"]
    (when-let [matches (seq (for [plugin (get-in input ["project" "build" "plugins" "plugin"])
                                  :when (and (= (get plugin "artifactId") "maven-compiler-plugin")
                                             (not= (get plugin "version") expected-version))]
                              (format "maven-compiler-plugin must have the following version: %s" expected-version)))]
      (clojure.string/join "\n" matches))))

(defn deny-missing-instrument-goal
  [input]
  (when-let [matches (seq (for [plugin (get-in input ["project" "build" "plugins" "plugin"])
                            :when (and (= (get plugin "artifactId") "activejdbc-instrumentation")
                                       (not= "instrument" (get-in plugin ["executions" "execution" "goals" "goal"])))]
                        (format "activejdbc-instrumentation plugin must have 'instrument' goal")))]
    (clojure.string/join "\n" matches)))

(defn deny-incorrect-surefire-plugin-version
  [input]
  (let [expected-version "2.18.1"]
    (when-let [matches (seq (for [plugin (get-in input ["project" "build" "plugins" "plugin"])
                                  :when (and (= (get plugin "artifactId") "maven-surefire-plugin")
                                             (not= (get plugin "version") expected-version))]
                              (format "maven-surefire-plugin must have the following version: %s" expected-version)))]
      matches)))