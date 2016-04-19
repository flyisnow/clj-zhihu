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

(ns ^{:doc "The clj-zhihu core functionality."
      :author "Xiangru Lian"}
    clj-zhihu.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [net.cgrand.enlive-html :as html]
            [clj-zhihu.auth :as auth]
            [taoensso.truss :as truss]))

(defn question
  "Given a question url, return properties."
  [url]
  (-> (client/get url))
)
