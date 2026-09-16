package it.autostrada.db;

import it.autostrada.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GestioneDAO {

    // ---------- Caselli ----------

    public List<CaselloInfo> elencoCaselli() throws SQLException {
        List<CaselloInfo> risultato = new ArrayList<>();
        String sql = "SELECT id, nome, regione, latitudine, longitudine, descrizione FROM caselli ORDER BY nome";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                risultato.add(new CaselloInfo(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("regione"),
                        rs.getDouble("latitudine"),
                        rs.getDouble("longitudine"),
                        rs.getString("descrizione")
                ));
            }
        }
        return risultato;
    }

    public CaselloInfo trovaCasello(int id) throws SQLException {
        String sql = "SELECT id, nome, regione, latitudine, longitudine, descrizione FROM caselli WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CaselloInfo(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getString("regione"),
                            rs.getDouble("latitudine"),
                            rs.getDouble("longitudine"),
                            rs.getString("descrizione")
                    );
                }
            }
        }
        return null;
    }

    public int aggiungiCasello(CaselloInfo c) throws SQLException {
        String sql = "INSERT INTO caselli (nome, regione, latitudine, longitudine, descrizione) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getRegione());
            ps.setDouble(3, c.getLatitudine());
            ps.setDouble(4, c.getLongitudine());
            ps.setString(5, c.getDescrizione());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public boolean rimuoviCasello(int id) throws SQLException {
        String sql = "DELETE FROM caselli WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- Tariffe ----------

    public List<Tariffa> elencoTariffe() throws SQLException {
        List<Tariffa> risultato = new ArrayList<>();
        String sql = "SELECT id, id_casello_ingresso, id_casello_uscita, prezzo FROM tariffe ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                risultato.add(new Tariffa(
                        rs.getInt("id"),
                        rs.getInt("id_casello_ingresso"),
                        rs.getInt("id_casello_uscita"),
                        rs.getDouble("prezzo")
                ));
            }
        }
        return risultato;
    }

    public Double trovaPrezzoTariffa(int idIngresso, int idUscita) throws SQLException {
        String sql = "SELECT prezzo FROM tariffe WHERE id_casello_ingresso = ? AND id_casello_uscita = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idIngresso);
            ps.setInt(2, idUscita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("prezzo");
                }
            }
        }
        return null;
    }

    public void impostaTariffa(int idIngresso, int idUscita, double prezzo) throws SQLException {
        String sql = """
                INSERT INTO tariffe (id_casello_ingresso, id_casello_uscita, prezzo) VALUES (?, ?, ?)
                ON CONFLICT(id_casello_ingresso, id_casello_uscita) DO UPDATE SET prezzo = excluded.prezzo
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idIngresso);
            ps.setInt(2, idUscita);
            ps.setDouble(3, prezzo);
            ps.executeUpdate();
        }
    }

    // ---------- Transiti ----------

    public List<Transito> elencoTransiti() throws SQLException {
        List<Transito> risultato = new ArrayList<>();
        String sql = "SELECT id, id_biglietto, id_casello_uscita, modalita_uscita, timestamp_uscita, pedaggio, stato FROM transiti ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                risultato.add(mappaTransito(rs));
            }
        }
        return risultato;
    }

    public List<Transito> elencoTransitiDaRiscuotere() throws SQLException {
        List<Transito> risultato = new ArrayList<>();
        String sql = "SELECT id, id_biglietto, id_casello_uscita, modalita_uscita, timestamp_uscita, pedaggio, stato FROM transiti WHERE stato = 'riscuotere' ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                risultato.add(mappaTransito(rs));
            }
        }
        return risultato;
    }

    public List<Biglietto> elencoMezziInCircolazione() throws SQLException {
        List<Biglietto> risultato = new ArrayList<>();
        String sql = """
            SELECT b.id, b.codice, b.targa, b.id_casello_ingresso, b.modalita_ingresso, b.timestamp_ingresso, b.id_trasmettitore
            FROM biglietti b
            LEFT JOIN transiti t ON t.id_biglietto = b.id
            WHERE t.id IS NULL
            ORDER BY b.timestamp_ingresso DESC
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                risultato.add(new Biglietto(
                        rs.getInt("id"),
                        rs.getString("codice"),
                        rs.getString("targa"),
                        rs.getInt("id_casello_ingresso"),
                        rs.getString("modalita_ingresso"),
                        rs.getString("timestamp_ingresso"),
                        rs.getString("id_trasmettitore")
                ));
            }
        }
        return risultato;
    }

    public int registraNuovoIngresso(int idBiglietto) throws SQLException {
        String sql = "INSERT INTO transiti (id_biglietto, stato) VALUES (?, 'aperto')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idBiglietto);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public boolean registraUscitaTransito(int idTransito, int idCaselloUscita, String modalitaUscita, String timestampUscita, double pedaggio, String nuovoStato) throws SQLException {
        String sql = """
                UPDATE transiti 
                SET id_casello_uscita = ?, modalita_uscita = ?, timestamp_uscita = ?, pedaggio = ?, stato = ? 
                WHERE id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCaselloUscita);
            ps.setString(2, modalitaUscita);
            ps.setString(3, timestampUscita);
            ps.setDouble(4, pedaggio);
            ps.setString(5, nuovoStato);
            ps.setInt(6, idTransito);
            return ps.executeUpdate() > 0;
        }
    }

    public Transito trovaTransitoApertoPerBiglietto(int idBiglietto) throws SQLException {
        String sql = "SELECT id, id_biglietto, id_casello_uscita, modalita_uscita, timestamp_uscita, pedaggio, stato FROM transiti WHERE id_biglietto = ? AND (stato = 'aperto' OR id_casello_uscita IS NULL)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBiglietto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mappaTransito(rs);
                }
            }
        }
        return null;
    }

    public boolean chiudiPagamentoTelepass(int idTransito) throws SQLException {
        String sql = "UPDATE transiti SET stato = 'pagato' WHERE id = ? AND stato = 'riscuotere'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTransito);
            return ps.executeUpdate() > 0;
        }
    }

    private Transito mappaTransito(ResultSet rs) throws SQLException {
        return new Transito(
                rs.getInt("id"),
                rs.getInt("id_biglietto"),
                rs.getInt("id_casello_uscita"),
                rs.getString("modalita_uscita"),
                rs.getString("timestamp_uscita"),
                rs.getDouble("pedaggio"),
                rs.getString("stato")
        );
    }
//--------MULTE---
    public List<Multa> elencoMulte() throws SQLException {
        List<Multa> risultato = new ArrayList<>();
        String sql = "SELECT id, id_biglietto, targa, id_casello_ingresso, id_casello_uscita, velocita_media, timestamp FROM multe ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                risultato.add(new Multa(
                        rs.getInt("id"),
                        rs.getInt("id_biglietto"),
                        rs.getString("targa"),
                        rs.getInt("id_casello_ingresso"),
                        rs.getInt("id_casello_uscita"),
                        rs.getDouble("velocita_media"),
                        rs.getString("timestamp")
                ));
            }
        }
        return risultato;
    }


}





