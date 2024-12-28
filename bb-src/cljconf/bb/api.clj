(ns cljconf.bb.api
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.string :as str]
            [ilmoraunio.cljconf.core :as cljconf]
            [pod-ilmoraunio-conftest-clj.api :as api]
            [sci.core :as sci]))

(def default-namespaces
  #{"user"
    "clojure.core"
    "clojure.set"
    "clojure.edn"
    "clojure.repl"
    "clojure.string"
    "clojure.walk"
    "clojure.template"})

(defn sci-eval
  [ctx s]
  (sci/eval-string* ctx (slurp s)))

(defn path->symbol
  [deps-paths path]
  (-> path
      (str/replace (re-pattern (format "^(%s)/" (str/join "|" (map str deps-paths)))) "")
      (str/replace #"\.(cljc|clj|bb)?$" "")
      (str/replace #"/" ".")
      (str/replace #"_" "-")
      symbol))

(defn namespace->source
  [config-filename]
  (let [deps-paths (some-> config-filename slurp edn/read-string :paths)
        source-m (->> deps-paths
                      (mapcat #(fs/glob % "**{.clj,.cljc,.bb}"))
                      (map str)
                      (map #(-> {:file (fs/file-name %)
                                 :source (slurp %)}))
                      (map #(-> [(->> %
                                      :source
                                      (re-find #"ns\s+([a-zA-Z][^\s^{}()\[\]]+)")
                                      second
                                      (path->symbol deps-paths)) %]))
                      (into {}))]
    (fn [{:keys [namespace]}] (source-m namespace))))

(defn eval-and-resolve-vars
  "Evaluates all fns inside a sci context and returns vars"
  [{:keys [config] :as _opts} policies]
  (let [ctx (sci/init (cond-> {:namespaces {'clojure.core {'abs abs
                                                           'add-tap add-tap
                                                           'file-seq file-seq
                                                           'infinite? infinite?
                                                           'iteration iteration
                                                           'NaN? NaN?
                                                           'parse-boolean parse-boolean
                                                           'parse-double parse-double
                                                           'parse-long parse-long
                                                           'parse-uuid parse-uuid
                                                           'random-uuid random-uuid
                                                           'remove-tap remove-tap
                                                           'slurp slurp
                                                           'tap> tap>
                                                           'update-keys update-keys
                                                           'update-vals update-vals}}}
                        (some? config) (assoc :load-fn (namespace->source config))))]
    (doseq [policy policies]
      (sci-eval ctx policy))
    (let [user-defined-namespaces (remove (comp default-namespaces str)
                                          (sci/eval-string* ctx "(all-ns)"))
          command (format "(map #(-> {:ns %% :ns-publics (ns-publics %%)}) [%s])"
                          (str/join " " (mapv #(str "'" %) user-defined-namespaces)))
          ns-publics-all (sci/eval-string* ctx command)]
      (mapcat (comp vals :ns-publics) ns-publics-all))))

(defn -summary
  [result]
  (reduce (fn [m [_filename results]]
            (-> m
                (update :total (partial + (count results)))
                (update :passed (partial + (count (remove :failure? results))))
                (update :warnings (partial + (count (filter #(and (#{:warn} (:rule-type %))
                                                                  (:failure? %))
                                                            results))))
                (update :failures (partial + (count (filter #(and (#{:allow :deny} (:rule-type %))
                                                                  (:failure? %))
                                                            results))))))
          {:total 0 :passed 0 :warnings 0 :failures 0}
          result))

(defn -summary-report
  [result]
  (let [summary (-summary result)
        summary-text (format "%d tests, %d passed, %d warnings, %d failures"
                             (:total summary)
                             (:passed summary)
                             (:warnings summary)
                             (:failures summary))]
    {:summary summary
     :summary-report (format "%s\n" summary-text)}))

(defn -failure-report
  [result]
  (let [failures-text (->> result
                           (mapcat (fn [[filename results]]
                                     (keep (fn [{:keys [message name rule-type failure?]}]
                                             (when failure?
                                               (format "%s - %s - %s - %s"
                                                       (case rule-type
                                                         (:allow :deny) "FAIL"
                                                         :warn "WARN")
                                                       filename
                                                       name
                                                       message)))
                                           results)))
                           (string/join "\n")
                           (format "%s\n"))
        summary-report (-summary-report result)]
    {:failure-report (format "%s\n%s" failures-text (:summary-report summary-report))
     :summary (:summary summary-report)
     :summary-report (:report summary-report)}))

(defn test
  [{:keys [args opts]}]
  (let [inputs (apply api/parse args)
        policies (->> (:policy opts)
                      (mapcat (partial fs/glob "."))
                      (filter #(-> % fs/extension #{"clj" "bb" "cljc"}))
                      (mapv str))
        vars (eval-and-resolve-vars opts policies)]
    {:result (apply cljconf/test inputs vars)}))

(defn any-failures?
  [{:keys [fail-on-warn]} result]
  (boolean (not-empty
             (mapcat
               (fn [[_filename results]]
                 (filter #(and ((cond-> #{:allow :deny}
                                  fail-on-warn (conj :warn)) (:rule-type %))
                               (:failure? %))
                         results))
               result))))

(defn test!
  ([m]
   (test! {} m))
  ([opts m]
   (let [{:keys [result]} (test m)]
     (if (any-failures? opts result)
       (let [failure-report (-failure-report result)]
         (throw (ex-info (:failure-report failure-report) (select-keys failure-report [:summary]))))
       (-summary-report result)))))