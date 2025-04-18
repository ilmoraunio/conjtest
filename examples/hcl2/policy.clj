(ns policy)

(defn deny-http
  [input]
  (for [[alb-listener-name alb-listener] (get-in input ["resource" "aws_alb_listener"])
        :when (= "HTTP" (get alb-listener "protocol"))]
    (format "ALB listener '%s' is using HTTP rather than HTTPS" alb-listener-name)))

(defn deny-fully-open-ingress
  [input]
  (for [[rule-name security-group-rule] (get-in input ["resource" "aws_security_group_rule"])
        cidr-block (get security-group-rule "cidr_blocks")
        :when (and (= "ingress" (get security-group-rule "type"))
                   (= "0.0.0.0/0" cidr-block))]
    (format "ASG rule '%s' defines a fully open ingress" rule-name)))

(defn deny-unencrypted-azure-disk
  [input]
  (for [[disk-name disk] (get-in input ["resource" "azurerm_managed_disk"])
        :let [encryption-settings (get disk "encryption_settings")]
        :when (not (true? (get encryption-settings "enabled")))]
    (format "Azure disk '%s' is not encrypted" disk-name)))

(def ^:private required-tags #{"environment" "owner"})

(defn deny-missing-tags
  [input]
  (sort (for [[resource-type resources] (get input "resource")
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
                  missing-tags))))
