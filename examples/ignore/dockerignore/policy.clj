(ns policy)

(defn deny-git-not-ignored
  [input]
  (let [matches (for [document input
                      {kind "Kind" value "Value" :as entry} document
                      :when (and (= kind "Path")
                                 (= value ".git"))]
                  entry)]
    (when (empty? matches)
      ".git directories should be ignored")))