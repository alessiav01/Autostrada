package it.autostrada.db;

import it.autostrada.model.Utente;
import it.autostrada.rest.PasswordUtil;

import java.sql.*;

public class AuthDAO {

    public Utente verificaCredenziali(String username, String password) throws SQLException {
        String hash = PasswordUtil.hash(password);
        String sql = "SELECT id, username, ruolo FROM utenti WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Utente(rs.getInt("id"), rs.getString("username"), rs.getString("ruolo"));
                }
            }
        }
        return null;
    }

    public void creaUtenteSeNonEsiste(String username, String password, String ruolo) throws SQLException {
        String checkSql = "SELECT id FROM utenti WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return; // esiste già, non fare nulla
            }
        }

        String insertSql = "INSERT INTO utenti (username, password_hash, ruolo) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, ruolo);
            ps.executeUpdate();
        }
    }
}