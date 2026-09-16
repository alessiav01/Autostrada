package it.autostrada.casello;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class CaselloMqttCallback implements MqttCallback {

    private final Casello casello;
    private String targaCorrente; // memorizza la targa tra un passo e l'altro del flusso
    private double pedaggioCorrente;
    public CaselloMqttCallback(Casello casello) {
        this.casello = casello;
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[MQTT] Connessione persa: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        System.out.println("[MQTT] Ricevuto su " + topic + " -> " + payload);

        String[] parts = topic.split("/");
        // pissir/{direzione}/{modalita}/{id_casello}/{attore}/{fase}
        if (parts.length < 6) return;

        String attore = parts[4];
        String fase = parts[5];

        switch (attore + "/" + fase) {
            case "telecamera/targa-letta":
                onTargaLetta(payload);
                break;
            case "server/esito":
                onEsitoServer(payload);
                break;
            case "server/pagamento-esito":
                onPagamentoEsito(payload);
                break;
            case "sbarra/stato":
                onStatoSbarra(payload);
                break;
            default:
                System.out.println("[MQTT] Evento non gestito: " + topic);
        }
    }



    @Override
    public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
        // opzionale
    }

    // ---------- Passo 1.2: it.autostrada.telecamera.Telecamera ha letto la targa ----------



    private void onTargaLetta(String payload) {
        targaCorrente = estraiCampo(payload, "targa");
        System.out.println("[Casello] Targa letta: " + targaCorrente);

        String direzione = casello.isEntrata() ? "ingresso" : "uscita";
        String modalita = casello.isAutomatico() ? "telepass" : "manuale";
        String topic = "pissir/" + direzione + "/" + modalita + "/" + casello.getId() + "/server/dati-transito";

        String json;
        if (casello.isAutomatico() && casello.getIdTrasmettitoreCorrente() != null) {
            json = "{\"targa\":\"" + targaCorrente + "\",\"idTrasmettitore\":\"" + casello.getIdTrasmettitoreCorrente() + "\"}";
        } else if (!casello.isEntrata() && !casello.isAutomatico()) {
            // Uscita manuale: invia sia la targa appena letta sia il codice biglietto inserito dal guidatore
            json = "{\"targa\":\"" + targaCorrente + "\",\"codiceBiglietto\":\"" + casello.getCodiceBigliettoCorrente() + "\"}";
        } else {
            json = "{\"targa\":\"" + targaCorrente + "\"}";
        }

        try {
            casello.publish(topic, json);
        } catch (Exception e) {
            System.err.println("Errore invio dati al server: " + e.getMessage());
        }
    }

    // ---------- Passo 1.7: il it.autostrada.server.Server ha risposto con l'esito (biglietto generato) ----------



    private void onEsitoServer(String payload) {
        System.out.println("[Casello] Esito ricevuto dal server: " + payload);

        String direzione = casello.isEntrata() ? "ingresso" : "uscita";
        String modalita = casello.isAutomatico() ? "telepass" : "manuale";

        if (casello.isEntrata()) {
            boolean autorizzato = payload.contains("\"autorizzato\":true");
            if (!autorizzato) {
                System.out.println("[Casello] Ingresso NON autorizzato, biglietto non erogato");
                return;
            }
            System.out.println("[Casello] Biglietto erogato al veicolo: " + payload);

            String topic = "pissir/" + direzione + "/" + modalita + "/" + casello.getId() + "/sbarra/comando";
            try {
                casello.publish(topic, "APRI");
            } catch (Exception e) {
                System.err.println("Errore richiesta apertura sbarra: " + e.getMessage());
            }

        } else if (modalita.equals("manuale")) {
            if (payload.contains("\"autorizzato\":false")) {
                System.out.println("[Casello] Uscita NON autorizzata: " + payload);
                return;
            }
            String pedaggioStr = estraiCampo(payload, "pedaggio");
            String targa = estraiCampo(payload, "targa");
            targaCorrente = targa;
            pedaggioCorrente = Double.parseDouble(pedaggioStr);
            casello.segnalaPedaggioRicevuto();
            System.out.println("[Casello] Targa: " + targa + " - Pedaggio da mostrare al veicolo: " + pedaggioStr + " EUR");
            System.out.println("[Casello] In attesa del pagamento...");

        } else {
            // Uscita Telepass: nessun pagamento interattivo, il server ha già registrato "da riscuotere"
            boolean autorizzato = payload.contains("\"autorizzato\":true");
            if (!autorizzato) {
                System.out.println("[Casello] Uscita NON autorizzata");
                return;
            }
            System.out.println("[Casello] Uscita Telepass autorizzata, pedaggio da riscuotere registrato.");

            String topic = "pissir/" + direzione + "/" + modalita + "/" + casello.getId() + "/sbarra/comando";
            try {
                casello.publish(topic, "APRI");
            } catch (Exception e) {
                System.err.println("Errore richiesta apertura sbarra: " + e.getMessage());
            }
        }
    }


    private void onPagamentoEsito(String payload) {
        System.out.println("[it.autostrada.casello.Casello] Esito pagamento ricevuto: " + payload);

        boolean registrato = payload.contains("\"pagamentoRegistrato\":true");
        if (!registrato) {
            System.out.println("[it.autostrada.casello.Casello] Pagamento NON registrato, sbarra resta chiusa");
            return;
        }

        String topic = "pissir/uscita/manuale/" + casello.getId() + "/sbarra/comando";
        try {
            casello.publish(topic, "APRI");
        } catch (Exception e) {
            System.err.println("Errore richiesta apertura sbarra: " + e.getMessage());
        }
    }

    // ---------- Passo 1.10: la it.autostrada.sbarra.Sbarra ha risposto con il suo stato ----------


    private void onStatoSbarra(String payloadStato) {
        System.out.println("[it.autostrada.casello.Casello] Stato sbarra: " + payloadStato);

        String direzione = casello.isEntrata() ? "ingresso" : "uscita";
        String modalita = casello.isAutomatico() ? "telepass" : "manuale";
        String topic = "pissir/" + direzione + "/" + modalita + "/" + casello.getId() + "/sbarra/comando";

        if (payloadStato.trim().equals("APERTA")) {
            System.out.println("[it.autostrada.casello.Casello] Transito in corso...");
            try {
                casello.publish(topic, "CHIUDI");
            } catch (Exception e) {
                System.err.println("Errore richiesta chiusura sbarra: " + e.getMessage());
            }
        } else if (payloadStato.trim().equals("CHIUSA")) {
            System.out.println("[it.autostrada.casello.Casello] Flusso completato.");
            targaCorrente = null;
            casello.segnalaCompletamento();
            new Thread(() -> {
                try {
                    Thread.sleep(200); // piccola pausa per lasciare concludere il callback corrente
                    casello.disconnect();
                    System.out.println("[it.autostrada.casello.Casello] Disconnesso dal broker.");
                } catch (Exception e) {
                    System.err.println("Errore durante la disconnessione: " + e.getMessage());
                }
            }).start();
        }
    }


    public String getTargaCorrente() {
        return targaCorrente;
    }

    public double getPedaggioCorrente() {
        return pedaggioCorrente;
    }

    // ---------- Helper parsing JSON minimale ----------

    private String estraiCampo(String json, String campo) {
        String chiave = "\"" + campo + "\":";
        int start = json.indexOf(chiave);
        if (start == -1) return null;
        start += chiave.length();
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return json.substring(start + 1, end);
        } else {
            int end = json.indexOf(',', start);
            if (end == -1) end = json.indexOf('}', start);
            return json.substring(start, end).trim();
        }
    }
}
