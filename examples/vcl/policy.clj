(ns policy)

(defn deny-purge-list
  [input]
  (let [allowlist #{"127.0.0.1" "localhost"}
        purges (get-in input ["acl" "purge"])
        violations (for [purge purges
                         :when (not (allowlist purge))]
                     purge)]
    (when (not-empty violations)
      (format "acl purge should be one of %s, was %s instead" (into [] allowlist) purges))))

(defn deny-incorrect-port
  [input]
  (when (not= "8080" (get-in input ["backend" "app" "port"]))
    "default backend port should be 8080"))