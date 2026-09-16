package it.autostrada.model;

public class Utente {

    private int id;
    private String username;
    private String ruolo;

    public Utente() {
    }

    public Utente(int id, String username, String ruolo) {
        this.id = id;
        this.username = username;
        this.ruolo = ruolo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRuolo() { return ruolo; }
    public void setRuolo(String ruolo) { this.ruolo = ruolo; }
}