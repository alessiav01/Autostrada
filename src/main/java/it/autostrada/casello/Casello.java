package it.autostrada.casello;

import it.autostrada.mqtt.SslUtil;
import it.autostrada.mqtt.TopicManager;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Casello {

    // ---------- Configurazione entità ----------
    private final int id;
    private final String nomeCasello;
    private String idTrasmettitoreCorrente;
    private String codiceBigliettoCorrente;
    private final boolean automatico; // true = telepass, false = manuale
    private final boolean entrata;    // true = ingresso, false = uscita
    private final java.util.concurrent.CountDownLatch flowCompletato = new java.util.concurrent.CountDownLatch(1);
    private final java.util.concurrent.CountDownLatch pedaggioRicevuto = new java.util.concurrent.CountDownLatch(1);



    // ---------- Configurazione MQTT ----------
    private final String brokerUrl = "ssl://localhost:8883";
    private final String username = "casello" ;
    private final String password = "ciao";
    private final String pathCaCert = "C:\\mosquitto-certs\\ca.crt";

    private MqttClient client;
    private final CaselloMqttCallback callback;

    public Casello(int id, String nomeCasello, boolean automatico, boolean entrata) {
        this.id = id;
        this.nomeCasello = nomeCasello;
        this.automatico = automatico;
        this.entrata = entrata;
        this.callback = new CaselloMqttCallback(this);
    }

    public void connect() throws Exception {
        String clientId = "it.autostrada.casello.Casello" + id + "_" + (entrata ? "Ingresso" : "Uscita") + "_"
                + (automatico ? "Telepass" : "Manuale") + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setSocketFactory(SslUtil.getSocketFactory(pathCaCert));
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);

        client.setCallback(callback);
        client.connect(options);

        System.out.println("[MQTT] " + nomeCasello + " connesso a " + brokerUrl);
        subscribeAll();
    }


    private void subscribeAll() throws MqttException {
        String direzione = entrata ? "ingresso" : "uscita";
        String modalita = automatico ? "telepass" : "manuale";
        TopicManager.Direzione dEnum = TopicManager.Direzione.valueOf(direzione.toUpperCase());
        TopicManager.Modalita mEnum = TopicManager.Modalita.valueOf(modalita.toUpperCase());

        subscribe(TopicManager.telecameraTargaLetta(dEnum, mEnum, id));
        subscribe(TopicManager.serverEsito(dEnum, mEnum, id));
        subscribe(TopicManager.sbarraStato(dEnum, mEnum, id));

        if (!entrata && !automatico) {
            subscribe(TopicManager.serverPagamentoEsito(id));
        }
    }

    private void subscribe(String topicFilter) throws MqttException {
        client.subscribe(topicFilter, 1);
        System.out.println("[MQTT] Sottoscritto a: " + topicFilter);
    }

    public void publish(String topic, String payload) throws MqttException {
        client.publish(topic, payload.getBytes(), 1, false);
        System.out.println("[MQTT] Pubblicato su " + topic + " -> " + payload);
    }



    // ---------- 1.1 RichiestaLetturaTarga (Ingresso Manuale) ----------

    public void gestisciIngressoManuale() {
        System.out.println("[" + nomeCasello + "] Avvio flusso Ingresso Manuale");
        try {
            String topic = TopicManager.telecameraRichiestaLetturaTarga(TopicManager.Direzione.INGRESSO, TopicManager.Modalita.MANUALE, id);
            publish(topic, "{}");
            // il resto del flusso (invio targa al server, biglietto erogato, apertura sbarra)
            // prosegue in modo reattivo dentro it.autostrada.casello.CaselloMqttCallback, man mano che arrivano le risposte
        } catch (Exception e) {
            System.err.println("Errore avvio ingresso manuale: " + e.getMessage());
        }
    }

    public void gestisciUscitaManuale(String codiceBiglietto) {
        this.codiceBigliettoCorrente = codiceBiglietto;
        System.out.println("[" + nomeCasello + "] Avvio flusso Uscita Manuale - biglietto inserito: " + codiceBiglietto);
        try {
            String topic = TopicManager.telecameraRichiestaLetturaTarga(TopicManager.Direzione.USCITA, TopicManager.Modalita.MANUALE, id);
            publish(topic, "{}");
        } catch (Exception e) {
            System.err.println("Errore avvio uscita manuale: " + e.getMessage());
        }
    }

    // ---------- 1.1 RichiestaLetturaTarga (Ingresso Manuale) ----------


    public void gestisciIngressoAutomatico(String idTrasmettitore) {
        this.idTrasmettitoreCorrente = idTrasmettitore;
        System.out.println("[" + nomeCasello + "] Rilevo Telepass, ID trasmettitore: " + idTrasmettitore);
        System.out.println("[" + nomeCasello + "] Avvio flusso Ingresso Automatico Telepass");
        try {
            String topic = TopicManager.telecameraRichiestaLetturaTarga(TopicManager.Direzione.INGRESSO, TopicManager.Modalita.TELEPASS, id);
            publish(topic, "{}");
        } catch (Exception e) {
            System.err.println("Errore avvio ingresso automatico: " + e.getMessage());
        }
    }

    public void eseguiPagamento(String targa, double importo) {
        System.out.println("[" + nomeCasello + "] Invio pagamento -> targa=" + targa + " importo=" + importo);
        String topic = "pissir/uscita/manuale/" + id + "/server/pagamento";
        try {
            publish(topic, "{\"targa\":\"" + targa + "\",\"importo\":" + importo + "}");
        } catch (Exception e) {
            System.err.println("Errore invio pagamento: " + e.getMessage());
        }
    }

    public void gestisciUscitaAutomatico(String idTrasmettitore) {
        this.idTrasmettitoreCorrente = idTrasmettitore;
        System.out.println("[" + nomeCasello + "] Rilevo Telepass, ID trasmettitore: " + idTrasmettitore);
        System.out.println("[" + nomeCasello + "] Avvio flusso Uscita Automatica Telepass");
        try {
            String topic = TopicManager.telecameraRichiestaLetturaTarga(TopicManager.Direzione.USCITA, TopicManager.Modalita.TELEPASS, id);
            publish(topic, "{}");
        } catch (Exception e) {
            System.err.println("Errore avvio uscita automatica: " + e.getMessage());
        }
    }




    public void attendiCompletamento() throws InterruptedException {
        flowCompletato.await();
    }

    public void segnalaCompletamento() {
        flowCompletato.countDown();
    }


    public void attendiPedaggio() throws InterruptedException {
        pedaggioRicevuto.await();
    }

    public void segnalaPedaggioRicevuto() {
        pedaggioRicevuto.countDown();
    }


    // ---------- Getter usati dal callback ----------
    public String getCodiceBigliettoCorrente() {
        return codiceBigliettoCorrente;
    }
    public String getTargaCorrente() {
        return callback.getTargaCorrente();
    }
    public double getPedaggioCorrente() {
        return callback.getPedaggioCorrente();
    }

    public int getId() { return id; }
    public String getIdTrasmettitoreCorrente() { return idTrasmettitoreCorrente; }
    public boolean isAutomatico() { return automatico; }
    public boolean isEntrata() { return entrata; }

    public void disconnect() throws MqttException {
        client.disconnect();
    }
}