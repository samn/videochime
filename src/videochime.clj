(ns videochime
  (:gen-class)
  (:require [videochime.noise :as noise]
            [clojure.core.match :refer [match]]
            [clj-http.client :as http]
            [cheshire.core :refer [parse-string] :as json]))

(def ^:dynamic *account-id* 1160438696001)
(def ^:dynamic *auth-token* "14fce7a31e4b08587081ee147")
(def url-base "http://data.brightcove.com/analytics-api/data/videocloud/account/")
(def counters (atom (sorted-map)))

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
  [(keyword key) (-> data last second)])

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

(defmulti chime
  "Calculate the pitch for a chime given the old and new value
  of a counter"
  first)

(defmethod chime :default
  [[event old new]]
  (let [raw-pitch (match (compare old new)
                    (_ :when neg?) 80
                    (_ :when zero?) 70
                    (_ :when pos?) 65)
        pitch (+ raw-pitch (rand noise/*pitch-variation*))]
    (noise/schedule-chime (noise/random-time) noise/chime pitch)))

(defn merge-hash-tuples
  "Takes tuples like [k v] (like mapping over a hash)
  and returns a list composed of the key and all values.
  Assumes the key (first element) is the same for all tuples."
  [& tuples]
  (cons (ffirst tuples) (map second tuples)))

(defn watch-counters
  "A watch to trigger the chimes when the counters are updated"
  [_ _ old-val new-val]
  ; the keys of new-val and old-val shouldn't change
  (->> (map merge-hash-tuples new-val old-val)
       (map chime)
       doall))

(add-watch counters :chimes watch-counters)

(defn run! [min-delay max-delay]
  (update-counters!)
  (Thread/sleep (+ min-delay (rand max-delay)))
  (recur min-delay max-delay))
  
(defn -main
  [min-delay max-delay]
  (let [min (Long/parseLong min-delay)
        max (Long/parseLong max-delay)]
    (update-counters!)
    (run! min max)))
