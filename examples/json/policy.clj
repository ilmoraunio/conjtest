(ns policy)

(defn deny-caret-ranges
  [input]
  (for [[_lib-name lib-version :as lib] (:dependencies input)
        :when (clojure.string/starts-with? lib-version "^")]
    (format "caret ranges not allowed, offending library: %s" lib)))

(def allow-declarative-example
  [:map
   [:dependencies [:map-of :keyword [:re
                                     {:error/fn (fn [{:keys [value]} _]
                                                  (format "caret ranges not allowed, version found: %s" value))}
                                     "^[0-9~]|latest|beta|>="]]]])