(ns policy
  (:require [clojure.string]))

(defn deny-invalid-uri
  [input]
  (for [[key value] input
        :when (and (clojure.string/includes? (clojure.string/lower-case (name key)) "url")
                   (not (clojure.string/includes? (clojure.string/lower-case value) "https://")))]
    (format "Invalid value: '%s'. Must start with 'https://...'" value)))

(defn deny-secrets
  [input]
  (for [[key _value] input
        :when (and (not= "secret.value.exception" (name key))
                   (clojure.string/includes? (clojure.string/lower-case (name key)) "secret"))]
    (format "'%s' may contain a secret value" key)))

(def is-url-key )

(def allow-declarative-example
  [:map
   [:SAMPLE_VALUE :string]
   [:other.value.url [:re {:error/message "Must start with 'https://'"} "^https://"]]
   [:secret.value.exception :string]])