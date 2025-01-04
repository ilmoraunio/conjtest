(ns policy)

(defn deny-http
  [input]
  (when-let [matches (seq (for [[alb-listener-name alb-listener] (get-in input ["resource" "aws_alb_listener"])
                                :when (= "HTTP" (get alb-listener "protocol"))]
                            alb-listener-name))]
    (format "ALB listeners %s are using HTTP rather than HTTPS" matches)))

(defn deny-fully-open-ingress
  [input]
  (when-let [matches (seq (for [[rule-name security-group-rule] (get-in input ["resource" "aws_security_group_rule"])
                                cidr-block (get security-group-rule "cidr_blocks")
                                :when (and (= "ingress" (get security-group-rule "type"))
                                           (= "0.0.0.0/0" cidr-block))]
                            rule-name))]
    (format "ASG rules %s define a fully open ingress" matches)))

(defn deny-unencrypted-azure-disk
  [input]
  (when-let [matches (seq (for [[disk-name disk] (get-in input ["resource" "azurerm_managed_disk"])
                                :let [encryption-settings (get disk "encryption_settings")]
                                :when (not (true? (get encryption-settings "enabled")))]
                            disk-name))]
    (format "Azure disks %s are not encrypted" matches)))

(def ^:private required-tags #{"environment" "owner"})

(defn deny-missing-tags
  [input]
  (when-let [matches (seq (for [[resource-type resources] (get input "resource")
                                [resource-name resource-definition] resources
                                :let [tags (into #{} (keys (get resource-definition "tags")))
                                      missing-tags (clojure.set/difference
                                                     (clojure.set/union tags required-tags)
                                                     tags)]
                                :when (and (clojure.string/starts-with? resource-type "aws_")
                                           (pos? (count missing-tags)))]
                            (format "AWS resource: %s named '%s' is missing required tags: %s"
                                    resource-type
                                    resource-name
                                    missing-tags)))]
    (clojure.string/join ", " (sort matches))))
