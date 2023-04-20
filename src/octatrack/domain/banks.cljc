(ns octatrack.domain.banks
  "Pure domain logic related to octatrack banks"
  (:require [clojure.spec.alpha :as s]))

;; Core entities & values
(def ^:const bank-count 16) ;; `const` optional metadata compiles this value instead of computing at runtime

(s/def ::bank-number (s/and integer? #(<= % bank-count) #(>= % 1)))

;; `s/conform` on a tagged union/disjunction will return which disjunct matched, example: (s/conform ::source nil) ; => [:empty nil]
(s/def ::source (s/or :integer ::bank-number
                      :empty nil?))
(s/def ::target ::bank-number)

(defn source-explanation [bank-number] (str "Source bank " bank-number " must be between 1 and " bank-count " or empty"))
(defn target-explanation [bank-number] (str "Target bank " bank-number " must be between 1 and " bank-count))

(s/def ::sources (s/coll-of ::source))
(s/def ::targets (s/coll-of ::target))

(s/def ::bank-pair (s/keys :req [::target ::source]))
(s/def ::bank-pairs (s/coll-of ::bank-pair))

(s/def ::total-bank-pairs (s/keys :req [::sources
                                        ::targets
                                        ::bank-pairs]))

;; Aggregate functions
(s/fdef validate-bank-pair
  :args (s/cat :bank-pair ::bank-pair
               :bank-pairs ::total-bank-pairs)
  :ret ::bank-pairs)

(defn validate-bank-pair
  "Validate pair of source & target banks with respect to the total specified pairs"
  ;; destructuring namespaced maps here, while preserving reference to a map with `:as`
  [{:keys [::source ::target] :as bank-pair}
   {:keys [::sources ::targets] :as total-map}]
  (when (some #{source} sources)
    (throw (Exception. (str "Can't copy source bank \"" source "\" muliple target banks!"))))
  (when (some #{target} targets)
    (throw (Exception. (str "Can't copy multiple source banks to target bank \"" target "\"!"))))
  bank-pair)