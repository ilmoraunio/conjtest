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
  (let [ctx (sci/init (cond-> {} (some? config) (assoc :load-fn (namespace->source config))))]
    (doseq [policy policies]
      (sci-eval ctx policy))
    (let [user-defined-namespaces (remove (comp default-namespaces str)
                                          (sci/eval-string* ctx "(all-ns)"))
          command (format "(map #(-> {:ns %% :ns-publics (ns-publics %%)}) [%s])"
                          (str/join " " (mapv #(str "'" %) user-defined-namespaces)))
          ns-publics-all (sci/eval-string* ctx command)]
      (mapcat (comp vals :ns-publics) ns-publics-all))))

(defn -summary-report
  [result]
  (let [summary (reduce (fn [m [_filename results]]
                          (-> m
                              (update :total (partial + (count results)))
                              (update :passed (partial + (count (remove :failure? results))))
                              (update :failures (partial + (count (filter :failure? results))))))
                        {:total 0 :passed 0 :failures 0}
                        result)
        summary-text (format "%d tests, %d passed, %d failures" (:total summary) (:passed summary) (:failures summary))]
    (format "%s\n" summary-text)))

(defn -failure-report
  [result]
  (let [failures-text (->> result
                           (mapcat (fn [[filename results]]
                                     (keep (fn [{:keys [message name failure?]}]
                                             (when failure?
                                               (format "FAIL - %s - %s - %s" filename name message)))
                                           results)))
                           (string/join "\n")
                           (format "%s\n"))]
    (format "%s\n%s" failures-text (-summary-report result))))

(defn test
  [{:keys [args opts]}]
  (let [inputs (apply api/parse args)
        policies (->> (:policy opts)
                      (mapcat (partial fs/glob "."))
                      (filter #(-> % fs/extension #{"clj" "bb" "cljc"}))
                      (mapv str))
        vars (eval-and-resolve-vars opts policies)
        result (->> vars
                    (map (partial cljconf/test inputs))
                    (apply merge-with into))]
    (println result)
    {:inputs inputs
     :policies policies
     :vars vars
     :result result}))

(defn any-failures?
  [result]
  (boolean (not-empty
             (mapcat
               (fn [[_filename results]]
                 (filter :failure? results))
               result))))

(defn test!
  [m]
  (let [{:keys [result]} (test m)]
    (if (any-failures? result)
      (throw (ex-info (-failure-report result) {}))
      (-summary-report result))))

(comment
  (test {:args ["test.yaml"]
         :opts {:policy #{"test/ilmoraunio/cljconf/example_rules.clj"}
                :config "cljconf.edn"}})
  ;; or
  (test! {:args ["test.yaml"]
         :opts {:policy #{"test/ilmoraunio/cljconf/example_rules.clj"}
                :config "cljconf.edn"}})
  )