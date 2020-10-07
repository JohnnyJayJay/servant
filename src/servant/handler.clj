(ns servant.handler
  (:import (java.nio.file Files Paths Path))
  (:require [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [ring.util.response :refer [file-response not-found]]
            [hiccup.core :refer [html]]
            [clojure.java.io :refer [file]]
            [clojure.string :refer [last-index-of]]
            [clojure.edn :as edn]))

(def config (edn/read-string "config.edn"))

(defn authenticated? [name pass]
  (and (= name (:user config))
       (= pass (:password config))))

(defn path->link [^Path path]
  [:a 
   {:href (str \/ (.relativize root path))} 
   (cond-> (.getFileName path)
           (Files/isDirectory path) (str "/"))])

(defn element->list-entry [element]
  [:li element])

(defn handler [{:keys [uri]}]
  (let [root (Paths/get (:root config))
        destination (.resolve root uri)]
    (cond 
      (Files/notExists destination)) (not-found "404 Not Found")
      (Files/isRegularFile destination) (file-response (str destination) {:index-files? false})
      :else (html 
              [:h1 "Elements in " [:a {:href "/"} "/"] (map path->link destination)] 
              [:a {:href "/"} "root"]
              [:a {:href ".."} ".."]
              [:ul 
               (->> (Files/list destination) 
                    (.toArray)
                    (sort-by (juxt #(Files/isRegularFile ^Path %) #(.getFileName ^Path %))) 
                    (map path->link)
                    (map element->list-entry))])))

(def app (-> handler
             (wrap-content-type) 
             (wrap-not-modified) 
             (wrap-basic-authentication authenticated?)))
