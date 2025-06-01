(ns policy)

(defn deny-no-id-rsa-files-ignored
  [input]
  (let [matches (for [document input
                      {kind :Kind value :Value :as entry} document
                      :when (and (= kind "Path")
                                 (= value "id_rsa"))]
                  entry)]
    (when (empty? matches)
      "id_rsa files should be ignored")))