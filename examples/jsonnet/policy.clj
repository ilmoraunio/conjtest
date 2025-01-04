(ns policy)

(defn deny-concat-array
  [input]
  (when-not (< (count (get-in input ["concat_array"])) 3)
    "Concat array should be less than 3"))

(defn deny-obj-member
  [input]
  (when (not (true? (get-in input ["obj_member"])))
    "Object member should be true"))