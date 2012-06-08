(ns videochime
  (:require [clojure.core.match :refer [match]]
            [overtone.live :refer :all]
            [clj-http.client :as http]
            [cheshire.core :refer [parse-string] :as json]))

(def ^:dynamic *account-id* 1160438696001)
(def ^:dynamic *auth-token* "14fce7a31e4b08587081ee147")
(def url-base "http://data.brightcove.com/analytics-api/data/videocloud/account/")
(def counters (atom (sorted-map)))
;; # of ms during which chimes should chime
(def ^:dynamic *chime-length* 3000)
(def ^:dynamic *pitch-variation* 4)

(defn build-url
  "Construct a url for a request to the analytics api.
  Uses *account-id*. Params are optional, but if included the
  first param should be the resource to fetch
  info for (video or player). The second arg should be the id
  of the resource being fetched."
  [& [resource id]]
  (if-not (nil? resource)
    (str url-base *account-id* "/" (name resource) "/" id)
    (str url-base *account-id*)))

(defn fetch-data
  "Returns the parsed body of a request to the Analytics API.
  A specific resource & id are optional"
  [& [resource id]]
  (let [headers {"Authorization" (str "Bearer " *auth-token*)}
        url (build-url resource id)]
    (-> 
      (http/get url {:headers headers})
      :body
      (json/parse-string))))

(defn extract-current-value
  "Return a k/v pair of key & the last value of data"
  [[key data]]
  [key (-> data last second)])

(defn fetch-current-counters
  "Fetch the most recent values for the values tracked as counters.
  Gets data from the Analytics API using fetch-data and extracts 
  the most recent value for each event type in data"
  [counters]
  (->> ((fetch-data) "data")
       (map extract-current-value)
       (reduce #(apply assoc %1 %2) counters)))

(defn update-counters!
  "Updates the counters with fresh data from the Analytics API"
  []
  (swap! counters fetch-current-counters)) 

;; TODO: bump up the volume for lower pitches
(definst chime [note 60 vol 3]
  (let [src (sin-osc (* 0.75 (midicps note)))
        env (env-gen (perc 0.01 1.0 vol))]
    (* src env)))

(defmulti calculate-pitch
  "Calculate the pitch for a chime given the old and new value
  of a counter"
  (fn [v _] (first v)))

(defmethod calculate-pitch :default
  [[_ old] [_ new]]
  (let [pitch (match (compare old new)
                (_ :when neg?) 80
                (_ :when zero?) 70
                (_ :when pos?) 65)]
    (+ pitch (rand *pitch-variation*))))

(defn schedule-chime
  "Chime with pitch pitc in (+ (now) delta)"
  [delta pitch]
  (at (+ (now) delta) (chime pitch)))

(defn random-time
  "Return a random time between 0 and *chime-length* seconds"
  []
  (rand *chime-length*))

(defn watch-counters
  "A watch to trigger the chimes when the counters are updated"
  [_ _ old-val new-val]
  ; the keys of new-val and old-val shouldn't change
  (->> (map calculate-pitch old-val new-val)
       (map #(schedule-chime (random-time) %))
       doall))

(add-watch counters :chimes watch-counters)

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))
