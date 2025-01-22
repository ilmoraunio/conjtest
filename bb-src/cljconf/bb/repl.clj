(ns cljconf.bb.repl
  (:require [sci.core :as sci]
            [babashka.nrepl.server]))

(defn -main
  []
  (babashka.nrepl.server/start-server! (sci/init {}))
  @(promise))