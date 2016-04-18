;; clj-zhihu
;; Copyright (C) 2016  Xiangru Lian <xlian@gmx.com>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns clj-zhihu.utils
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]))

(defn get-xsrf
  "get the xsrf value given cookiestore"
  []
  (-> (clj-http.cookies/get-cookies @(resolve 'clj-http.core/*cookie-store*))
      (get "_xsrf")
      :value))

(defn download
  "Download from url to path."
  [url path]
  (with-open [f (io/output-stream (io/file path))]
    (->> (client/get url {:as :byte-array})
         :body
         (.write f))))
