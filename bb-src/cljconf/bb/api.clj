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
  [policies {:keys [config] :as _opts}]
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

(defn parse
  [args {:keys [go-parsers-only parser] :as _opts}]
  (apply
    (cond
      (and (some? parser)
           (some? go-parsers-only)) (partial api/parse-go-as parser)
      (some? go-parsers-only) api/parse-go
      (some? parser) (partial api/parse-as parser)
      :else api/parse)
    args))

(defn test!
  [inputs {:keys [policy] :as opts}]
  (let [inputs (parse inputs opts)
        policies (->> policy
                      (mapcat (partial fs/glob "."))
                      (filter #(-> % fs/extension #{"clj" "bb" "cljc"}))
                      (mapv str))
        vars (eval-and-resolve-vars policies opts)]
    (cljconf/test-with-opts! inputs vars opts)))