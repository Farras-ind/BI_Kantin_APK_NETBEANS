package Koneksi;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

public class Koneksi {
    private static Connection conn;

    public static Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/db_kantinbi_apk",
                    "root", "");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Koneksi database gagal!\nPastikan XAMPP (MySQL) menyala.\n" + e.getMessage());
        }
        return conn;
    }
}