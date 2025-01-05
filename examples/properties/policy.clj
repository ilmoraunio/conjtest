(ns policy)

(defn deny-invalid-uri
  [input]
  (when-let [matches (seq (for [[key value] input
                                :when (and (clojure.string/includes? (clojure.string/lower-case key) "url")
                                           (not (clojure.string/includes? (clojure.string/lower-case value) "http")))]
                            (format "Must have a valid uri defined '%s'" value)))]
    (clojure.string/join ", " matches)))

(defn deny-secrets
  [input]
  (when-let [matches (seq (for [[key _value] input
                                :when (and (not= "secret.value.exception" key)
                                           (clojure.string/includes? (clojure.string/lower-case key) "secret"))]
                            (format "'%s' may contain a secret value" key)))]
    (clojure.string/join ", " matches)))