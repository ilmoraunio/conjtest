(ns conjtest.example-broken-rules)

(defn deny-will-trigger-with-exception
  [input]
  (clojure.string/starts-with? (:nilly-field input) "foo"))

(defn deny-will-trigger-cleanly
  [input]
  true)

(defn deny-will-not-trigger
  [input]
  false)
