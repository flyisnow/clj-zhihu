(ns clj-zhihu.utils
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [seesaw.core :as ss])
  (:import [org.apache.commons.io IOUtils]))

(def regex-map
  {:hashid #"hash_id&quot;: &quot;(.*)&quot;},"
   :xsrf #"\<input\stype=\"hidden\"\sname=\"_xsrf\"\svalue=\"(\S+)\""
   :homepageid #"<h2.*?\\\/people\\\/(.*?)\\\" class=\\\"zg-link\\\""})

(defn get-xsrf
  "get the xsrf value given an html page"
  [html-source]
  (find-by-regex html-source
                 (:xsrf regex-map)
                 not-empty))

(defn get-hashid
  "get the hash id given an html page"
  [html-source]
  (find-by-regex html-source
                 (:hashid regex-map)
                 not-empty))

(defn find-by-regex
  "return the last group element given a regex and source"
  [source reg validator]
  (if-let [e (last (re-find reg source))]
    (if (validator e)
      e
      (throw (Exception. "Result not valid!")))
    (throw (Exception. "Result not found!"))))

(defn get-captcha
  "return the captcha"
  []
  (let [captcha-url   "http://www.zhihu.com/captcha.gif"
        download-path "captcha.gif"
        _             (with-open [f (io/output-stream (io/file "resources" download-path))]
                        (.write f (:body (client/get captcha-url {:as :byte-array}))))
        image-frame   (show-image download-path)]
    (try (prn "please input the captcha")
         (read-line)
         (finally (ss/dispose! image-frame)))))

(defn show-image
  "show an image in a JFrame"
  [image-path]
  (with-open [captcha-stream (io/input-stream (io/resource image-path))]
    (let [captcha-bytearray (IOUtils/toByteArray captcha-stream)]
      (->
       (ss/frame :title "Captcha"
                 :content (ss/label :icon (seesaw.icon/icon
                                           (javax.swing.ImageIcon. captcha-bytearray))))
       ss/pack!
       ss/show!))))
