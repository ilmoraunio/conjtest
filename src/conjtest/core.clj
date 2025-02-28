(ns conjtest.core
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
        (some->> (:name m)
                 name
                 (re-find #"(^allow|^deny|^warn)-")
                 second
                 keyword))))

(defn rule?
  [x]
  (try (some? (rule-type x))
       (catch Exception _
         false)))

(defn rule-name
  [rule]
  (some-> (or (:name rule)
              (and (var? rule) (:name (var-get rule)))
              (:name (meta rule))
              (:rule/name (meta rule)))
          (name)))

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
  [inputs rule {:keys [trace] :as _opts}]
  (let [rule-type (or (rule-type rule) :deny)]
    (into (cond
            (map? inputs) {}
            (vector? inputs) [])
          (map (fn [input]
                 (let [rule-target (cond
                                     (map? inputs) (second input)
                                     (vector? inputs) input)
                       rule-name (rule-name rule)
                       result (try ((rule-function rule) rule-target)
                                   (catch Exception e
                                     (cond
                                       trace (with-out-str (clojure.stacktrace/print-stack-trace e))
                                       (instance? clojure.lang.ExceptionInfo (class e)) (ex-message e)
                                       :else (str e))))
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
                     (map? inputs) [(first input) [(cond-> {:message (when (true? failure)
                                                                       (or (string-or-nil result)
                                                                           (coll-or-nil result)
                                                                           (rule-message rule)
                                                                           :conjtest/rule-validation-failed))
                                                            :name rule-name
                                                            :rule-type rule-type
                                                            :failure? failure}
                                                     trace (assoc :result result
                                                                  :rule rule
                                                                  :rule-target rule-target))]]
                     (vector? inputs) (cond-> {:message (when (true? failure)
                                                          (or (string-or-nil result)
                                                              (coll-or-nil result)
                                                              (rule-message rule)
                                                              :conjtest/rule-validation-failed))
                                               :name rule-name
                                               :rule-type rule-type
                                               :failure? failure}
                                        trace (assoc :result result
                                                     :rule rule
                                                     :rule-target rule-target)))))
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
   (cond
     (and filename rule-type name message)
     (format "%s - %s - %s - %s"
             (case rule-type
               (:allow :deny) "FAIL"
               :warn "WARN")
             filename
             name
             message)
     (and filename rule-type message)
     (format "%s - %s - %s"
             (case rule-type
               (:allow :deny) "FAIL"
               :warn "WARN")
             filename
             message)))
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

(defn -trace-entries
  [filename results]
  (map (fn [{:keys [name rule-target result] :as _rule-eval}]
         (format "Rule name: %s\nInput file: %s\nParsed input: %sResult: %s"
                 name
                 filename
                 (with-out-str (clojure.pprint/pprint rule-target))
                 (pr-str result)))
       results))

(defn -trace-report
  [result]
  (->> (cond
         (map? result) (mapcat (fn [[filename results]]
                                 (-trace-entries filename results))
                               result)
         (coll? result) (-trace-entries nil result))
       (string/join "\n---\n")
       (format "---\n%s\n---\n")))

(defn -summary-report
  [result {:keys [trace] :as _opts}]
  (let [summary (-summary result)
        summary-text (format "%d tests, %d passed, %d warnings, %d failures"
                             (:total summary)
                             (:passed summary)
                             (:warnings summary)
                             (:failures summary))]
    (cond-> {:summary summary
             :summary-report (when summary (format "%s\n" summary-text))
             :result result}
      trace (assoc :trace-report (-trace-report result)))))

(defn -failure-report
  [result {:keys [trace] :as opts}]
  (let [failures-text (->> result
                           (mapcat (fn [[filename results]]
                                     (keep (fn [{:keys [failure?] :as rule-eval}]
                                             (when failure?
                                               (-format-message filename rule-eval)))
                                           results)))
                           (string/join "\n")
                           (format "%s\n"))
        summary-report (-summary-report result opts)]
    (cond-> {:summary (:summary summary-report)
             :failure-report (when (:summary summary-report)
                               (format "%s\n%s" failures-text (:summary-report summary-report)))
             :result result}
      trace (assoc :trace-report (-trace-report result)))))

(defn filter-results
  [results {:keys [fail-on-warn]}]
  (filter #(and ((cond-> #{:allow :deny}
                   fail-on-warn (conj :warn)) (:rule-type %))
                (:failure? %))
          results))

(defn any-failures?
  [result opts]
  (boolean (not-empty
             (mapcat
               (cond
                 (map? result) (fn [[_filename evaluations]] (filter-results evaluations opts))
                 (coll? result) (fn [evaluations] (filter-results evaluations opts)))
               result))))

(defn test-with-opts
  ([inputs rules]
   (test-with-opts inputs rules nil))
  ([inputs rules opts]
   (let [result (cond
                  (map? inputs)
                  (apply merge-with into (map #(-test inputs % opts)
                                              (resolve-functions rules)))
                  (vector? inputs)
                  (mapcat identity (keep (comp not-empty #(-test inputs % opts))
                                         (resolve-functions rules))))]
     (if (any-failures? result opts)
       (-failure-report result opts)
       (-summary-report result opts)))))

(defn test
  [inputs & rules]
  (test-with-opts inputs rules))

(defn test-with-opts!
  ([inputs rules]
   (test-with-opts! inputs rules nil))
  ([inputs rules opts]
   (let [{:keys [failure-report summary-report summary trace-report] :as report} (test-with-opts inputs rules opts)]
     (when trace-report
       (println (format "TRACE:\n%s" trace-report)))
     (cond
       (some? failure-report) (throw (ex-info failure-report summary))
       (some? summary-report) report))))

(defn test!
  [inputs & rules]
  (test-with-opts! inputs rules))
