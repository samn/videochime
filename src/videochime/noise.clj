(ns videochime.noise
  (:require [overtone.live :refer :all]))

;; # of ms during which chimes should chime
(def ^:dynamic *chime-length* 3000)
(def ^:dynamic *pitch-variation* 4)

;; TODO: bump up the volume for lower pitches
(definst chime [note 60 vol 3]
  (let [src (sin-osc (midicps note))
        env (env-gen (perc 0.01 1.0 vol))]
    (* src env)))

(defn schedule-chime
  "Chime with pitch pitc in (+ (now) delta)"
  [delta inst pitch]
  (at (+ (now) delta) (inst pitch)))

(defn random-time
  "Return a random time between 0 and *chime-length* milliseconds"
  []
  (rand *chime-length*))
