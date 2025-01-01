(ns policy)

(defn deny-latest-tags
  [input]
  (let [services (get-in input ["services"])
        images (->> services vals (keep #(get % "image")))]
    (when (seq (filter #(clojure.string/ends-with? % ":latest") images))
      "No images tagged latest")))

(defn deny-old-compose-versions
  [input]
  (let [version (parse-double (get input "version"))]
    (when (< version 3.5)
      "Must be using at least version 3.5 of the Compose file format")))