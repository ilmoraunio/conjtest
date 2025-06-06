(ns policy)

(defn deny-invalid-uri
  [input]
  (for [[key value] input
        :when (and (clojure.string/includes? (clojure.string/lower-case (name key)) "url")
                   (not (clojure.string/includes? (clojure.string/lower-case value) "http")))]
    (format "Must have a valid uri defined '%s'" value)))

(defn deny-secrets
  [input]
  (for [[key _value] input
        :when (and (not= "secret.value.exception" (name key))
                   (clojure.string/includes? (clojure.string/lower-case (name key)) "secret"))]
    (format "'%s' may contain a secret value" key)))