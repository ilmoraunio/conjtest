(ns policy-go)

(defn deny-latest-tags
  [input]
  (let [services (:services input)
        images (->> services vals (keep :image))]
    (when (seq (filter #(clojure.string/ends-with? % ":latest") images))
      "No images tagged latest")))

(defn deny-old-compose-versions
  [input]
  (let [version (parse-double (:version input))]
    (when (< version 3.5)
      "Must be using at least version 3.5 of the Compose file format")))