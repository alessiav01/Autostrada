package it.autostrada.model;

public class CaselloInfo {

    private int id;
    private String nome;
    private String regione;
    private double latitudine;
    private double longitudine;
    private String descrizione;

    public CaselloInfo() {
    }

    public CaselloInfo(int id, String nome, String regione, double latitudine, double longitudine, String descrizione) {
        this.id = id;
        this.nome = nome;
        this.regione = regione;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
        this.descrizione = descrizione;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getRegione() { return regione; }
    public void setRegione(String regione) { this.regione = regione; }

    public double getLatitudine() { return latitudine; }
    public void setLatitudine(double latitudine) { this.latitudine = latitudine; }

    public double getLongitudine() { return longitudine; }
    public void setLongitudine(double longitudine) { this.longitudine = longitudine; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
}