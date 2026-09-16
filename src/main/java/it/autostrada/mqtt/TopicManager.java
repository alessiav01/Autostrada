package it.autostrada.mqtt;

public class TopicManager {

    private static final String ROOT = "pissir";

    private TopicManager() {
        // solo metodi statici, nessuna istanza
    }

    // ==================== ENUM ====================

    public enum Direzione {
        INGRESSO("ingresso"),
        USCITA("uscita");

        private final String value;

        Direzione(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Modalita {
        MANUALE("manuale"),
        TELEPASS("telepass");

        private final String value;

        Modalita(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // ==================== Veicolo <-> it.autostrada.casello.Casello ====================

    public static String veicoloRichiesta(Direzione direzione, Modalita modalita, int idCasello) {
        return build(direzione, modalita, idCasello, "veicolo", "richiesta");
    }

    public static String veicoloEsito(Direzione direzione, Modalita modalita, int idCasello) {
        return build(direzione, modalita, idCasello, "veicolo", "esito");
    }

    // ==================== it.autostrada.casello.Casello <-> it.autostrada.telecamera.Telecamera ====================

    public static String telecameraRichiestaLetturaTarga(Direzione direzione, Modalita modalita, int idCasello) {
        return build(direzione, modalita, idCasello, "telecamera", "richiesta-lettura-targa");
    }

    public static String telecameraTargaLetta(Direzione direzione, Modalita modalita, int idCasello) {
        return build(direzione, modalita, idCasello, "telecamera", "targa-letta");
    }

    // ==================== it.autostrada.casello.Casello <-> it.autostrada.server.Server Centrale ====================

    public static String serverDatiTransito(Direzione direzione, Modalita modalita, int idCasello) {
        return build(direzione, modalita, idCasello, "server", "dati-transito");
    }

    public static String serverEsito(Direzione direzione, Modalita modalita, int idCasello) {
        return build(direzione, modalita, idCasello, "server", "esito");
    }

    // Pagamento: esiste solo per uscita/manuale
    public static String serverPagamento(int idCasello) {
        return build(Direzione.USCITA, Modalita.MANUALE, idCasello, "server", "pagamento");
    }

    public static String serverPagamentoEsito(int idCasello) {
        return build(Direzione.USCITA, Modalita.MANUALE, idCasello, "server", "pagamento-esito");
    }

    // ==================== it.autostrada.casello.Casello <-> it.autostrada.sbarra.Sbarra ====================

    public static String sbarraComando(Direzione direzione, Modalita modalita, int idCasello) {
        return build(direzione, modalita, idCasello, "sbarra", "comando");
    }

    public static String sbarraStato(Direzione direzione, Modalita modalita, int idCasello) {
        return build(direzione, modalita, idCasello, "sbarra", "stato");
    }

    // ==================== Wildcard filter per subscribe ====================

    public static String filterTutto() {
        return ROOT + "/#";
    }

    public static String filterPerDirezione(Direzione direzione) {
        return ROOT + "/" + direzione.getValue() + "/#";
    }

    public static String filterPerModalita(Modalita modalita) {
        return ROOT + "/+/" + modalita.getValue() + "/#";
    }

    public static String filterPerCasello(int idCasello) {
        return ROOT + "/+/+/" + idCasello + "/#";
    }

    public static String filterPerAttore(String attore) {
        return ROOT + "/+/+/+/" + attore + "/#";
    }

    public static String filterServerDatiTransito() {
        return ROOT + "/+/+/+/server/dati-transito";
    }

    public static String filterServerPagamento() {
        return ROOT + "/uscita/manuale/+/server/pagamento";
    }

    // ==================== Costruzione generica ====================

    private static String build(Direzione direzione, Modalita modalita, int idCasello, String attore, String fase) {
        return ROOT + "/" + direzione.getValue() + "/" + modalita.getValue() + "/" + idCasello + "/" + attore + "/" + fase;
    }

    // ==================== Parsing di un topic ricevuto ====================

    public static TopicInfo parse(String topic) {
        String[] parts = topic.split("/");
        if (parts.length < 6 || !parts[0].equals(ROOT)) {
            throw new IllegalArgumentException("Topic non valido: " + topic);
        }
        return new TopicInfo(parts[1], parts[2], parts[3], parts[4], parts[5]);
    }

    public static class TopicInfo {
        public final String direzione;
        public final String modalita;
        public final String idCasello;
        public final String attore;
        public final String fase;

        public TopicInfo(String direzione, String modalita, String idCasello, String attore, String fase) {
            this.direzione = direzione;
            this.modalita = modalita;
            this.idCasello = idCasello;
            this.attore = attore;
            this.fase = fase;
        }

        @Override
        public String toString() {
            return "TopicInfo{direzione=" + direzione + ", modalita=" + modalita +
                    ", idCasello=" + idCasello + ", attore=" + attore + ", fase=" + fase + "}";
        }
    }
}