(ns conjtest.bb.util)

(def debug? true)

(defn debug
  [& msg]
  (when debug?
    (binding [*out* *err*]
      (apply println msg))))