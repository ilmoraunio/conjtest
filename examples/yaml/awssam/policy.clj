(ns policy)

(def ^:private denylist #{"*"})
(def ^:private sensitive-denylist #{#"password" #"Password" #"Pass" #"pass"})
(def ^:private runtime-denylist #{"python2.7" "node4.3"})

(defn ends-with?
  [coll needle]
  (letfn [(check [coll needle]
            (boolean (seq (filter (partial clojure.string/ends-with? needle) coll))))]
    (cond
      (coll? needle) (some #(check coll %) needle)
      (string? needle) (check coll needle))))

(defn deny-python2.7
  [input]
  (when (get-in input
                ["Resources"
                 "LambdaFunction"
                 "Properties"
                 "Runtime"])
    "python2.7 runtime not allowed"))

(defn deny-denylisted-runtimes
  [input]
  (let [runtime (get-in input
                        ["Resources"
                         "LambdaFunction"
                         "Properties"
                         "Runtime"])]
    (when (contains? runtime-denylist runtime)
      (format "'%s' runtime not allowed" runtime))))

(defn deny-excessive-action-permissions
  [input]
  (let [policies (get-in input ["Resources"
                                "LambdaFunction"
                                "Properties"
                                "Policies"])]
    (when (not-empty (filter (fn [{statements "Statement"}]
                               (some (fn [{actions "Action"
                                           effect "Effect"}]
                                       (and (= effect "Allow")
                                            (ends-with? denylist actions)))
                                     statements)) policies))
      "excessive Action permissions not allowed")))

(defn deny-excessive-resource-permissions
  [input]
  (let [policies (get-in input ["Resources"
                                "LambdaFunction"
                                "Properties"
                                "Policies"])]
    (when (not-empty (filter (fn [{statements "Statement"}]
                               (some (fn [{resources "Resource"
                                           effect "Effect"}]
                                       (and (= effect "Allow")
                                            (ends-with? denylist resources)))
                                     statements)) policies))
      "excessive Resource permissions not allowed")))

(defn deny-sensitive-environment-variables
  [input]
  (let [variables (get-in input ["Resources"
                                "LambdaFunction"
                                "Properties"
                                "Environment"
                                "Variables"])]
    (prn ::help (filter (fn [x] (some (fn [y] (re-find y x)) sensitive-denylist)) variables))
    (when (seq (filter (fn [x] (some (fn [y] (re-find y x)) sensitive-denylist)) variables))
      "Sensitive data not allowed in environment variables")))