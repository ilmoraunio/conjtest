(ns policy)

(defn deny-caret-ranges
  [input]
  (when-let [matches (seq (for [[_lib-name lib-version :as lib] (:dependencies input)
                                :when (clojure.string/starts-with? lib-version "^")]
                            lib))]
    (format "caret ranges not allowed, offending libraries: %s" matches)))