(ns ilmoraunio.cljconf.core
  (:refer-clojure :exclude [test])
  (:require [clojure.string :as string]
            [clojure.string]
            [malli.core :as m]))

(defn string-or-nil
  [x]
  (when (string? x) x))

(defn coll-or-nil
  [x]
  (when (coll? x) x))

(defn rule-type
  [rule]
  (let [m (meta rule)]
    (or (:type rule)
        (and (var? rule) (:type (var-get rule)))
        (:rule/type m)
        (keyword (second ((fnil (partial re-find #"(^allow|^deny|^warn)-") "") (name (:name m)))))
        (throw (ex-info "invalid rule" {:rule rule})))))

(defn rule?
  [x]
  (try (some? (rule-type x))
       (catch Exception _
         false)))

(defn rule-name
  [rule]
  (name (or (:name rule)
            (and (var? rule) (:name (var-get rule)))
            (:name (meta rule))
            (:rule/name (meta rule))
            (throw (ex-info "rule name not found" {:rule rule})))))

(defn rule-message
  [rule]
  (or (:message rule)
      (and (var? rule) (:message (var-get rule)))
      (:rule/message (meta rule))))

(defn rule-function
  [rule]
  (let [f-or-schema (or (:rule rule)
                        (and (var? rule) (:rule (var-get rule)))
                        rule)]
    (if (vector? f-or-schema)
      (partial m/validate f-or-schema)
      f-or-schema)))

(defn -test
  [inputs rule]
  (let [rule-type (rule-type rule)]
    (into (cond
            (map? inputs) {}
            (vector? inputs) [])
          (map (fn [input]
                 (let [rule-target (cond
                                     (map? inputs) (second input)
                                     (vector? inputs) input)
                       result ((rule-function rule) rule-target)
                       failure (boolean (case rule-type
                                          :allow (or (not result)
                                                     (string? result)
                                                     (and (coll? result)
                                                          (not-empty result)))
                                          (:warn :deny) (when result
                                                          (if (coll? result)
                                                            (not-empty result)
                                                            true))))]
                   (cond
                     (map? inputs) [(first input) [{:message (when (true? failure)
                                                               (or (string-or-nil result)
                                                                   (coll-or-nil result)
                                                                   (rule-message rule)
                                                                   :cljconf/rule-validation-failed))
                                                    :name (rule-name rule)
                                                    :rule-type rule-type
                                                    :rule-target (ffirst inputs)
                                                    :failure? failure}]]
                     (vector? inputs) {:message (when (true? failure)
                                                  (or (string-or-nil result)
                                                      (coll-or-nil result)
                                                      (rule-message rule)
                                                      :cljconf/rule-validation-failed))
                                       :name (rule-name rule)
                                       :rule-type rule-type
                                       :rule-target rule-target
                                       :failure? failure})))
               inputs))))

(defn resolve-ns-functions
  [namespace]
  (->> (filter (comp rule? second) (ns-publics namespace))
       (map second)))

(defn resolve-functions
  [rules]
  (->> (mapcat (fn [x]
                 (cond
                   (map? x) [x]
                   (fn? x) [x]
                   (var? x) (filter rule? [x])
                   (instance? clojure.lang.Namespace x) (resolve-ns-functions x)
                   (symbol? x) (resolve-ns-functions x)))
               rules)
       (sort #(compare (str %1) (str %2)))
       (dedupe)))

(defn -format-message
  ([filename rule-type name message]
   (format "%s - %s - %s - %s"
           (case rule-type
             (:allow :deny) "FAIL"
             :warn "WARN")
           filename
           name
           message))
  ([filename {:keys [message name rule-type]}]
   (cond
     (or (string? message) (keyword? message)) (-format-message filename rule-type name message)
     (coll? message) (clojure.string/join "\n" (map (partial -format-message filename rule-type name) message)))))

(defn -count-results
  [m results]
  (-> m
      (update :total (partial + (count results)))
      (update :passed (partial + (count (remove :failure? results))))
      (update :warnings (partial + (count (filter #(and (#{:warn} (:rule-type %))
                                                        (:failure? %))
                                                  results))))
      (update :failures (partial + (count (filter #(and (#{:allow :deny} (:rule-type %))
                                                        (:failure? %))
                                                  results))))))

(def initial-count-state {:total 0 :passed 0 :warnings 0 :failures 0})

(defn -summary
  [result]
  (cond
    (map? result) (reduce (fn [m [_filename results]] (-count-results m results)) initial-count-state result)
    (coll? result) (-count-results initial-count-state result)))

(defn -summary-report
  [result]
  (let [summary (-summary result)
        summary-text (format "%d tests, %d passed, %d warnings, %d failures"
                             (:total summary)
                             (:passed summary)
                             (:warnings summary)
                             (:failures summary))]
    {:summary summary
     :summary-report (format "%s\n" summary-text)
     :result result}))

(defn -failure-report
  [result]
  (let [failures-text (->> result
                           (mapcat (fn [[filename results]]
                                     (keep (fn [{:keys [failure?] :as rule-eval}]
                                             (when failure?
                                               (-format-message filename rule-eval)))
                                           results)))
                           (string/join "\n")
                           (format "%s\n"))
        summary-report (-summary-report result)]
    {:summary (:summary summary-report)
     :failure-report (format "%s\n%s" failures-text (:summary-report summary-report))
     :result result}))

(defn filter-results
  [{:keys [fail-on-warn]} results]
  (filter #(and ((cond-> #{:allow :deny}
                   fail-on-warn (conj :warn)) (:rule-type %))
                (:failure? %))
          results))

(defn any-failures?
  [opts result]
  (boolean (not-empty
             (mapcat
               (cond
                 (map? result) (fn [[_filename evaluations]] (filter-results opts evaluations))
                 (coll? result) (fn [evaluations] (filter-results opts evaluations)))
               result))))

(defn test-with-opts
  [inputs rules & opts]
  (let [result (cond
                 (map? inputs)
                 (apply merge-with into (map (partial -test inputs)
                                             (resolve-functions rules)))
                 (vector? inputs)
                 (mapcat identity (keep (comp not-empty (partial -test inputs))
                                        (resolve-functions rules))))]
    (if (any-failures? opts result)
      (-failure-report result)
      (-summary-report result))))

(defn test
  [inputs & rules]
  (test-with-opts inputs rules))

(defn test-with-opts!
  [inputs rules & opts]
  (let [{:keys [failure-report summary-report summary] :as report} (test-with-opts inputs rules opts)]
    (cond
      (some? summary-report) report
      (some? failure-report) (throw (ex-info failure-report summary)))))

(defn test!
  [inputs & rules]
  (test-with-opts! inputs rules))