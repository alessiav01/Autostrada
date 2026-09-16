package it.autostrada.rest;

import io.javalin.Javalin;
import it.autostrada.db.AuthDAO;
import it.autostrada.db.GestioneDAO;
import it.autostrada.model.CaselloInfo;
import it.autostrada.model.Tariffa;

public class RestServer {

    private final GestioneDAO dao = new GestioneDAO();
    private Javalin app;
    private final AuthDAO authDAO = new AuthDAO();
    private final SessionManager sessionManager = new SessionManager();

    public void start(int port) {
        app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).start(port);

        // Utenti di default
        try {
            authDAO.creaUtenteSeNonEsiste("admin", "admin123", "amministratore");
            authDAO.creaUtenteSeNonEsiste("impiegato", "impiegato123", "impiegato");
        } catch (Exception e) {
            System.err.println("Errore creazione utenti di default: " + e.getMessage());
        }

        // ---------- Login ----------

        app.post("/login", ctx -> {
            var body = ctx.bodyAsClass(java.util.Map.class);
            String username = (String) body.get("username");
            String password = (String) body.get("password");

            var utente = authDAO.verificaCredenziali(username, password);
            if (utente == null) {
                ctx.status(401).json("{\"errore\":\"Credenziali non valide\"}");
                return;
            }

            String token = sessionManager.creaSessione(utente);
            ctx.json(java.util.Map.of("token", token, "ruolo", utente.getRuolo(), "username", utente.getUsername()));
        });

        // ---------- Filtro globale: Login obbligatorio per tutte le API ----------

        app.before(ctx -> {
            String path = ctx.path();

            // Esclude i file statici del frontend e l'endpoint di login
            if (path.equals("/login") || path.equals("/") || path.endsWith(".html") || path.endsWith(".js") || path.endsWith(".css") || path.endsWith(".ico")) {
                return;
            }

            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(401).json("{\"errore\":\"Token mancante\"}");
                throw new io.javalin.http.HttpResponseException(401, "Non autenticato");
            }

            String token = authHeader.substring("Bearer ".length());
            var utente = sessionManager.getUtente(token);
            if (utente == null) {
                ctx.status(401).json("{\"errore\":\"Token non valido\"}");
                throw new io.javalin.http.HttpResponseException(401, "Token non valido");
            }

            ctx.attribute("utente", utente);
        });

        // ---------- Endpoints CASELLI ----------

        app.get("/caselli", ctx -> {
            ctx.json(dao.elencoCaselli());
        });

        app.post("/caselli", ctx -> {
            richiedeAmministratore(ctx);
            CaselloInfo nuovo = ctx.bodyAsClass(CaselloInfo.class);
            int id = dao.aggiungiCasello(nuovo);
            ctx.status(201).json("{\"id\":" + id + "}");
        });

        app.delete("/caselli/{id}", ctx -> {
            richiedeAmministratore(ctx);
            int id = Integer.parseInt(ctx.pathParam("id"));
            boolean rimosso = dao.rimuoviCasello(id);
            if (rimosso) {
                ctx.status(204);
            } else {
                ctx.status(404).json("{\"errore\":\"Casello non trovato\"}");
            }
        });

        // ---------- Endpoints TARIFFE ----------

        app.get("/tariffe", ctx -> {
            ctx.json(dao.elencoTariffe());
        });

        app.put("/tariffe", ctx -> {
            richiedeAmministratore(ctx);
            Tariffa t = ctx.bodyAsClass(Tariffa.class);
            dao.impostaTariffa(t.getIdCaselloIngresso(), t.getIdCaselloUscita(), t.getPrezzo());
            ctx.status(200).json("{\"ok\":true}");
        });

        // ---------- Endpoints TRANSITI & MEZZI ----------

        // Storico completo dei transiti
        app.get("/transiti", ctx -> {
            ctx.json(dao.elencoTransiti());
        });

        // Mezzi attualmente in circolazione (Transiti aperti)
        app.get("/transiti/in-circolazione", ctx -> {
            ctx.json(dao.elencoMezziInCircolazione());
        });

        // Transiti pendenti da riscuotere (Telepass)
        app.get("/transiti/da-riscuotere", ctx -> {
            ctx.json(dao.elencoTransitiDaRiscuotere());
        });

        // Chiusura del pagamento Telepass
        app.post("/transiti/{id}/chiudi-pagamento", ctx -> {
            richiedeAmministratore(ctx);
            int id = Integer.parseInt(ctx.pathParam("id"));
            boolean chiuso = dao.chiudiPagamentoTelepass(id);
            if (chiuso) {
                ctx.status(200).json("{\"ok\":true}");
            } else {
                ctx.status(404).json("{\"errore\":\"Transito non trovato o già pagato\"}");
            }
        });


        // ---------- Endpoints STATISTICHE / REPORT ----------
        // ---------- Endpoints STATISTICHE / REPORT ----------
        app.get("/statistiche", ctx -> {
            try {
                var tuttiITransiti = dao.elencoTransiti();
                double incassoTotale = 0.0;
                int transitiPagati = 0;
                int transitiDaRiscuotere = 0;

                for (var t : tuttiITransiti) {
                    if ("pagato".equalsIgnoreCase(t.getStato())) {
                        transitiPagati++;
                        incassoTotale += t.getPedaggio();
                    } else if ("riscuotere".equalsIgnoreCase(t.getStato())) {
                        transitiDaRiscuotere++;
                    }
                }

                ctx.json(java.util.Map.of(
                        "incassoTotale", incassoTotale,
                        "transitiTotali", tuttiITransiti.size(),
                        "transitiPagati", transitiPagati,
                        "transitiDaRiscuotere", transitiDaRiscuotere
                ));
            } catch (Exception e) {
                ctx.status(500).json(java.util.Map.of("errore", "Impossibile calcolare le statistiche"));
            }
        });

        app.get("/multe", ctx -> {
            ctx.json(dao.elencoMulte());
        });
    }

    private void richiedeAmministratore(io.javalin.http.Context ctx) {
        it.autostrada.model.Utente utente = ctx.attribute("utente");
        if (utente == null || !utente.getRuolo().equals("amministratore")) {
            ctx.status(403).json("{\"errore\":\"Operazione riservata agli amministratori\"}");
            throw new io.javalin.http.HttpResponseException(403, "Accesso negato");
        }
    }

    public void stop() {
        if (app != null) app.stop();
    }
}






