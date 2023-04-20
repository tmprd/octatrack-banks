(ns octatrack.application.validation
  "Application layer for parsing and validation of data at boundaries"
  (:require [octatrack.domain.banks :as banks]
            [octatrack.infrastructure.utils :as utils]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

;; Specification
(s/fdef pair-str->bank-pair
  :args (s/cat :pair-str (s/and string? (complement str/blank?)))
  :ret ::banks/bank-pair)

(s/fdef add-bank-pair ;; todo specify that returning bank-pairs must be greater than arg bank-pairs
  :args (s/cat :bank-pair ::banks/bank-pair
               :bank-pairs ::banks/total-bank-pairs)
  :ret ::banks/total-bank-pairs)

(s/fdef pair-strs->bank-pairs
  :args (s/cat :bank-pair-strs (s/coll-of string?))
  :ret ::banks/bank-pairs)

;; Implementation
(defn bank-string->bank-number ;; no spec
  "Bank number '0' as an input will be interpreted as empty/nil, for convenience"
  [bank-string]
  (let [bank-number (utils/parse-int bank-string)]
    (if (= 0 bank-number)
      nil
      bank-number)))

(defn pair-str->bank-pair 
  "Example: '1=2' => {:target 1 :source 2}"
  [pair-string]
  (let [[target source] (str/split pair-string #"=")]
    (when-not (and target source)
      (throw (Exception. "Invalid input! Enter 1 target and 1 source banks")))
    #::banks{:target (utils/validate ::banks/target (banks/target-explanation target) (bank-string->bank-number target))
             :source (utils/validate ::banks/source (banks/source-explanation source) (bank-string->bank-number source))}))

(defn add-bank-pair
  "Add bank pair to total state of source and target bank pairs"
  ;; destructure namespaced map
  [{:keys [::banks/source ::banks/target]}
   {:keys [::banks/sources ::banks/targets ::banks/bank-pairs] :as total-map}]
  ;;  TODO more obvious accumulation of immutable map
  (-> total-map
      (assoc-in [::banks/sources] (conj sources source))
      (assoc-in [::banks/targets] (conj targets target))
      (assoc-in [::banks/bank-pairs] (conj bank-pairs {::banks/source source ::banks/target target}))))

(defn pair-strs->bank-pairs
  "Parse/validate vector of `target=source` strings, return vector of {:source :target} maps"
  [bank-pairs]
  (::banks/bank-pairs
  ;; could use transducer here to decouple parsing & validation
   (reduce (fn [total-map bank-pair-str]
             (-> (pair-str->bank-pair bank-pair-str)
                 (banks/validate-bank-pair total-map)
                 (add-bank-pair total-map)))
           ;; seed value to keep track of validated banks
           #::banks{:sources [] :targets [] :bank-pairs []}
           bank-pairs)))