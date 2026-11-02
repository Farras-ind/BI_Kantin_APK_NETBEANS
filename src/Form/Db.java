package Form;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;


public class Db {

    private static final String URL  = "jdbc:mysql://localhost:3306/db_kantinbi_apk";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static String rupiah(int angka) {
        return "Rp " + NumberFormat.getInstance(new Locale("id", "ID")).format(angka);
    }
}