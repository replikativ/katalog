(ns katalog.core
  (:require [replikativ.crdt.cdvcs.realize :refer [head-value]]
              [replikativ.crdt.cdvcs.stage :as s]
              [hasch.core :refer [uuid]]
              [replikativ.stage :refer [create-stage! connect! subscribe-crdts!]]
              [replikativ.peer :refer [client-peer server-peer]]
              [kabel.platform :refer [create-http-kit-handler! start stop]]
              [konserve.memory :refer [new-mem-store]]
              [full.async :refer [<?? <? go-try go-loop-try]]
              [clojure.core.async :refer [chan go-loop go]]))


(def server-state
  (atom
   {:port-index 31744
    :address "127.0.0.1"}))


(defn uri [state]
  "Create new server port"
  (str "ws://"
       (:address @state)
       ":"
       (-> state deref :port-index inc)))


(def eval-fns
    {'(fn [_ new] (if (set? new) new #{new}))
     (fn [_ new] (if (set? new) new #{new}))
     'conj conj})

(defn intitalize-error-chan
  ""
  [crdt]
  (let [err-ch (chan)
        _ (go-loop [e (<? err-ch)]
            (when e
              (println "ERROR:" e)
              (recur (<? err-ch))))])
  (assoc crdt :err-ch (chan)))


(defn start-peer
  "Start server peer"
  [{:keys [err-ch store name]}]
  (assoc crdt :peer
         (server-peer
          (create-http-kit-handler! uri err-ch)
          ""
          server-store
          err-ch)))


(defn store [{:keys [user-id ] :as request} state]
  (->> request
      (merge {:store (<?? new-mem-store)})
      initilize-error-chan
      (swap! state assoc-in [user-id (uuid)])))



(comment

  (def uri "ws://127.0.0.1:31744")

  (def cdvcs-id #uuid "8e9074a1-e3b0-4c79-8765-b6537c7d0c44")

  (def server-store (<?? (new-mem-store)))

  (def err-ch (chan))

  

  (def server
    (server-peer
     (create-http-kit-handler! uri err-ch)
     ""
     server-store
     err-ch))

  (start server)

  (stop server)
)
