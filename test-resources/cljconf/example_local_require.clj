(ns cljconf.example-local-require
  (:require [cljconf.util.core :as util]))

(def ^:private allowlist ["hello-kubernetes"])

(defn allow-allowlisted-selector-only
  [input]
  (and (= "Service" (:kind input))
       (util/is-allowlisted? allowlist (get-in input [:spec :selector :app]))))