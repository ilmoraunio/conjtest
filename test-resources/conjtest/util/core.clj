(ns conjtest.util.core)

(defn is-allowlisted?
  [allowlist x]
  (assert (coll? allowlist))
  (some? ((into #{} allowlist) x)))
