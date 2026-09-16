package it.autostrada.telecamera;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.Scanner;

public class TelecameraMqttCallback implements MqttCallback {

    private final Telecamera telecamera;
    private final Scanner scanner = new Scanner(System.in);

    public TelecameraMqttCallback(Telecamera telecamera) {
        this.telecamera = telecamera;
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[MQTT] Connessione persa: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        System.out.println("[MQTT] Ricevuto su " + topic);

        String[] parts = topic.split("/");
        // pissir/{direzione}/{modalita}/{id_casello}/telecamera/richiesta-lettura-targa
        if (parts.length < 6) return;

        String direzione = parts[1];
        String modalita = parts[2];
        String idCasello = parts[3];

        System.out.print("[Telecamera] Casello " + idCasello + " (" + direzione + "/" + modalita + ") - inserisci targa rilevata: ");
        String targa = scanner.nextLine().trim().toUpperCase();

        String topicRisposta = "pissir/" + direzione + "/" + modalita + "/" + idCasello + "/telecamera/targa-letta";
        try {
            telecamera.publish(topicRisposta, "{\"targa\":\"" + targa + "\"}");
        } catch (Exception e) {
            System.err.println("Errore pubblicazione targa: " + e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
        // opzionale
    }
}

