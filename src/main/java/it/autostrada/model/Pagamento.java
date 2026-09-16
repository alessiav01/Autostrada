package it.autostrada.model;

public class Pagamento {

    private int id;
    private int idTransito;
    private double importo;
    private String metodo;
    private String timestamp;
    private String esito;

    public Pagamento() {
    }

    public Pagamento(int id, int idTransito, double importo, String metodo, String timestamp, String esito) {
        this.id = id;
        this.idTransito = idTransito;
        this.importo = importo;
        this.metodo = metodo;
        this.timestamp = timestamp;
        this.esito = esito;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdTransito() { return idTransito; }
    public void setIdTransito(int idTransito) { this.idTransito = idTransito; }

    public double getImporto() { return importo; }
    public void setImporto(double importo) { this.importo = importo; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getEsito() { return esito; }
    public void setEsito(String esito) { this.esito = esito; }
}