(ns servant.handler
  (:import (java.nio.file Files Paths Path LinkOption)
           (java.io File))
  (:require [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [ring.util.response :as response]
            [ring.util.codec :as codec]
            [hiccup.core :refer [html]]
            [clojure.edn :as edn]
            [clojure.string :as string]))

(def opts (make-array LinkOption 0))

(def config (edn/read-string (slurp "config.edn")))

(defn authenticated? [name pass]
  (and (= name (:user config))
       (= pass (:password config))))

(defn path->link [^Path root ^Path path]
  (let [full-path (.resolve root path)]
    [:a
     {:href (str \/ (string/replace (.toString path) File/separator "/"))}
     (cond-> (.getFileName path)
             (and (not= root full-path) (Files/isDirectory full-path opts)) (str "/"))])) 

(defn element->list-entry [element]
  [:li element])

(def more (make-array String 0))

(defn handler [{:keys [uri]}]
  (let [root (Paths/get (:root config) more)
        destination (.resolve root (codec/url-decode (subs uri 1)))
        path->link (partial path->link root)]
    (cond 
      (Files/notExists destination opts) (response/not-found "404 Not Found")
      (Files/isRegularFile destination opts) (response/file-response (.toString destination) {:index-files? false})
      :else (-> (response/response 
                  (html 
                    [:h1 "Elements in " [:a {:href "/"} "/"] (map path->link (.relativize root destination))] 
                    [:a {:href "/"} "root"] 
                    [:br]
                    [:a {:href ".."} ".."]
                    [:ul 
                     (->> (Files/list destination) 
                          (.toArray)
                          (map #(.relativize root ^Path %))
                          (sort-by (juxt #(Files/isRegularFile ^Path % opts) #(.getFileName ^Path %))) 
                          (map path->link)
                          (map element->list-entry))]))
                (response/content-type "text/html")))))

(def app (-> handler
             (wrap-defaults site-defaults)
             (wrap-basic-authentication authenticated?)))
             
