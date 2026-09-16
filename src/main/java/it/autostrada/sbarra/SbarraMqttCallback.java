package it.autostrada.sbarra;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SbarraMqttCallback implements MqttCallback {

    private final Sbarra sbarra;

    public SbarraMqttCallback(Sbarra sbarra) {
        this.sbarra = sbarra;
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[MQTT] Connessione persa: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String comando = new String(message.getPayload()).trim();
        System.out.println("[MQTT] Ricevuto su " + topic + " -> " + comando);

        String[] parts = topic.split("/");
        // pissir/{direzione}/{modalita}/{id_casello}/sbarra/comando
        if (parts.length < 6) return;

        String direzione = parts[1];
        String modalita = parts[2];
        String idCasello = parts[3];
        String topicStato = "pissir/" + direzione + "/" + modalita + "/" + idCasello + "/sbarra/stato";

        // Eseguito su thread separato per non bloccare il client MQTT durante l'attesa
        new Thread(() -> {
            try {
                Thread.sleep(1500); // simula il tempo fisico di apertura/chiusura
                String stato = comando.equalsIgnoreCase("APRI") ? "APERTA" : "CHIUSA";
                sbarra.publish(topicStato, stato);
            } catch (Exception e) {
                System.err.println("Errore pubblicazione stato sbarra: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
        // opzionale
    }
}