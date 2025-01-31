(ns cljconf.util.core)

(defn is-allowlisted?
  [allowlist x]
  (assert (coll? allowlist))
  (some? ((into #{} allowlist) x)))