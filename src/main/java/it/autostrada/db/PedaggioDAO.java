package it.autostrada.db;

import java.sql.*;
import java.util.UUID;

public class PedaggioDAO {



    public record BigliettoInfo(String targa, int idCaselloIngresso, String timestampIngresso) {}

    // ---------- INGRESSO: crea biglietto ----------


public String registraIngresso(String targa, String idCasello, String modalita, String idTrasmettitore) throws SQLException {
    assicuraVeicolo(targa, modalita.equals("telepass"));

    String codice = UUID.randomUUID().toString();

    String sql = "INSERT INTO biglietti (codice, targa, id_casello_ingresso, modalita_ingresso, id_trasmettitore) VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, codice);
        ps.setString(2, targa);
        ps.setInt(3, Integer.parseInt(idCasello));
        ps.setString(4, modalita);
        ps.setString(5, idTrasmettitore); // null per manuale, va bene
        ps.executeUpdate();
    }
    return codice;
}


    public BigliettoInfo trovaInfoBiglietto(int idBiglietto) throws SQLException {
        String sql = "SELECT targa, id_casello_ingresso, timestamp_ingresso FROM biglietti WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBiglietto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BigliettoInfo(
                            rs.getString("targa"),
                            rs.getInt("id_casello_ingresso"),
                            rs.getString("timestamp_ingresso")
                    );
                }
            }
        }
        return null;
    }
    // ---------- USCITA: calcola pedaggio (manuale) o registra subito da riscuotere (telepass) ----------

    private static final double PREZZO_PER_KM = 0.12; // EUR/km, valore indicativo

    public double calcolaPedaggio(int idCaselloIngresso, int idCaselloUscita) throws SQLException {
        // 1. Se esiste già una tariffa memorizzata per questa tratta, usala
        String sql = "SELECT prezzo FROM tariffe WHERE id_casello_ingresso = ? AND id_casello_uscita = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCaselloIngresso);
            ps.setInt(2, idCaselloUscita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("prezzo");
                }
            }
        }

        // 2. Nessuna tariffa esplicita: calcola in base alla distanza reale tra i caselli
        double distanzaKm = calcolaDistanzaKm(idCaselloIngresso, idCaselloUscita);
        double prezzo = Math.round(distanzaKm * PREZZO_PER_KM * 100.0) / 100.0;

        System.out.println("[it.autostrada.db.PedaggioDAO] Tariffa non trovata, calcolata per distanza: "
                + Math.round(distanzaKm * 10.0) / 10.0 + " km -> " + prezzo + " EUR");

        // 3. Memorizza la nuova tariffa calcolata, per le prossime volte
        salvaTariffa(idCaselloIngresso, idCaselloUscita, prezzo);

        return prezzo;
    }

    public double calcolaVelocitaMediaKmh(int idCaselloIngresso, int idCaselloUscita, String timestampIngresso) throws SQLException {
        double distanzaKm = calcolaDistanzaKm(idCaselloIngresso, idCaselloUscita);

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        java.time.LocalDateTime ingresso = java.time.LocalDateTime.parse(timestampIngresso, fmt);
        java.time.LocalDateTime uscita = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);

        double oreImpiegate = java.time.Duration.between(ingresso, uscita).getSeconds() / 3600.0;
        if (oreImpiegate <= 0) oreImpiegate = 0.001; // evita divisione per zero su test istantanei

        return distanzaKm / oreImpiegate;
    }

    public void registraMulta(int idBiglietto, String targa, int idCaselloIngresso, int idCaselloUscita, double velocitaMedia) throws SQLException {
        String sql = "INSERT INTO multe (id_biglietto, targa, id_casello_ingresso, id_casello_uscita, velocita_media) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBiglietto);
            ps.setString(2, targa);
            ps.setInt(3, idCaselloIngresso);
            ps.setInt(4, idCaselloUscita);
            ps.setDouble(5, velocitaMedia);
            ps.executeUpdate();
        }
    }

    private double calcolaDistanzaKm(int idIngresso, int idUscita) throws SQLException {
        double[] coordIngresso = getCoordinate(idIngresso);
        double[] coordUscita = getCoordinate(idUscita);
        return haversine(coordIngresso[0], coordIngresso[1], coordUscita[0], coordUscita[1]);
    }

    private double[] getCoordinate(int idCasello) throws SQLException {
        String sql = "SELECT latitudine, longitudine FROM caselli WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCasello);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new double[]{rs.getDouble("latitudine"), rs.getDouble("longitudine")};
                }
            }
        }
        throw new SQLException("it.autostrada.casello.Casello " + idCasello + " non trovato in tabella caselli");
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // raggio terrestre in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void salvaTariffa(int idIngresso, int idUscita, double prezzo) throws SQLException {
        String sql = "INSERT OR IGNORE INTO tariffe (id_casello_ingresso, id_casello_uscita, prezzo) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idIngresso);
            ps.setInt(2, idUscita);
            ps.setDouble(3, prezzo);
            ps.executeUpdate();
        }
    }

    public int trovaBigliettoApertoPerTarga(String targa) throws SQLException {
        String sql = """
            SELECT b.id FROM biglietti b
            WHERE b.targa = ?
              AND NOT EXISTS (SELECT 1 FROM transiti t WHERE t.id_biglietto = b.id)
            ORDER BY b.id DESC LIMIT 1
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }

    public int trovaBigliettoPerCodice(String codice) throws SQLException {
        String sql = """
        SELECT b.id FROM biglietti b
        WHERE b.codice = ?
          AND NOT EXISTS (SELECT 1 FROM transiti t WHERE t.id_biglietto = b.id)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codice);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }

    public void registraTransitoRiscuotere(int idBiglietto, String idCasello, String modalita, double pedaggio) throws SQLException {
        String sql = "INSERT INTO transiti (id_biglietto, id_casello_uscita, modalita_uscita, pedaggio, stato) VALUES (?, ?, ?, ?, 'riscuotere')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idBiglietto);
            ps.setInt(2, Integer.parseInt(idCasello));
            ps.setString(3, modalita);
            ps.setDouble(4, pedaggio);
            ps.executeUpdate();
        }
    }

    // ---------- PAGAMENTO (uscita manuale) ----------

    public int registraTransitoPagato(int idBiglietto, String idCasello, double pedaggio) throws SQLException {
        String sql = "INSERT INTO transiti (id_biglietto, id_casello_uscita, modalita_uscita, pedaggio, stato) VALUES (?, ?, 'manuale', ?, 'pagato')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idBiglietto);
            ps.setInt(2, Integer.parseInt(idCasello));
            ps.setDouble(3, pedaggio);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public void registraPagamento(int idTransito, double importo) throws SQLException {
        String sql = "INSERT INTO pagamenti (id_transito, importo, esito) VALUES (?, ?, 'confermato')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTransito);
            ps.setDouble(2, importo);
            ps.executeUpdate();
        }
    }

    // ---------- Helper ----------

    private void assicuraVeicolo(String targa, boolean telepassAttivo) throws SQLException {
        String insertSql = "INSERT OR IGNORE INTO veicoli (targa, telepass_attivo) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, targa);
            ps.setInt(2, telepassAttivo ? 1 : 0);
            ps.executeUpdate();
        }

        // Se il veicolo esisteva già (es. inserito prima con modalità manuale)
        // e ora arriva con telepass, aggiorniamo il flag
        if (telepassAttivo) {
            String updateSql = "UPDATE veicoli SET telepass_attivo = 1 WHERE targa = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, targa);
                ps.executeUpdate();
            }
        }
    }
}