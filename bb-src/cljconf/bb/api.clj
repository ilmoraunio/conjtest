(ns cljconf.bb.api
  (:require [babashka.fs :as fs]
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

(defn eval-and-get-rules
  "Evaluates all policies inside a sci context and returns rules"
  [policies]
  (let [ctx (sci/init {})]
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
        rules (eval-and-get-rules policies)
        result (->> rules
                    (map (partial cljconf/test inputs))
                    (apply merge-with into))]
    (println result)
    {:inputs inputs
     :policies policies
     :rules rules
     :result result}))

(comment
  (test {:args ["test.yaml"], :opts {:policy #{"test/ilmoraunio/cljconf/example_rules.clj"}}})
  ;; ```
  ;; $ bb test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj
  ;; $ bb --jar target/cljconf.jar test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj
  ;; $ ./cljconf test test.yaml --policy test/ilmoraunio/cljconf/example_rules.clj
  ;; ```
  )