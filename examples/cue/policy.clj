(ns policy)

(defn deny-apps-not-using-v1
  [input]
  (let [api-version (get input "apiVersion")
        name (get-in input ["metadata" "name"])]
    (when-not (= api-version "apps/v1")
      (format "apiVersion must be apps/v1 in '%s'" name))))

(defn deny-replica-count-less-than-2
  [input]
  (let [replicas (get-in input ["spec" "replicas"])]
    (when (< replicas 2)
      (format "Replica count must be greater than 2, you have %f" replicas))))

(defn deny-ports-outside-of-8080
  [input]
  (let [containers (get-in input ["spec" "template" "spec" "containers"])
        ports (->> containers
                   (mapcat #(get % "ports"))
                   (mapv #(get % "containerPort")))]
    (when-not (every? (partial = 8080.0) ports)
      (format "The image port should be 8080 in deployment.cue. you have: %s" ports))))

(defn deny-no-images-tagged
  [input]
  (let [containers (get-in input ["spec" "template" "spec" "containers"])]
    (when-not (seq (filter #(clojure.string/ends-with? (get % "image") ":latest") containers))
      "No images tagged latest")))