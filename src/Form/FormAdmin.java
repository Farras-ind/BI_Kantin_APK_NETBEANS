/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Form;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Form Admin: kelola menu + ubah status pesanan. */
public class FormAdmin extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(FormAdmin.class.getName());

    private static final String[] STATUS = {
        "Menunggu Konfirmasi", "Sedang Dimasak", "Siap Diambil", "Selesai"
    };

    // ---- tab menu ----
    private final DefaultTableModel menuModel = tabelModel("ID", "Nama Menu", "Harga", "Kategori");
    private final JTable tblMenu = new JTable(menuModel);
    private final JTextField txtNama = new JTextField(18);
    private final JTextField txtHarga = new JTextField(10);
    private final JComboBox<String> cbKategori = new JComboBox<>(new String[]{"Makanan", "Minuman"});

    // ---- tab pesanan ----
    private final DefaultTableModel pesananModel =
            tabelModel("ID", "Siswa", "Menu", "Jumlah", "Total", "Status");
    private final JTable tblPesanan = new JTable(pesananModel);
    private final JComboBox<String> cbFilter = new JComboBox<>();
    private final JComboBox<String> cbStatus = new JComboBox<>(STATUS);

    public FormAdmin() {
        this("admin");
    }

    public FormAdmin(String username) {
        initComponents();
        getContentPane().setLayout(new BorderLayout());
        setTitle("Bi Kantin - Admin (" + username + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Kelola Menu", buildTabMenu());
        tabs.addTab("Pesanan Masuk", buildTabPesanan());

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            new FormLogin().setVisible(true);
            dispose();
        });
        JPanel atas = new JPanel(new BorderLayout());
        atas.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
        atas.add(new JLabel("Panel Admin Kantin"), BorderLayout.WEST);
        atas.add(btnLogout, BorderLayout.EAST);

        add(atas, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        loadMenu();
        cbFilter.addItem("Semua");
        for (String s : STATUS) {
            cbFilter.addItem(s);
        }
        loadPesanan();
    }

    private static DefaultTableModel tabelModel(String... kolom) {
        return new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    /* =================== TAB KELOLA MENU =================== */

    private JPanel buildTabMenu() {
        tblMenu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMenu.setRowHeight(24);
        tblMenu.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblMenu.getSelectedRow() >= 0) {
                int r = tblMenu.getSelectedRow();
                txtNama.setText(menuModel.getValueAt(r, 1).toString());
                txtHarga.setText(menuModel.getValueAt(r, 2).toString());
                cbKategori.setSelectedItem(menuModel.getValueAt(r, 3).toString());
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Data menu (klik baris tabel untuk mengubah)"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Nama menu"), g);
        g.gridx = 1; form.add(txtNama, g);
        g.gridx = 2; form.add(new JLabel("Harga (Rp)"), g);
        g.gridx = 3; form.add(txtHarga, g);
        g.gridx = 4; form.add(new JLabel("Kategori"), g);
        g.gridx = 5; form.add(cbKategori, g);

        JButton btnTambah = new JButton("Tambah");
        JButton btnUbah = new JButton("Ubah");
        JButton btnHapus = new JButton("Hapus");
        JButton btnBersih = new JButton("Bersihkan");
        btnTambah.addActionListener(e -> tambahMenu());
        btnUbah.addActionListener(e -> ubahMenu());
        btnHapus.addActionListener(e -> hapusMenu());
        btnBersih.addActionListener(e -> bersihkanForm());

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tombol.add(btnTambah);
        tombol.add(btnUbah);
        tombol.add(btnHapus);
        tombol.add(btnBersih);

        JPanel bawah = new JPanel(new BorderLayout());
        bawah.add(form, BorderLayout.CENTER);
        bawah.add(tombol, BorderLayout.SOUTH);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JScrollPane(tblMenu), BorderLayout.CENTER);
        p.add(bawah, BorderLayout.SOUTH);
        return p;
    }

    private void loadMenu() {
        menuModel.setRowCount(0);
        String sql = "SELECT id, nama_menu, harga, kategori FROM menu ORDER BY kategori, nama_menu";
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                menuModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("nama_menu"),
                    rs.getInt("harga"), rs.getString("kategori")});
            }
        } catch (SQLException ex) {
            error("Gagal memuat menu", ex);
        }
    }

    /** Validasi isian form; mengembalikan harga, atau -1 kalau tidak valid. */
    private int validasiForm() {
        if (txtNama.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama menu wajib diisi.");
            return -1;
        }
        try {
            int harga = Integer.parseInt(txtHarga.getText().trim());
            if (harga <= 0) {
                throw new NumberFormatException();
            }
            return harga;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Harga harus berupa angka lebih dari 0.");
            return -1;
        }
    }

    private int idTerpilih() {
        int r = tblMenu.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih dulu menu di tabel.");
            return -1;
        }
        return (int) menuModel.getValueAt(r, 0);
    }

    private void tambahMenu() {
        int harga = validasiForm();
        if (harga < 0) {
            return;
        }
        String sql = "INSERT INTO menu (nama_menu, harga, kategori) VALUES (?, ?, ?)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, txtNama.getText().trim());
            ps.setInt(2, harga);
            ps.setString(3, (String) cbKategori.getSelectedItem());
            ps.executeUpdate();
            bersihkanForm();
            loadMenu();
        } catch (SQLException ex) {
            error("Gagal menambah menu", ex);
        }
    }

    private void ubahMenu() {
        int id = idTerpilih();
        int harga = validasiForm();
        if (id < 0 || harga < 0) {
            return;
        }
        String sql = "UPDATE menu SET nama_menu = ?, harga = ?, kategori = ? WHERE id = ?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, txtNama.getText().trim());
            ps.setInt(2, harga);
            ps.setString(3, (String) cbKategori.getSelectedItem());
            ps.setInt(4, id);
            ps.executeUpdate();
            bersihkanForm();
            loadMenu();
        } catch (SQLException ex) {
            error("Gagal mengubah menu", ex);
        }
    }

    private void hapusMenu() {
        int id = idTerpilih();
        if (id < 0) {
            return;
        }
        try (Connection c = Db.get()) {
            // Menu yang sudah pernah dipesan tidak dihapus supaya riwayat pesanan tetap utuh
            try (PreparedStatement cek = c.prepareStatement("SELECT COUNT(*) FROM pesanan WHERE menu_id = ?")) {
                cek.setInt(1, id);
                try (ResultSet rs = cek.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(this,
                                "Menu ini sudah pernah dipesan, jadi tidak bisa dihapus.");
                        return;
                    }
                }
            }
            if (JOptionPane.showConfirmDialog(this, "Hapus menu ini?", "Konfirmasi",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM menu WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            bersihkanForm();
            loadMenu();
        } catch (SQLException ex) {
            error("Gagal menghapus menu", ex);
        }
    }

    private void bersihkanForm() {
        txtNama.setText("");
        txtHarga.setText("");
        cbKategori.setSelectedIndex(0);
        tblMenu.clearSelection();
    }

    /* =================== TAB PESANAN MASUK =================== */

    private JPanel buildTabPesanan() {
        tblPesanan.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPesanan.setRowHeight(24);
        tblPesanan.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblPesanan.getSelectedRow() >= 0) {
                cbStatus.setSelectedItem(
                        pesananModel.getValueAt(tblPesanan.getSelectedRow(), 5).toString());
            }
        });
        cbFilter.addActionListener(e -> loadPesanan());

        JButton btnRefresh = new JButton("Muat ulang");
        btnRefresh.addActionListener(e -> loadPesanan());
        JPanel atas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        atas.add(new JLabel("Filter status:"));
        atas.add(cbFilter);
        atas.add(btnRefresh);

        JButton btnSimpan = new JButton("Simpan status");
        btnSimpan.addActionListener(e -> ubahStatus());
        JPanel bawah = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bawah.add(new JLabel("Ubah status pesanan terpilih menjadi:"));
        bawah.add(cbStatus);
        bawah.add(btnSimpan);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(atas, BorderLayout.NORTH);
        p.add(new JScrollPane(tblPesanan), BorderLayout.CENTER);
        p.add(bawah, BorderLayout.SOUTH);
        return p;
    }

    private void loadPesanan() {
        pesananModel.setRowCount(0);
        String filter = (String) cbFilter.getSelectedItem();
        boolean semua = filter == null || filter.equals("Semua");
        String sql = "SELECT p.id, u.username, m.nama_menu, p.jumlah, p.total_harga, p.status "
                + "FROM pesanan p "
                + "LEFT JOIN users u ON u.id = p.user_id "
                + "LEFT JOIN menu m ON m.id = p.menu_id "
                + (semua ? "" : "WHERE p.status = ? ")
                + "ORDER BY p.id DESC";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (!semua) {
                ps.setString(1, filter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pesananModel.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("username"), rs.getString("nama_menu"),
                        rs.getInt("jumlah"), Db.rupiah(rs.getInt("total_harga")),
                        rs.getString("status")});
                }
            }
        } catch (SQLException ex) {
            error("Gagal memuat pesanan", ex);
        }
    }

    private void ubahStatus() {
        int r = tblPesanan.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih dulu pesanan di tabel.");
            return;
        }
        int id = (int) pesananModel.getValueAt(r, 0);
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("UPDATE pesanan SET status = ? WHERE id = ?")) {
            ps.setString(1, (String) cbStatus.getSelectedItem());
            ps.setInt(2, id);
            ps.executeUpdate();
            loadPesanan();
        } catch (SQLException ex) {
            error("Gagal mengubah status", ex);
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
        java.awt.EventQueue.invokeLater(() -> new FormAdmin().setVisible(true));
    }
}