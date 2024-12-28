(ns ilmoraunio.cljconf.example-local-require
  (:require [ilmoraunio.cljconf.util.core :as util]))

(def ^:private allowlist ["hello-kubernetes"])

(defn allow-allowlisted-selector-only
  [input]
  (and (= "Service" (get input "kind"))
       (util/is-allowlisted? allowlist (get-in input ["spec" "selector" "app"]))))