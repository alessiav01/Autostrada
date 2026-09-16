package it.autostrada.sbarra;

import it.autostrada.mqtt.SslUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Sbarra {

    private final String brokerUrl = "ssl://localhost:8883";
    private final String clientId = "it.autostrada.sbarra.Sbarra";
    private final String username = "sbarra";
    private final String password = "ciao";
    private final String pathCaCert = "C:\\mosquitto-certs\\ca.crt";

    private MqttClient client;
    private final SbarraMqttCallback callback;

    public Sbarra() {
        this.callback = new SbarraMqttCallback(this);
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

        System.out.println("[MQTT] it.autostrada.sbarra.Sbarra connessa a " + brokerUrl);
        subscribe("pissir/+/+/+/sbarra/comando");
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