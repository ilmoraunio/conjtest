(ns policy)

(def ^:private cmd-denylist
  #{#"apk" #"apt" #"pip" #"curl" #"wget"})

(defn deny-unallowed-commands
  [input]
  (distinct (for [dockerfile input
                  {:keys [Cmd] :as command} dockerfile
                  value (:Value command)
                  re cmd-denylist
                  :let [eval-result (re-find re value)]
                  :when (and (= "run" Cmd)
                             (not (nil? eval-result)))]
              (format "unallowed command found '%s'" value))))

(def ^:private image-denylist
  #{#"openjdk"})

(defn deny-unallowed-images
  [input]
  (for [dockerfile input
        {:keys [Cmd] :as command} dockerfile
        value (:Value command)
        re image-denylist
        :let [eval-result (re-find re value)]
        :when (and (= "from" Cmd)
                   (not (nil? eval-result)))]
    (format "unallowed image found '%s'" value)))