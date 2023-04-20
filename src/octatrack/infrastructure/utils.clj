(ns octatrack.infrastructure.utils
  "Infrastructure layer, providing common functionality that isn't application or domain specific"
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

;; Generic stuff
(defn parse-int [str] (edn/read-string str))
(defn strip-leading-zeroes [str] (int (bigint str)))

(defn pad-two-digits [number]
  (format "%02d"  (if (int? number)
                    number
                    (parse-int number))))

(defn filter-missing [actual expected]
  ;; filter a filter
  (filter (fn [expected-element]
            (not-any? #(= expected-element %) actual))
          expected))

;; Validation
(defn validate [spec fail-message input]
  (when-not (s/valid? spec input)
    ;; ex-info contains the full exception plus structured spec data
    (throw (ex-info (str "Invalid input: " fail-message) (s/explain-data spec input))))
  input)

;; Files
(defn create-directory [path] (.mkdir (io/file path)))

(defn get-directory-contents-tree [path]
  ;;  `file-seq` lists the directory itself, so we skip that
  (->> (io/file path)
       (file-seq)
       (reverse) ;; show files before folders, useful for deleting
       (butlast)))

(defn clear-directory [path]
  (doall (->> (get-directory-contents-tree path)
              (map io/delete-file))))