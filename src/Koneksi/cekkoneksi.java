package Koneksi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class cekkoneksi {
    public static void main(String[] args) {
        Connection c = Koneksi.getConnection();
        if (c == null) {
            System.out.println("GAGAL: tidak bisa konek ke database");
            return;
        }
        System.out.println("KONEK! Database berhasil terhubung.");
        try {
            Statement st = c.createStatement();
            ResultSet rs = st.executeQuery("SELECT username, role FROM users");
            while (rs.next()) {
                System.out.println("User: " + rs.getString("username") + " (" + rs.getString("role") + ")");
            }
        } catch (Exception e) {
            System.out.println("Konek, tapi tabel users bermasalah: " + e.getMessage());
        }
    }
}