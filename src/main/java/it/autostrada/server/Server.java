package it.autostrada.server;

import it.autostrada.mqtt.SslUtil;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


//il server è una classe che consideriamo un ente/ microservizio che hab bisogno del inidrizzo di porta ssl , usarname e password csi che si identifichi su quella paorta
public class Server {

    private final String brokerUrl = "ssl://localhost:8883";
    private final String clientId = "ServerCentrale";

    private String username = "serverCentrale"; // tutte le password sono ciao ricordalo
    private String password = "ciao";
    //
    private String pathCaCert = "C:\\mosquitto-certs\\ca.crt";
    private String serverCert = "C:\\mosquitto-certs\\server.crt";
    private String serverKey = "C:\\mosquitto-certs\\server.key";

    private MqttClient client;
    private final ServerMqttCallback callback;



    public Server() {
        this.callback = new ServerMqttCallback(this);
    }

    public void connect() throws Exception {
        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setSocketFactory(SslUtil.getSocketFactory(pathCaCert));
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        client.setCallback(callback);
        client.connect(options);

        System.out.println("[MQTT] Connesso a " + brokerUrl);
        subscribeAll();
    }

    private void subscribeAll() throws MqttException {
        subscribe("pissir/+/+/+/server/dati-transito");
        subscribe("pissir/uscita/manuale/+/server/pagamento");
    }

    private void subscribe(String topicFilter) throws MqttException {
        client.subscribe(topicFilter, 1);
        System.out.println("[MQTT] Sottoscritto a: " + topicFilter);
    }

    public void publish(String topic, String payload) throws MqttException {
        client.publish(topic, payload.getBytes(), 1, false);
        System.out.println("[MQTT] Pubblicato su " + topic + " -> " + payload);
    }

    public void disconnect() throws MqttException {
        client.disconnect();
    }
}