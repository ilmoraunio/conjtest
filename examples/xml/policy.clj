(ns policy)

(def expected-maven-surefire-plugin-version "2.18.1")
(def expected-maven-compiler-plugin-version "2.5.1")

(defn deny-incorrect-compiler-plugin-version
  [input]
  (for [plugin (get-in input [:project :build :plugins :plugin])
        :when (and (= (:artifactId plugin) "maven-compiler-plugin")
                   (not= (:version plugin) expected-maven-compiler-plugin-version))]
    (format "maven-compiler-plugin must have the following version: %s" expected-maven-compiler-plugin-version)))

(defn deny-missing-instrument-goal
  [input]
  (for [plugin (get-in input [:project :build :plugins :plugin])
        :when (and (= (:artifactId plugin) "activejdbc-instrumentation")
                   (not= "instrument" (get-in plugin [:executions :execution :goals :goal])))]
    (format "activejdbc-instrumentation plugin must have 'instrument' goal")))

(defn deny-incorrect-surefire-plugin-version
  [input]
  (for [plugin (get-in input [:project :build :plugins :plugin])
        :when (and (= (:artifactId plugin) "maven-surefire-plugin")
                   (not= (:version plugin) expected-maven-surefire-plugin-version))]
    (format "maven-surefire-plugin must have the following version: %s" expected-maven-surefire-plugin-version)))

(def allow-declarative-example
  [:map
   [:project
    [:map
     [:build
      [:map
       [:plugins
        [:map
         [:plugin [:vector [:multi {:dispatch :artifactId}
                            ["activejdbc-instrumentation" [:map
                                                           [:executions
                                                            [:map
                                                             [:execution
                                                              [:map
                                                               [:goals
                                                                [:map
                                                                 [:goal [:= {:error/message "activejdbc-instrumentation plugin must have 'instrument' goal"}
                                                                         "instrument"]]]]]]]]]]
                            ["maven-compiler-plugin" [:map
                                                      [:version [:= {:error/fn (fn [{:keys [value]} _] (format "incorrect maven-compiler-plugin version '%s', should be '%s'" value expected-maven-compiler-plugin-version))}
                                                                 expected-maven-compiler-plugin-version]]]]
                            ["maven-surefire-plugin" [:map
                                                      [:version [:= {:error/fn (fn [{:keys [value]} _] (format "incorrect maven-surefire-plugin version '%s', should be '%s'" value expected-maven-surefire-plugin-version))}
                                                                 expected-maven-surefire-plugin-version]]]]
                            [:malli.core/default :map]]]]]]]]]]])