(ns policy)

(def ^:private cmd-denylist
  #{#"apk" #"apt" #"pip" #"curl" #"wget"})

(defn deny-unallowed-commands
  [input]
  (let [matches (for [dockerfile input
                      command dockerfile
                      value (get command "Value")
                      re cmd-denylist
                      :let [eval-result (re-find re value)]
                      :when (and (= "run" (get command "Cmd"))
                                 (not (nil? eval-result)))]
                  value)]
    (when (not-empty matches)
      (format "unallowed commands found %s" (into [] (distinct matches))))))

(def ^:private image-denylist
  #{#"openjdk"})

(defn deny-unallowed-images
  [input]
  (let [matches (for [dockerfile input
                      command dockerfile
                      value (get command "Value")
                      re image-denylist
                      :let [eval-result (re-find re value)]
                      :when (and (= "from" (get command "Cmd"))
                                 (not (nil? eval-result)))]
                  value)]
    (when (not-empty matches)
      (format "unallowed image found %s" (into [] (distinct matches))))))