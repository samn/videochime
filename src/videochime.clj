(ns videochime
  (:require [clojure.core.match :refer [match]]
            [overtone.live :refer :all]
            [clj-http.client :as http]
            [cheshire.core :refer [parse-string] :as json]))

(def ^:dynamic *account-id* 1160438696001)
(def ^:dynamic *auth-token* "14fce7a31e4b08587081ee147")
(def url-base "http://data.brightcove.com/analytics-api/data/videocloud/account/")

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

(defn get-data
  "Returns the parsed body of a request to the Analytics API.
  A specific resource & id are optional"
  [& [resource id]]
  (let [headers {"Authorization" (str "Bearer " *auth-token*)}
        url (build-url resource id)]
    (-> 
      (http/get url {:headers headers})
      :body
      (json/parse-string))))

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))
