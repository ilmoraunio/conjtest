(ns ilmoraunio.cljconf.core
  (:refer-clojure :exclude [test])
  (:require [clojure.string]))

(defn string-or-nil
  [x]
  (when (string? x) x))

(defn rule?
  [x]
  (or (:rule/type (meta x))
      (clojure.string/starts-with? (str (:name (meta x))) "allow-")
      (clojure.string/starts-with? (str (:name (meta x))) "deny-")))

(defn rule-type
  [f]
  (let [m (meta f)]
    (or (:rule/type m)
        (keyword ((fnil (partial re-find #"^allow|^deny") "") (name (:name m))))
        (throw (ex-info "invalid rule" {:rule f})))))

(defn rule-name
  [f]
  (name (:name (meta f))))

(defn -test
  [rules f]
  (let [rule-type (rule-type f)]
    (into {}
          (keep (fn [[filename input]]
                  (let [result (f input)]
                    (when (case rule-type
                            :allow (or (not result) (string? result))
                            :deny result)
                      [filename [{:message (or (string-or-nil result)
                                               (:rule/message (meta f))
                                               :cljconf/rule-validation-failed)
                                  :name (rule-name f)
                                  :rule-type rule-type}]])))
                rules))))

(defn resolve-ns-functions
  [namespace]
  (->> (filter (comp rule? second) (ns-publics namespace))
       (map second)))

(defn resolve-functions
  [& xs]
  (->> (mapcat (fn [x]
                 (cond (var? x) (filter rule? [x])
                   (instance? clojure.lang.Namespace x) (resolve-ns-functions x)
                   (symbol? x) (resolve-ns-functions x)))
           xs)
       (sort #(compare (str %1) (str %2)))
       (dedupe)))

(defn test
  [rules & xs]
  (apply merge-with into (map (partial -test rules)
                              (apply resolve-functions xs))))