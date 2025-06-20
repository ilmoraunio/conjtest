(ns policy)

(defn deny-http
  [input]
  (for [[alb-listener-name alb-listeners] (get-in input [:resource :aws_alb_listener])
        alb-listener alb-listeners
        :when (= "HTTP" (:protocol alb-listener))]
    (format "ALB listener '%s' is using HTTP rather than HTTPS" alb-listener-name)))

(defn deny-fully-open-ingress
  [input]
  (for [[rule-name security-group-rules] (get-in input [:resource :aws_security_group_rule])
        security-group-rule security-group-rules
        cidr-block (:cidr_blocks security-group-rule)
        :when (and (= "ingress" (:type security-group-rule))
                   (= "0.0.0.0/0" cidr-block))]
    (format "ASG rule '%s' defines a fully open ingress" rule-name)))

(defn deny-unencrypted-azure-disk
  [input]
  (for [[disk-name disks] (get-in input [:resource :azurerm_managed_disk])
        disk disks
        encryption-settings (:encryption_settings disk)
        :when (not (true? (:enabled encryption-settings)))]
    (format "Azure disk '%s' is not encrypted" disk-name)))

(def ^:private required-tags #{:environment :owner})

(defn deny-missing-tags
  [input]
  (sort (for [[resource-type resources] (:resource input)
              [resource-name resource-definitions] resources
              resource-definition resource-definitions
              :let [tags (into #{} (keys (:tags resource-definition)))
                    missing-tags (clojure.set/difference
                                   (clojure.set/union tags required-tags)
                                   tags)]
              :when (and (clojure.string/starts-with? (name resource-type) "aws_")
                         (pos? (count missing-tags)))]
          (format "AWS resource: %s named '%s' is missing required tags: %s"
                  resource-type
                  resource-name
                  missing-tags))))

(def allow-declarative-example
  [:map
   [:resource
    [:map
     [:aws_alb_listener [:map-of :keyword
                         [:vector [:map [:protocol [:= {:error/message "AWS ALB listener should use 'HTTPS' protocol"} "HTTPS"]]]]]]
     [:aws_security_group_rule [:map-of :keyword
                                [:vector [:map [:cidr_blocks [:not= {:error/message "CIDR block cannot be fully open"} ["0.0.0.0/0"]]]]]]]
     [:azurerm_managed_disk [:map-of :keyword
                             [:vector [:map
                                       [:encryption_settings [:vector [:map [:enabled [:= {:error/message "Disk must be encrypted"} true]]]]]]]]]]]])