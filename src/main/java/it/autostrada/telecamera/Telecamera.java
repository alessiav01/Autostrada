package it.autostrada.telecamera;

import it.autostrada.mqtt.SslUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Telecamera {

    private final String brokerUrl = "ssl://localhost:8883";
    private final String clientId = "it.autostrada.telecamera.Telecamera";
    private final String username = "telecamera";
    private final String password = "ciao";
    private final String pathCaCert = "C:\\mosquitto-certs\\ca.crt";

    private MqttClient client;
    private final TelecameraMqttCallback callback;

    public Telecamera() {
        this.callback = new TelecameraMqttCallback(this);
    }

    public void connect() throws Exception {
        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setSocketFactory(SslUtil.getSocketFactory(pathCaCert));
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);

        client.setCallback(callback);
        client.connect(options);

        System.out.println("[MQTT] it.autostrada.telecamera.Telecamera connessa a " + brokerUrl);
        subscribe("pissir/+/+/+/telecamera/richiesta-lettura-targa");
    }

    private void subscribe(String topicFilter) throws MqttException {
        client.subscribe(topicFilter, 1);
        System.out.println("[MQTT] Sottoscritto a: " + topicFilter);
    }

    public void publish(String topic, String payload) throws MqttException {
        client.publish(topic, payload.getBytes(), 1, false);
        System.out.println("[MQTT] Pubblicato su " + topic + " -> " + payload);
    }
}