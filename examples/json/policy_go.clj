(ns policy-go)

(defn deny-caret-ranges
  [input]
  (for [[_lib-name lib-version :as lib] (:dependencies input)
        :when (clojure.string/starts-with? lib-version "^")]
    (format "caret ranges not allowed, offending library: %s" lib)))