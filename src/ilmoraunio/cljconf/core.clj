(ns ilmoraunio.cljconf.core
  (:refer-clojure :exclude [test])
  (:require [clojure.string]
            [malli.core :as m]))

(defn string-or-nil
  [x]
  (when (string? x) x))

(defn rule-type
  [rule]
  (let [m (meta rule)]
    (or (:type rule)
        (:type (var-get rule))
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
            (:name (var-get rule))
            (:name (meta rule)))))

(defn rule-message
  [rule]
  (or (:message rule)
      (:message (var-get rule))
      (:rule/message (meta rule))))

(defn rule-function
  [rule]
  (let [f-or-schema (or (:rule rule)
                        (:rule (var-get rule))
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
                                          :allow (or (not result) (string? result))
                                          (:warn :deny) result))]
                   (cond
                     (map? inputs) [(first input) [{:message (when (true? failure)
                                                               (or (string-or-nil result)
                                                                   (rule-message rule)
                                                                   :cljconf/rule-validation-failed))
                                                    :name (rule-name rule)
                                                    :rule-type rule-type
                                                    :rule-target (ffirst inputs)
                                                    :failure? failure}]]
                     (vector? inputs) {:message (when (true? failure)
                                                  (or (string-or-nil result)
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
  [& xs]
  (->> (mapcat (fn [x]
                 (cond
                   (map? x) [x]
                   (var? x) (filter rule? [x])
                   (instance? clojure.lang.Namespace x) (resolve-ns-functions x)
                   (symbol? x) (resolve-ns-functions x)))
               xs)
       (sort #(compare (str %1) (str %2)))
       (dedupe)))

(defn test
  [inputs & xs]
  (cond
    (map? inputs)
    (apply merge-with into (map (partial -test inputs)
                                (apply resolve-functions xs)))
    (vector? inputs)
    (mapcat identity (keep (comp not-empty (partial -test inputs))
                           (apply resolve-functions xs)))))