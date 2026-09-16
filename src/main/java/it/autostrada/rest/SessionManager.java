package it.autostrada.rest;

import it.autostrada.model.Utente;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Map<String, Utente> sessioni = new ConcurrentHashMap<>();

    public String creaSessione(Utente utente) {
        String token = UUID.randomUUID().toString();
        sessioni.put(token, utente);
        return token;
    }

    public Utente getUtente(String token) {
        return sessioni.get(token);
    }

    public void invalidaSessione(String token) {
        sessioni.remove(token);
    }
}