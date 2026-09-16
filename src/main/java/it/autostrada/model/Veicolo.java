package it.autostrada.model;

public class Veicolo {

    private String targa;
    private boolean telepassAttivo;

    public Veicolo() {
    }

    public Veicolo(String targa, boolean telepassAttivo) {
        this.targa = targa;
        this.telepassAttivo = telepassAttivo;
    }

    public String getTarga() { return targa; }
    public void setTarga(String targa) { this.targa = targa; }

    public boolean isTelepassAttivo() { return telepassAttivo; }
    public void setTelepassAttivo(boolean telepassAttivo) { this.telepassAttivo = telepassAttivo; }
}