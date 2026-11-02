/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Form;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/** Form Siswa: pesan menu (keranjang) + tracking status pesanan. */
public class FormSiswa extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(FormSiswa.class.getName());

    /** Satu baris di keranjang (hanya di memori sampai siswa menekan "Pesan Sekarang"). */
    private static class ItemKeranjang {
        int menuId;
        String nama;
        int harga;
        int jumlah;
    }

    private final int userId;
    private final List<ItemKeranjang> keranjang = new ArrayList<>();

    private final DefaultTableModel menuModel = tabelModel("ID", "Nama Menu", "Harga", "Kategori");
    private final JTable tblMenu = new JTable(menuModel);
    private final JComboBox<String> cbKategori =
            new JComboBox<>(new String[]{"Semua", "Makanan", "Minuman"});
    private final JSpinner spJumlah = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));

    private final DefaultTableModel cartModel = tabelModel("Menu", "Harga", "Jumlah", "Subtotal");
    private final JTable tblCart = new JTable(cartModel);
    private final JLabel lblTotal = new JLabel("Total: Rp 0");

    private final DefaultTableModel statusModel =
            tabelModel("ID", "Menu", "Jumlah", "Total", "Status");
    private final JTable tblStatus = new JTable(statusModel);
    private final JLabel lblUpdate = new JLabel(" ");
    private final JTabbedPane tabs = new JTabbedPane();
    private final Timer timerRefresh;

    public FormSiswa() {
        this(0, "siswa");
    }

    public FormSiswa(int userId, String username) {
        this.userId = userId;
        initComponents();
        getContentPane().setLayout(new BorderLayout());
        setTitle("Bi Kantin - Siswa (" + username + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);

        tabs.addTab("Pesan Menu", buildTabPesan());
        tabs.addTab("Status Pesanan", buildTabStatus());

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            new FormLogin().setVisible(true);
            dispose();
        });
        JPanel atas = new JPanel(new BorderLayout());
        atas.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
        atas.add(new JLabel("Halo, " + username + "! Selamat memesan."), BorderLayout.WEST);
        atas.add(btnLogout, BorderLayout.EAST);

        add(atas, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        loadMenu();
        loadStatus(false);

        // Tracking "real-time": status dimuat ulang otomatis tiap 5 detik
        timerRefresh = new Timer(5000, e -> loadStatus(false));
        timerRefresh.start();
    }

    @Override
    public void dispose() {
        if (timerRefresh != null) {
            timerRefresh.stop();
        }
        super.dispose();
    }

    private static DefaultTableModel tabelModel(String... kolom) {
        return new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    /* =================== TAB PESAN MENU =================== */

    private JPanel buildTabPesan() {
        tblMenu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMenu.setRowHeight(24);
        tblCart.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCart.setRowHeight(24);
        cbKategori.addActionListener(e -> loadMenu());

        JButton btnTambah = new JButton("Tambah ke Keranjang");
        btnTambah.addActionListener(e -> tambahKeKeranjang());
        JPanel kontrolMenu = new JPanel(new FlowLayout(FlowLayout.LEFT));
        kontrolMenu.add(new JLabel("Kategori:"));
        kontrolMenu.add(cbKategori);
        kontrolMenu.add(new JLabel("Jumlah:"));
        kontrolMenu.add(spJumlah);
        kontrolMenu.add(btnTambah);

        JPanel panelMenu = new JPanel(new BorderLayout(4, 4));
        panelMenu.setBorder(BorderFactory.createTitledBorder("Daftar menu"));
        panelMenu.add(new JScrollPane(tblMenu), BorderLayout.CENTER);
        panelMenu.add(kontrolMenu, BorderLayout.SOUTH);

        JButton btnHapusItem = new JButton("Hapus item");
        JButton btnPesan = new JButton("Pesan Sekarang");
        btnHapusItem.addActionListener(e -> hapusItem());
        btnPesan.addActionListener(e -> checkout());
        JPanel kontrolCart = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        kontrolCart.add(lblTotal);
        kontrolCart.add(btnHapusItem);
        kontrolCart.add(btnPesan);

        JPanel panelCart = new JPanel(new BorderLayout(4, 4));
        panelCart.setBorder(BorderFactory.createTitledBorder("Keranjang"));
        panelCart.add(new JScrollPane(tblCart), BorderLayout.CENTER);
        panelCart.add(kontrolCart, BorderLayout.SOUTH);

        JPanel p = new JPanel(new java.awt.GridLayout(2, 1, 8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(panelMenu);
        p.add(panelCart);
        return p;
    }

    private void loadMenu() {
        menuModel.setRowCount(0);
        String kat = (String) cbKategori.getSelectedItem();
        boolean semua = kat == null || kat.equals("Semua");
        String sql = "SELECT id, nama_menu, harga, kategori FROM menu "
                + (semua ? "" : "WHERE kategori = ? ")
                + "ORDER BY kategori, nama_menu";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (!semua) {
                ps.setString(1, kat);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    menuModel.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("nama_menu"),
                        rs.getInt("harga"), rs.getString("kategori")});
                }
            }
        } catch (SQLException ex) {
            error("Gagal memuat menu", ex);
        }
    }

    private void tambahKeKeranjang() {
        int r = tblMenu.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih dulu menu di tabel.");
            return;
        }
        int menuId = (int) menuModel.getValueAt(r, 0);
        int jumlah = (int) spJumlah.getValue();

        for (ItemKeranjang it : keranjang) {
            if (it.menuId == menuId) {   // menu yang sama: tambahkan jumlahnya
                it.jumlah += jumlah;
                refreshKeranjang();
                return;
            }
        }
        ItemKeranjang baru = new ItemKeranjang();
        baru.menuId = menuId;
        baru.nama = menuModel.getValueAt(r, 1).toString();
        baru.harga = (int) menuModel.getValueAt(r, 2);
        baru.jumlah = jumlah;
        keranjang.add(baru);
        refreshKeranjang();
    }

    private void hapusItem() {
        int r = tblCart.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih dulu item di keranjang.");
            return;
        }
        keranjang.remove(r);
        refreshKeranjang();
    }

    private void refreshKeranjang() {
        cartModel.setRowCount(0);
        int total = 0;
        for (ItemKeranjang it : keranjang) {
            int sub = it.harga * it.jumlah;
            total += sub;
            cartModel.addRow(new Object[]{it.nama, Db.rupiah(it.harga), it.jumlah, Db.rupiah(sub)});
        }
        lblTotal.setText("Total: " + Db.rupiah(total));
    }

    /** Insert ke tabel pesanan: satu baris per menu, semuanya dalam satu transaksi. */
    private void checkout() {
        if (keranjang.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keranjang masih kosong.");
            return;
        }
        String sqlHarga = "SELECT harga FROM menu WHERE id = ?";
        String sqlInsert = "INSERT INTO pesanan (user_id, menu_id, jumlah, total_harga, status) "
                + "VALUES (?, ?, ?, ?, 'Menunggu Konfirmasi')";

        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement psHarga = c.prepareStatement(sqlHarga);
                 PreparedStatement psInsert = c.prepareStatement(sqlInsert)) {
                for (ItemKeranjang it : keranjang) {
                    psHarga.setInt(1, it.menuId);
                    try (ResultSet rs = psHarga.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Menu \"" + it.nama + "\" sudah tidak tersedia.");
                        }
                        int hargaTerbaru = rs.getInt(1);   // harga selalu dari database
                        psInsert.setInt(1, userId);
                        psInsert.setInt(2, it.menuId);
                        psInsert.setInt(3, it.jumlah);
                        psInsert.setInt(4, hargaTerbaru * it.jumlah);
                        psInsert.addBatch();
                    }
                }
                psInsert.executeBatch();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            error("Pesanan gagal dibuat", ex);
            return;
        }

        keranjang.clear();
        refreshKeranjang();
        loadStatus(false);
        tabs.setSelectedIndex(1);
        JOptionPane.showMessageDialog(this, "Pesanan berhasil dibuat. Pantau statusnya di tab ini.");
    }

    /* =================== TAB STATUS PESANAN =================== */

    private JPanel buildTabStatus() {
        tblStatus.setRowHeight(24);
        JButton btnRefresh = new JButton("Muat ulang");
        btnRefresh.addActionListener(e -> loadStatus(true));

        JPanel atas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        atas.add(btnRefresh);
        atas.add(lblUpdate);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(atas, BorderLayout.NORTH);
        p.add(new JScrollPane(tblStatus), BorderLayout.CENTER);
        return p;
    }

    /** @param tampilkanError false untuk refresh otomatis supaya tidak memunculkan dialog berulang. */
    private void loadStatus(boolean tampilkanError) {
        int terpilih = tblStatus.getSelectedRow();
        String sql = "SELECT p.id, m.nama_menu, p.jumlah, p.total_harga, p.status "
                + "FROM pesanan p LEFT JOIN menu m ON m.id = p.menu_id "
                + "WHERE p.user_id = ? ORDER BY p.id DESC";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                statusModel.setRowCount(0);
                while (rs.next()) {
                    statusModel.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("nama_menu"), rs.getInt("jumlah"),
                        Db.rupiah(rs.getInt("total_harga")), rs.getString("status")});
                }
            }
            if (terpilih >= 0 && terpilih < statusModel.getRowCount()) {
                tblStatus.setRowSelectionInterval(terpilih, terpilih);
            }
            lblUpdate.setText("Diperbarui otomatis tiap 5 detik. Terakhir: "
                    + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
        } catch (SQLException ex) {
            if (tampilkanError) {
                error("Gagal memuat status pesanan", ex);
            }
        }
    }

    private void error(String judul, SQLException ex) {
        JOptionPane.showMessageDialog(this, judul + ":\n" + ex.getMessage(),
                "Error database", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>                        

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new FormSiswa().setVisible(true));
    }
}