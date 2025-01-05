(ns policy)

(defn deny-disallowed-ciphers
  [input]
  (let [disallowed-ciphers #{"TLS_RSA_WITH_AES_256_GCM_SHA384"}
        ciphers (into #{} (get-in input ["entryPoints" "http" "tls" "cipherSuites"]))]
    (when (not (empty? (clojure.set/intersection disallowed-ciphers ciphers)))
      (format "Following ciphers are not allowed: %s", (into [] disallowed-ciphers)))))