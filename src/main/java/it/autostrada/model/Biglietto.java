package it.autostrada.model;

public class Biglietto {

    private int id;
    private String codice;
    private String targa;
    private int idCaselloIngresso;
    private String modalitaIngresso;
    private String timestampIngresso;
    private String idTrasmettitore;

    public Biglietto() {
    }

    public Biglietto(int id, String codice, String targa, int idCaselloIngresso,
                     String modalitaIngresso, String timestampIngresso, String idTrasmettitore) {
        this.id = id;
        this.codice = codice;
        this.targa = targa;
        this.idCaselloIngresso = idCaselloIngresso;
        this.modalitaIngresso = modalitaIngresso;
        this.timestampIngresso = timestampIngresso;
        this.idTrasmettitore = idTrasmettitore;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodice() { return codice; }
    public void setCodice(String codice) { this.codice = codice; }

    public String getTarga() { return targa; }
    public void setTarga(String targa) { this.targa = targa; }

    public int getIdCaselloIngresso() { return idCaselloIngresso; }
    public void setIdCaselloIngresso(int idCaselloIngresso) { this.idCaselloIngresso = idCaselloIngresso; }

    public String getModalitaIngresso() { return modalitaIngresso; }
    public void setModalitaIngresso(String modalitaIngresso) { this.modalitaIngresso = modalitaIngresso; }

    public String getTimestampIngresso() { return timestampIngresso; }
    public void setTimestampIngresso(String timestampIngresso) { this.timestampIngresso = timestampIngresso; }

    public String getIdTrasmettitore() { return idTrasmettitore; }
    public void setIdTrasmettitore(String idTrasmettitore) { this.idTrasmettitore = idTrasmettitore; }
}