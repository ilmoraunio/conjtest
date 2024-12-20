(ns cljconf.bb.api
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
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
      (str/replace #"\.cljc?$" "")
      (str/replace #"/" ".")
      (str/replace #"_" "-")
      symbol))

(defn namespace->source
  [deps-filename]
  (let [deps-paths (some-> deps-filename slurp edn/read-string :paths)
        source-m (->> deps-paths
                      (mapcat #(fs/glob % "**{.clj,cljc}"))
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
  [{:keys [deps] :as _opts} policies]
  (let [ctx (sci/init (cond-> {} (some? deps) (assoc :load-fn (namespace->source deps))))]
    (doseq [policy policies]
      (sci-eval ctx policy))
    (let [user-defined-namespaces (remove (comp default-namespaces str)
                                          (sci/eval-string* ctx "(all-ns)"))
          command (format "(map #(-> {:ns %% :ns-publics (ns-publics %%)}) [%s])"
                          (str/join " " (mapv #(str "'" %) user-defined-namespaces)))
          ns-publics-all (sci/eval-string* ctx command)]
      (mapcat (comp vals :ns-publics) ns-publics-all))))

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

(comment
  (test {:args ["test.yaml"]
         :opts {:policy #{"test/ilmoraunio/cljconf/example_rules.clj"}
                :deps "deps.edn"}})
  ;; ```
  ;; $ bb test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj
  ;; $ bb --jar target/cljconf.jar test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj
  ;; $ ./cljconf test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj
  ;; ```
  )