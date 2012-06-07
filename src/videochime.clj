(ns videochime
  (:require [clojure.core.match :refer [match]]
            [overtone.live :refer :all]
            [clj-http.client :as http]
            [cheshire.core :refer [parse-string] :as json]))

(def ^:dynamic *account-id* 1160438696001)
(def ^:dynamic *auth-token* "14fce7a31e4b08587081ee147")
(def url-base "http://data.brightcove.com/analytics-api/data/videocloud/account/")
(def counters (atom {}))

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
  Gets data from the Analytics API using fetch-data and extracts the
  most recent value for each event type in data"
  []
  (apply hash-map (flatten (map extract-current-value ((fetch-data) "data")))))

(defn update-counters!
  "Updates the counters atom with fresh data from the Analytics API"
  []
  (reset! counters (fetch-current-counters)))

;; TODO: bump up the volume for lower pitches
(definst chime [note 60 vol 1]
  (let [src (sin-osc (* 0.75 (midicps note)))
        env (env-gen (perc 0.01 1.0 vol))]
    (* src env)))

(defn calculate-pitch
  "Calculate the pitch for a chime given the old and new value
  of a counter"
  [old new]
  (match (compare old new)
    (_ :when neg?) 80
    (_ :when zero?) 70
    (_ :when pos?) 65))

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))
