(ns policy-go)

(defn deny-python2.7
  [input]
  (when (= "python2.7" (get-in input [:provider :runtime]))
    "Python 2.7 cannot be the default provider runtime"))

(defn deny-functions-python2.7
  [input]
  (when (seq (for [[_ function] (:functions input)
                   :when (= "python2.7" (:runtime function))]
               function))
    "Python 2.7 cannot be used as the runtime for functions"))

(defn deny-missing-tags
  [input]
  (when-not (get-in input [:provider :tags :author])
    "Should set provider tags for author"))