package it.autostrada.model;

public class Tariffa {

    private int id;
    private int idCaselloIngresso;
    private int idCaselloUscita;
    private double prezzo;

    public Tariffa() {
    }

    public Tariffa(int id, int idCaselloIngresso, int idCaselloUscita, double prezzo) {
        this.id = id;
        this.idCaselloIngresso = idCaselloIngresso;
        this.idCaselloUscita = idCaselloUscita;
        this.prezzo = prezzo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdCaselloIngresso() { return idCaselloIngresso; }
    public void setIdCaselloIngresso(int idCaselloIngresso) { this.idCaselloIngresso = idCaselloIngresso; }

    public int getIdCaselloUscita() { return idCaselloUscita; }
    public void setIdCaselloUscita(int idCaselloUscita) { this.idCaselloUscita = idCaselloUscita; }

    public double getPrezzo() { return prezzo; }
    public void setPrezzo(double prezzo) { this.prezzo = prezzo; }
}