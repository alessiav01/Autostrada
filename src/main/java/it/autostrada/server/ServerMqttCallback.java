package it.autostrada.server;

import it.autostrada.db.PedaggioDAO;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class ServerMqttCallback implements MqttCallback {

    private final Server server; // per poter pubblicare la risposta
    private final PedaggioDAO dao = new PedaggioDAO();

    public ServerMqttCallback(Server server) {
        this.server = server;
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

        String direzione = parts[1];
        String modalita = parts[2];
        String idCasello = parts[3];
        String fase = parts[5];

        switch (fase) {
            case "dati-transito":
                handleDatiTransito(direzione, modalita, idCasello, payload);
                break;
            case "pagamento":
                handlePagamento(idCasello, payload);
                break;
            default:
                System.out.println("[MQTT] Fase non gestita: " + fase);
        }
    }

    @Override
    public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
        // opzionale
    }





    private void handleDatiTransito(String direzione, String modalita, String idCasello, String payload) {
        System.out.println("Elaboro transito -> direzione=" + direzione + " modalita=" + modalita + " casello=" + idCasello);

        String idTrasmettitore = estraiCampo(payload, "idTrasmettitore"); // null se manuale, va bene
        String esitoTopic = "pissir/" + direzione + "/" + modalita + "/" + idCasello + "/server/esito";

        try {
            if (direzione.equals("ingresso")) {
                String targa = estraiCampo(payload, "targa");
                String codice = dao.registraIngresso(targa, idCasello, modalita, idTrasmettitore);
                server.publish(esitoTopic, "{\"autorizzato\":true,\"codiceBiglietto\":\"" + codice + "\"}");

            } else {
                int idBiglietto;

                if (modalita.equals("manuale")) {
                    String codiceBiglietto = estraiCampo(payload, "codiceBiglietto");
                    String targaLetta = estraiCampo(payload, "targa");

                    idBiglietto = dao.trovaBigliettoPerCodice(codiceBiglietto);
                    if (idBiglietto == -1) {
                        server.publish(esitoTopic, "{\"autorizzato\":false,\"motivo\":\"biglietto non trovato\"}");
                        return;
                    }

                    PedaggioDAO.BigliettoInfo infoVerifica = dao.trovaInfoBiglietto(idBiglietto);
                    if (!infoVerifica.targa().equalsIgnoreCase(targaLetta)) {
                        System.out.println("[SICUREZZA] Targa non corrispondente! Attesa=" + infoVerifica.targa() + " Letta=" + targaLetta);
                        server.publish(esitoTopic, "{\"autorizzato\":false,\"motivo\":\"targa non corrispondente al biglietto\"}");
                        return;
                    }
                } else {
                    String targa = estraiCampo(payload, "targa");
                    idBiglietto = dao.trovaBigliettoApertoPerTarga(targa);
                }

                if (idBiglietto == -1) {
                    server.publish(esitoTopic, "{\"autorizzato\":false,\"motivo\":\"biglietto non trovato\"}");
                    return;
                }

                PedaggioDAO.BigliettoInfo info = dao.trovaInfoBiglietto(idBiglietto);
                double pedaggio = dao.calcolaPedaggio(info.idCaselloIngresso(), Integer.parseInt(idCasello));

                double velocitaMedia = dao.calcolaVelocitaMediaKmh(info.idCaselloIngresso(), Integer.parseInt(idCasello), info.timestampIngresso());
                //System.out.println("[DEBUG] Velocità media calcolata: " + velocitaMedia + " km/h");
                if (velocitaMedia > 130) {
                    dao.registraMulta(idBiglietto, info.targa(), info.idCaselloIngresso(), Integer.parseInt(idCasello), velocitaMedia);
                    System.out.println("[MULTA] Targa " + info.targa() + " - velocità media: " + Math.round(velocitaMedia) + " km/h (limite 130 km/h)");
                }

                String rispostaBase = "\"targa\":\"" + info.targa() + "\","
                        + "\"timestampIngresso\":\"" + info.timestampIngresso() + "\","
                        + "\"caselloIngresso\":" + info.idCaselloIngresso() + ","
                        + "\"pedaggio\":" + pedaggio;

                if (modalita.equals("telepass")) {
                    dao.registraTransitoRiscuotere(idBiglietto, idCasello, modalita, pedaggio);
                    server.publish(esitoTopic, "{\"autorizzato\":true," + rispostaBase + "}");
                } else {
                    server.publish(esitoTopic, "{" + rispostaBase + "}");
                }
            }
        } catch (Exception e) {
            System.err.println("Errore gestione dati-transito: " + e.getMessage());
        }
    }
   //VALUTARE UUN  EVENTUALE MODIFICA DEL METODO DOVE PASSO ANCHE IL BIGLIETTO
    private void handlePagamento(String idCasello, String payload) {
        System.out.println("Elaboro pagamento -> casello=" + idCasello);

        String targa = estraiCampo(payload, "targa");
        double importo = Double.parseDouble(estraiCampo(payload, "importo"));

        String esitoTopic = "pissir/uscita/manuale/" + idCasello + "/server/pagamento-esito";

        try {
            int idBiglietto = dao.trovaBigliettoApertoPerTarga(targa);
            if (idBiglietto == -1) {
                server.publish(esitoTopic, "{\"pagamentoRegistrato\":false,\"motivo\":\"biglietto non trovato\"}");
                return;
            }

            int idTransito = dao.registraTransitoPagato(idBiglietto, idCasello, importo);
            dao.registraPagamento(idTransito, importo);

            server.publish(esitoTopic, "{\"pagamentoRegistrato\":true}");
        } catch (Exception e) {
            System.err.println("Errore pubblicazione esito pagamento: " + e.getMessage());
        }
    }

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