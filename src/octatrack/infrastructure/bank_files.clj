(ns octatrack.infrastructure.bank-files
  "Infrastructure layer for persisting bank changes as files"
  (:require [octatrack.domain.banks :as banks]
            [octatrack.infrastructure.logger :as log]
            [octatrack.infrastructure.utils :as utils]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; Specification
(s/fdef copy-bank-file-pair
  :args (s/cat :source-folder string?
               :target-folder string?
               :bank-pair ::banks/bank-pair))

;; Implementation
(defn format-bank-name [bank-number] (when bank-number (str "bank" (utils/pad-two-digits bank-number))))

  ;; TODO octatrack doesn't like but accepts empty bank files. Ideally we'd want an empty bank that's properly formatted
(defn- create-empty-file [folder filename] nil)

(defn- transfered-status-message [source-bank-name target-bank-name]
  (if source-bank-name
    (str "Copied " source-bank-name " to " target-bank-name)
    (str "Empty bank set as " target-bank-name)))

(defn- copy-bank-file
  "Copy source bank file to target bank file, or create empty target file if no source provided"
  [source-folder source-bank-name target-folder target-bank-name file-extension]
  (let [source-filename (str source-bank-name file-extension)
        target-filename  (str target-bank-name file-extension)]
    (if source-bank-name
      (io/copy (io/file source-folder source-filename) (io/file target-folder target-filename))
      (create-empty-file target-folder target-filename))))

(defn- copy-bank-file-pair 
  "Copy bank files, given a source and target"
  [source-folder target-folder bank-pair]
  (log/debug bank-pair)
  (let [source-bank-name (format-bank-name (::banks/source bank-pair))
        target-bank-name (format-bank-name (::banks/target bank-pair))]
    (try
      (copy-bank-file source-folder source-bank-name target-folder target-bank-name ".strd")
      (copy-bank-file source-folder source-bank-name target-folder target-bank-name ".work")
      (log/printlog (transfered-status-message source-bank-name target-bank-name))
      (catch Exception e
        ;; Allow other files to be copied
        (log/printlog (str "(!) Error copying \"" source-bank-name "\" to \"" target-bank-name "\""))
        (log/printlog (str "(!) " (ex-message e)))))))

(defn copy-bank-pairs [bank-pairs project-folder-path target-folder-path]
  ;; `doall` realizes side-effects from lazy sequence, in this case, moving files around
  (doall (map #(copy-bank-file-pair project-folder-path target-folder-path %) bank-pairs)))


;; -------------------------------------------- How to find missing banks using a transducer
(defn bank-filename->number [strd-filename]
  (-> strd-filename
      (str/replace  ".strd" "")
      (str/replace  "bank" "")
      (utils/strip-leading-zeroes)))

(def ^:private bank-file-number-transducer
  "Mapping and filtering of file objects to bank numbers.
   Type `(bank numbers, file -> bank numbers) -> (bank numbers, file -> bank numbers)`.
   Excludes .work (cached bank) files for simplicity."
  ;; This is a composite of transformations/step functions used in a transduce. These are decoupled from a reducing function.
  ;; (In Clojure, `map` and `filter` each return a transducer themselves when no params are provided.)
  ;; Returns a higher-order function, which takes a reducing function, and returns a transformation of that reducing function.
  (comp
   (map #(^java.io.File .getName %)) ; java method GetName() wrapped in lambda to use as first-class fn, type hinted to avoid reflection
   (filter #(and (str/starts-with? % "bank")
                 (str/ends-with? % ".strd")))
   (map bank-filename->number)))

(defn- get-missing-bank-numbers-transduce
  "Given some project files, find which bank numbers are missing based on the bank files"
  ;; Transduce is used for composing a reduce while decoupling the logic inside.
  ;; Whereas reduce could map->filter->map in one iteration, it would couple the logic together with the reducing function.
  ;; Instead, we specify the reducing function (2nd arg) independently of the composite step function (1st arg).
  ;; file -> map filename -> filter if bank filename -> map bank number -> (reduce) filter if bank number not in aggregate
  [project-files]
  (transduce
   bank-file-number-transducer ;; composite step functions here
   (fn [expected-numbers & bank-number] ; wrapped reducing function here
     (remove #(= % (first bank-number)) expected-numbers))
   (range 1 (inc banks/bank-count))
   project-files))

(defn show-missing-banks
  "Print out current missing expected target bank numbers"
  [bank-folder-path]
  (let [missing-banks (->> (utils/get-directory-contents-tree bank-folder-path)
                           (get-missing-bank-numbers-transduce))
        missing-bank-count (count missing-banks)]
    (log/printlog
     (if (> missing-bank-count 0)
       (str "(!!!) " missing-bank-count " missing banks: [" (str/join " " missing-banks) "]")
       "No missing banks"))))



;; Example alternative implementation
(defn- ^:example get-missing-bank-numbers-reduce
  "Unused, example implementation of `get-missing-bank-numbers` using `reduce` instead of `transduce`"
  ;; Here, the step functions and reducing function are coupled.
  [project-files]
  (reduce
   (fn [expected-numbers existing-file]
     (let [filename (.getName existing-file)] ;; step function
       (if (and (str/starts-with? filename "bank") ;; step function
                (str/ends-with? filename ".strd"))
         (remove #(= % (bank-filename->number filename)) expected-numbers) ; reducing function here
         expected-numbers)))
   (range 1 (inc banks/bank-count))
   project-files))