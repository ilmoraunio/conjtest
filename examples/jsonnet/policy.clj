(ns policy)

(defn deny-concat-array
  [input]
  (when-not (< (count (:concat_array input)) 3)
    "Concat array should be less than 3"))

(defn deny-obj-member
  [input]
  (when (not (true? (:obj_member input)))
    "Object member should be true"))