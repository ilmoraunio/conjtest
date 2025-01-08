(ns policy)

(defn deny-wrong-port
  [input]
  (when (not= 9000.0 (get-in input ["play" "server" "http" "port"]))
    "Play http server port should be 9000"))

(defn deny-http-bind-address
  [input]
  (when (not= "0.0.0.0" (get-in input ["play" "server" "http" "address"]))
    "Play http server bind address should be 0.0.0.0"))