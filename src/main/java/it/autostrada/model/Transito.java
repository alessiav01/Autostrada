package it.autostrada.model;

public class Transito {

    private int id;
    private int idBiglietto;
    private int idCaselloUscita;
    private String modalitaUscita;
    private String timestampUscita;
    private double pedaggio;
    private String stato;

    public Transito() {
    }

    public Transito(int id, int idBiglietto, int idCaselloUscita, String modalitaUscita,
                    String timestampUscita, double pedaggio, String stato) {
        this.id = id;
        this.idBiglietto = idBiglietto;
        this.idCaselloUscita = idCaselloUscita;
        this.modalitaUscita = modalitaUscita;
        this.timestampUscita = timestampUscita;
        this.pedaggio = pedaggio;
        this.stato = stato;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdBiglietto() { return idBiglietto; }
    public void setIdBiglietto(int idBiglietto) { this.idBiglietto = idBiglietto; }

    public int getIdCaselloUscita() { return idCaselloUscita; }
    public void setIdCaselloUscita(int idCaselloUscita) { this.idCaselloUscita = idCaselloUscita; }

    public String getModalitaUscita() { return modalitaUscita; }
    public void setModalitaUscita(String modalitaUscita) { this.modalitaUscita = modalitaUscita; }

    public String getTimestampUscita() { return timestampUscita; }
    public void setTimestampUscita(String timestampUscita) { this.timestampUscita = timestampUscita; }

    public double getPedaggio() { return pedaggio; }
    public void setPedaggio(double pedaggio) { this.pedaggio = pedaggio; }

    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }
}