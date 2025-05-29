(ns policy)

(defn deny-incorrect-sha
  [input]
  (let [expected-version "sha256:d7ec60cf8390612b360c857688b383068b580d9a6ab78417c9493170ad3f1616"
        version (get-in input [:bom :metadata :component :version])]
    (when (not= version expected-version)
      (format "current SHA256 %s is not equal to expected SHA256 %s" version expected-version))))