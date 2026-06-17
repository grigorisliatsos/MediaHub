import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.imageio.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main extends JFrame {

    // ── Auto-save path ────────────────────────────────────────────────────────
    private static final File DEFAULT_SAVE_FILE =
            new File(System.getProperty("user.home"), "mediahub_collection.csv");

    // Κυριο αρχειο αποθηκευσης (JSON). Το CSV κρατιεται μονο ως παλιο αντιγραφο.
    private static final File DEFAULT_JSON_FILE =
            new File(System.getProperty("user.home"), "mediahub_collection.json");

    // ── Φακελος αποθηκευσης εικονων (δικα μας αντιγραφα, ανεξαρτητα απο τα πρωτοτυπα)
    private static final File IMAGE_DIR =
            new File(System.getProperty("user.home"), ".mediahub_images");

    // ── Palette ───────────────────────────────────────────────────────────────
    static boolean darkMode = false;

    static Color bg()       { return darkMode ? new Color(15,15,20)    : new Color(248,248,246); }
    static Color surface()  { return darkMode ? new Color(25,25,32)    : Color.WHITE; }
    static Color surface2() { return darkMode ? new Color(36,36,46)    : new Color(243,242,239); }
    static Color border()   { return darkMode ? new Color(58,58,72)    : new Color(218,216,210); }
    static Color txt()      { return darkMode ? new Color(240,240,245) : new Color(22,22,20); }
    static Color txtMuted() { return darkMode ? new Color(165,165,175) : new Color(115,113,108); }

    // Νέο Petrol / Teal Accent Χρώμα
    static Color accent()   { return new Color(11, 130, 134); }
    static Color accentHov(){ return new Color(8, 104, 107); }

    static Color movieCol()  { return darkMode ? new Color(37,99,235)   : new Color(55,138,221); }
    static Color movieBg()   { return darkMode ? new Color(23,37,84)    : new Color(230,241,251); }
    static Color seriesCol() { return darkMode ? new Color(124,58,237)  : new Color(99,60,210); }
    static Color seriesBg()  { return darkMode ? new Color(46,16,101)   : new Color(237,233,254); }
    static Color bookCol()   { return darkMode ? new Color(16,185,129)  : new Color(5,120,85); }
    static Color bookBg()    { return darkMode ? new Color(6,78,59)     : new Color(209,250,229); }
    static Color gameCol()   { return darkMode ? new Color(245,158,11)  : new Color(180,100,0); }
    static Color gameBg()    { return darkMode ? new Color(78,52,4)     : new Color(254,243,199); }

    static Color starOn()   { return new Color(251,191,36); }
    static Color starOff()  { return darkMode ? new Color(60,60,72)    : new Color(209,207,200); }
    static Color danger()   { return new Color(239,68,68); }
    static Color dangerBg() { return darkMode ? new Color(69,10,10)    : new Color(254,226,226); }
    static Color editCol()  { return new Color(16,185,129); }
    static Color editBg()   { return darkMode ? new Color(6,78,59)     : new Color(209,250,229); }

    static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD,  20);
    static final Font FONT_CARD  = new Font("Segoe UI", Font.BOLD,  13);
    static final Font FONT_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD,  10);
    static final Font FONT_SYM   = new Font("Segoe UI Symbol", Font.PLAIN, 13);
    static final Font FONT_SYM_L = new Font("Segoe UI Symbol", Font.PLAIN, 20);

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<MediaItem> collection = new ArrayList<>();
    private String filterType  = "all";
    private String searchQuery = "";
    private boolean filterFavorite = false;     // μονο αγαπημενα
    private boolean dirty = false;               // υπαρχουν μη-αποθηκευμενες αλλαγες;
    private int     filterMinRating = 0;         // 0 = ολες, 4 = 4+ αστερια
    private String  filterStatus = "";           // "" = ολες οι καταστασεις

    // Κωδικα & ετικετες καταστασης
    static final String[] STATUS_KEYS   = {"", "pending", "in_progress", "completed", "abandoned"};
    static final String[] STATUS_LABELS = {"—", "Εκκρεμεί", "Σε εξέλιξη", "Ολοκληρωμένο", "Εγκαταλείφθηκε"};
    static String statusLabel(String key){
        for (int i=0;i<STATUS_KEYS.length;i++) if (STATUS_KEYS[i].equals(key)) return STATUS_LABELS[i];
        return "—";
    }
    static String statusKeyFromLabel(String label){
        for (int i=0;i<STATUS_LABELS.length;i++) if (STATUS_LABELS[i].equals(label)) return STATUS_KEYS[i];
        return "";
    }
    static Color statusColor(String key){
        return switch (key) {
            case "pending"     -> new Color(217,119,6);
            case "in_progress" -> new Color(37,99,235);
            case "completed"   -> new Color(16,185,129);
            case "abandoned"   -> new Color(120,120,130);
            default            -> new Color(150,150,160);
        };
    }

    // ── Undo (αναιρεση) ───────────────────────────────────────────────────────
    private final Deque<List<MediaItem>> undoStack = new ArrayDeque<>();
    private JButton undoBtn;                         // κουμπι στο toolbar

    // ── Εικονες ───────────────────────────────────────────────────────────────
    private String pendingImagePath = "";            // εικονα επιλεγμενη στη φορμα προσθηκης
    private JLabel imageNameLabel;                    // ονομα αρχειου που εμφανιζεται
    private final Map<String, ImageIcon> thumbCache = new HashMap<>();

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JPanel      rootPanel, listPanel, statsPanel;
    private JTextField  searchField;
    private JComboBox<String> typeBox, sortBox;
    private JTextField  titleField, yearField, genreField, extraField1, extraField2, extraField3;
    private JTextField  dateField1, dateField2, platDateField;
    private JCheckBox   platCheckBox;
    private JLabel      extraLabel1, extraLabel2, extraLabel3, dateLabel1, dateLabel2, platDateLabel;
    private JPanel      dateRow1Panel, dateRow2Panel, platPanel, extraRow3Panel;
    private JLabel      countLabel;
    private JTabbedPane tabs;
    private JPanel      filterBar;
    private RatingPanel ratingPanel;
    private JTextArea   notesArea;
    private JComboBox<String> statusBox;        // φορμα: κατασταση
    private JCheckBox   favCheckBox;            // φορμα: αγαπημενο
    private JTextField  tagsField;              // φορμα: ετικετες
    private JComboBox<String> statusFilterBox;  // φιλτρο καταστασης
    private ChipButton favChip, ratingChip;     // quick-filter chips

    // Προτιμησεις (διατηρουνται μεταξυ εκκινησεων)
    private static final File PREFS_FILE =
            new File(System.getProperty("user.home"), "mediahub_prefs.properties");
    private int prefSortIndex = 0;

    // ── Boot ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new Main().setVisible(true);
        });
    }

    public Main() {
        setTitle("MediaHub");
        setSize(1080, 780);
        setMinimumSize(new Dimension(880, 650));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        loadPrefs();
        buildUI();
        autoLoad();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!dirty) {            // καμια μη-αποθηκευμενη αλλαγη -> εξοδος χωρις ερωτηση
                    System.exit(0);
                    return;
                }
                int choice = JOptionPane.showOptionDialog(
                        Main.this,
                        "Εχεις μη αποθηκευμενες αλλαγες. Θελεις να τις αποθηκευσεις πριν βγεις;",
                        "Αποθηκευση πριν την εξοδο",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{"Αποθηκευση & Εξοδος", "Εξοδος χωρις αποθηκευση", "Ακυρωση"},
                        "Αποθηκευση & Εξοδος"
                );
                switch (choice) {
                    case 0 -> { quickSave(); System.exit(0); }
                    case 1 -> System.exit(0);
                }
            }
        });
    }

    private void autoLoad() {
        try {
            // 1) Αν υπαρχει JSON, ειναι η κυρια πηγη
            if (DEFAULT_JSON_FILE.exists()) {
                String text = Files.readString(DEFAULT_JSON_FILE.toPath(), StandardCharsets.UTF_8);
                int loaded = importJsonText(text);
                if (loaded > 0) { refreshList(); showToast("Φορτωθηκαν " + loaded + " εγγραφες!", true); }
                return;
            }
            // 2) Πρωτη φορα μετα την αναβαθμιση: μεταφορα απο το παλιο CSV
            if (DEFAULT_SAVE_FILE.exists()) {
                List<String> lines = Files.readAllLines(DEFAULT_SAVE_FILE.toPath(), StandardCharsets.UTF_8);
                int loaded = importCsvLines(lines);
                if (loaded > 0) {
                    quickSave();   // δημιουργια του JSON αρχειου
                    refreshList();
                    showToast("Μεταφερθηκαν " + loaded + " εγγραφες σε JSON (το CSV κρατηθηκε ως αντιγραφο)", true);
                }
            }
        } catch (Exception ex) {
            System.err.println("AutoLoad failed: " + ex.getMessage());
        } finally {
            dirty = false;   // η φορτωση δεν θεωρειται «αλλαγη»
        }
    }

    // Αυτοματη αποθηκευση -> JSON (UTF-8)
    private void quickSave() {
        try {
            Files.writeString(DEFAULT_JSON_FILE.toPath(), collectionToJson(), StandardCharsets.UTF_8);
            dirty = false;
        } catch (IOException ex) {
            System.err.println("QuickSave failed: " + ex.getMessage());
        }
    }

    // ── Προτιμησεις (dark mode, τελευταιο φιλτρο, ταξινομηση) ──────────────────
    private void loadPrefs() {
        try {
            if (!PREFS_FILE.exists()) return;
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(PREFS_FILE.toPath(), StandardCharsets.UTF_8)) { p.load(r); }
            darkMode      = Boolean.parseBoolean(p.getProperty("darkMode", String.valueOf(darkMode)));
            filterType    = p.getProperty("filterType", filterType);
            prefSortIndex = Integer.parseInt(p.getProperty("sortIndex", "0"));
        } catch (Exception ex) {
            System.err.println("loadPrefs failed: " + ex.getMessage());
        }
    }
    private void savePrefs() {
        try {
            Properties p = new Properties();
            p.setProperty("darkMode", String.valueOf(darkMode));
            p.setProperty("filterType", filterType);
            p.setProperty("sortIndex", String.valueOf(sortBox==null ? prefSortIndex : sortBox.getSelectedIndex()));
            try (Writer w = Files.newBufferedWriter(PREFS_FILE.toPath(), StandardCharsets.UTF_8)) {
                p.store(w, "MediaHub preferences");
            }
        } catch (Exception ex) {
            System.err.println("savePrefs failed: " + ex.getMessage());
        }
    }

    private void buildUI() {
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(bg());
        setContentPane(rootPanel);
        rootPanel.add(buildSidebar(),  BorderLayout.WEST);
        rootPanel.add(buildMainArea(), BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        JPanel side = new ScrollableForm();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(surface());
        side.setBorder(new EmptyBorder(20,20,20,20));

        JLabel title = new JLabel("MediaHub");
        title.setFont(FONT_TITLE); title.setForeground(txt());
        title.setAlignmentX(LEFT_ALIGNMENT);
        side.add(title); side.add(vgap(4));

        JLabel sub = new JLabel("Η προσωπικη σου συλλογη");
        sub.setFont(FONT_SMALL); sub.setForeground(txtMuted());
        sub.setAlignmentX(LEFT_ALIGNMENT);
        side.add(sub); side.add(vgap(15));
        side.add(separator()); side.add(vgap(12));

        // Type selector
        side.add(formLabel("ΤΥΠΟΣ")); side.add(vgap(4));
        typeBox = styledCombo(new String[]{"Ταινια","Σειρα","Βιβλιο","Video Game"});
        typeBox.addActionListener(e -> updateDynamicFields());
        side.add(typeBox); side.add(vgap(10));

        // Title
        side.add(formLabel("ΤΙΤΛΟΣ")); side.add(vgap(4));
        titleField = styledField(""); side.add(titleField); side.add(vgap(10));

        // Year + Genre
        JPanel yg = new JPanel(new GridLayout(1,2,8,0));
        yg.setOpaque(false); yg.setAlignmentX(LEFT_ALIGNMENT);
        yg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        JPanel py = new JPanel(new BorderLayout(0,2)); py.setOpaque(false);
        py.add(formLabel("ΕΤΟΣ"), BorderLayout.NORTH);
        yearField = styledField("2026"); py.add(yearField, BorderLayout.CENTER);
        JPanel pg = new JPanel(new BorderLayout(0,2)); pg.setOpaque(false);
        pg.add(formLabel("ΕΙΔΟΣ"), BorderLayout.NORTH);
        genreField = styledField("π.χ. RPG"); pg.add(genreField, BorderLayout.CENTER);
        yg.add(py); yg.add(pg);
        side.add(yg); side.add(vgap(10));

        // Extra 1
        extraLabel1 = formLabel("ΣΚΗΝΟΘΕΤΗΣ"); side.add(extraLabel1); side.add(vgap(4));
        extraField1 = styledField(""); side.add(extraField1); side.add(vgap(10));

        // Extra 2
        extraLabel2 = formLabel("ΗΘΟΠΟΙΟΙ"); side.add(extraLabel2); side.add(vgap(4));
        extraField2 = styledField(""); side.add(extraField2); side.add(vgap(10));

        // Extra 3 (μονο για βιβλια: εκδοτης)
        extraRow3Panel = new JPanel(new BorderLayout(0,4));
        extraRow3Panel.setOpaque(false); extraRow3Panel.setAlignmentX(LEFT_ALIGNMENT);
        extraRow3Panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        extraLabel3 = formLabel("ΕΚΔΟΤΗΣ");
        extraField3 = styledField("");
        extraRow3Panel.add(extraLabel3, BorderLayout.NORTH);
        extraRow3Panel.add(extraField3, BorderLayout.CENTER);
        side.add(extraRow3Panel); side.add(vgap(10));

        // Date 1
        dateRow1Panel = new JPanel(new BorderLayout(0,2));
        dateRow1Panel.setOpaque(false); dateRow1Panel.setAlignmentX(LEFT_ALIGNMENT);
        dateRow1Panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        dateLabel1 = formLabel("ΗΜΕΡΟΜΗΝΙΑ ΠΑΡΑΚΟΛΟΥΘΗΣΗΣ");
        dateField1 = styledField("");
        dateRow1Panel.add(dateLabel1, BorderLayout.NORTH);
        dateRow1Panel.add(dateWithPicker(dateField1), BorderLayout.CENTER);
        side.add(dateRow1Panel); side.add(vgap(10));

        // Date 2
        dateRow2Panel = new JPanel(new BorderLayout(0,2));
        dateRow2Panel.setOpaque(false); dateRow2Panel.setAlignmentX(LEFT_ALIGNMENT);
        dateRow2Panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        dateLabel2 = formLabel("ΗΜΕΡΟΜΗΝΙΑ ΟΛΟΚΛΗΡΩΣΗΣ");
        dateField2 = styledField("");
        dateRow2Panel.add(dateLabel2, BorderLayout.NORTH);
        dateRow2Panel.add(dateWithPicker(dateField2), BorderLayout.CENTER);
        side.add(dateRow2Panel); side.add(vgap(10));

        // Platinum Panel (Για Video Games)
        platPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        platPanel.setOpaque(false); platPanel.setAlignmentX(LEFT_ALIGNMENT);
        platPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        JPanel pLeft = new JPanel(new BorderLayout(0, 2)); pLeft.setOpaque(false);
        platCheckBox = new JCheckBox("Platinum Trophy;");
        platCheckBox.setFont(FONT_SMALL); platCheckBox.setForeground(txt()); platCheckBox.setOpaque(false);
        pLeft.add(platCheckBox, BorderLayout.CENTER);
        JPanel pRight = new JPanel(new BorderLayout(0, 2)); pRight.setOpaque(false);
        platDateLabel = formLabel("ΗΜ. PLATINUM");
        platDateField = styledField("π.χ. 12/05/2026");
        pRight.add(platDateLabel, BorderLayout.NORTH); pRight.add(dateWithPicker(platDateField), BorderLayout.CENTER);
        platPanel.add(pLeft); platPanel.add(pRight);
        side.add(platPanel); side.add(vgap(10));

        platCheckBox.addActionListener(e -> platDateField.setEnabled(platCheckBox.isSelected()));

        // Rating
        side.add(formLabel("ΒΑΘΜΟΛΟΓΙΑ")); side.add(vgap(4));
        ratingPanel = new RatingPanel(); ratingPanel.setAlignmentX(LEFT_ALIGNMENT);
        side.add(ratingPanel); side.add(vgap(12));

        // Κατασταση + Αγαπημενο
        side.add(formLabel("ΚΑΤΑΣΤΑΣΗ")); side.add(vgap(4));
        statusBox = styledCombo(STATUS_LABELS);
        side.add(statusBox); side.add(vgap(8));
        favCheckBox = new JCheckBox("♥  Αγαπημενο");
        favCheckBox.setFont(FONT_SYM); favCheckBox.setForeground(txt()); favCheckBox.setOpaque(false);
        favCheckBox.setAlignmentX(LEFT_ALIGNMENT);
        side.add(favCheckBox); side.add(vgap(12));

        // Εικονα (προαιρετικο)
        side.add(formLabel("ΕΙΚΟΝΑ (προαιρετικο)")); side.add(vgap(4));
        JPanel imgRow = new JPanel(new BorderLayout(6,0));
        imgRow.setOpaque(false); imgRow.setAlignmentX(LEFT_ALIGNMENT);
        imgRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JButton imagePickBtn = smallBtn("Επιλογη εικονας...");
        imagePickBtn.addActionListener(e -> pickImageForForm());
        imageNameLabel = new JLabel("Καμια εικονα");
        imageNameLabel.setFont(FONT_SMALL); imageNameLabel.setForeground(txtMuted());
        imgRow.add(imagePickBtn, BorderLayout.WEST); imgRow.add(imageNameLabel, BorderLayout.CENTER);
        side.add(imgRow); side.add(vgap(15));

        // Σημειωσεις / σχολια (προαιρετικο)
        side.add(formLabel("ΣΗΜΕΙΩΣΕΙΣ (προαιρετικο)")); side.add(vgap(4));
        notesArea = new JTextArea(3, 20);
        notesArea.setLineWrap(true); notesArea.setWrapStyleWord(true);
        notesArea.setFont(FONT_BODY); notesArea.setBackground(surface2()); notesArea.setForeground(txt());
        notesArea.setCaretColor(txt()); notesArea.setBorder(new EmptyBorder(6,8,6,8));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(new LineBorder(border(),1,true));
        notesScroll.setAlignmentX(LEFT_ALIGNMENT);
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        notesScroll.setPreferredSize(new Dimension(260, 70));
        notesScroll.getViewport().setBackground(surface2());
        side.add(notesScroll); side.add(vgap(15));

        // Ετικετες (tags) — χωρισμενες με κομμα
        side.add(formLabel("ΕΤΙΚΕΤΕΣ (χωρισμενες με κομμα)")); side.add(vgap(4));
        tagsField = styledField("π.χ. rewatch, comfort, classics");
        side.add(tagsField); side.add(vgap(15));

        // Add button
        JButton addBtn = new JButton("+ Καταχωρηση");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addBtn.setBackground(accent()); addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false); addBtn.setBorderPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        addBtn.setAlignmentX(LEFT_ALIGNMENT);
        addBtn.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){ addBtn.setBackground(accentHov()); }
            public void mouseExited(MouseEvent e) { addBtn.setBackground(accent()); }
        });
        addBtn.addActionListener(e -> addItem());
        side.add(addBtn);

        // Toolbar
        JPanel toolbar = new JPanel(new GridLayout(2, 2, 6, 6));
        toolbar.setOpaque(false); toolbar.setAlignmentX(LEFT_ALIGNMENT);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        undoBtn = smallBtn("Αναιρεση");
        undoBtn.setToolTipText("Αναιρεση της τελευταιας ενεργειας (διαγραφη, προσθηκη, επεξεργασια)");
        undoBtn.addActionListener(e -> undo());
        JButton darkBtn = smallBtn(darkMode ? "Light" : "Dark");
        darkBtn.addActionListener(e -> { darkMode=!darkMode; darkBtn.setText(darkMode?"Light":"Dark"); refreshTheme(); savePrefs(); });
        JButton saveBtn = smallBtn("Αποθηκευση");
        saveBtn.addActionListener(e -> saveToFile());
        JButton loadBtn = smallBtn("Φορτωση");
        loadBtn.addActionListener(e -> loadFromFile());
        toolbar.add(undoBtn); toolbar.add(darkBtn); toolbar.add(saveBtn); toolbar.add(loadBtn);
        updateUndoState();

        updateDynamicFields();

        // Η φορμα κυλαει· η μπαρα εργαλειων μενει καρφιτσωμενη κατω.
        JScrollPane sideScroll = new JScrollPane(side,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setBorder(BorderFactory.createEmptyBorder());
        sideScroll.getViewport().setBackground(surface());
        sideScroll.setBackground(surface());
        sideScroll.getVerticalScrollBar().setUnitIncrement(28);

        JPanel toolbarWrap = new JPanel(new BorderLayout());
        toolbarWrap.setBackground(surface());
        toolbarWrap.setBorder(new CompoundBorder(new MatteBorder(1,0,0,0,border()), new EmptyBorder(12,20,16,20)));
        toolbarWrap.add(toolbar, BorderLayout.CENTER);

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(surface());
        container.setBorder(new MatteBorder(0,0,0,1,border()));
        container.setPreferredSize(new Dimension(300, 0));
        container.add(sideScroll, BorderLayout.CENTER);
        container.add(toolbarWrap, BorderLayout.SOUTH);
        return container;
    }

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(bg());

        JPanel topBar = new JPanel(new BorderLayout(12,0));
        topBar.setBackground(surface());
        topBar.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,border()), new EmptyBorder(12,20,12,20)));

        JPanel searchWrap = new JPanel(new BorderLayout(6,0));
        searchWrap.setBackground(surface2());
        searchWrap.setBorder(new CompoundBorder(new LineBorder(border(),1,true), new EmptyBorder(0,10,0,10)));
        searchWrap.setPreferredSize(new Dimension(280,34));
        JLabel si = new JLabel("Αναζητηση...");
        si.setFont(FONT_SMALL); si.setForeground(txtMuted());
        searchField = new JTextField();
        searchField.setFont(FONT_BODY); searchField.setBackground(surface2());
        searchField.setForeground(txt()); searchField.setCaretColor(txt());
        searchField.setBorder(BorderFactory.createEmptyBorder()); searchField.setOpaque(false);
        searchWrap.add(si, BorderLayout.WEST); searchWrap.add(searchField, BorderLayout.CENTER);
        searchField.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e)  { searchQuery=searchField.getText(); refreshList(); }
            public void removeUpdate(DocumentEvent e)  { searchQuery=searchField.getText(); refreshList(); }
            public void changedUpdate(DocumentEvent e) {}
        });

        JPanel sortWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
        sortWrap.setOpaque(false);
        JLabel sl = new JLabel("Ταξινομηση:");
        sl.setFont(FONT_SMALL); sl.setForeground(txtMuted());
        sortBox = styledCombo(new String[]{
                "Τιτλος A-Z","Τιτλος Z-A","Βαθμολογια ↓","Βαθμολογια ↑",
                "Ετος (νεο)","Ετος (παλαιο)","Προσφατα","Αγαπημενα πρωτα"});
        sortBox.setPreferredSize(new Dimension(170,30));
        if (prefSortIndex >= 0 && prefSortIndex < sortBox.getItemCount()) sortBox.setSelectedIndex(prefSortIndex);
        sortBox.addActionListener(e -> { refreshList(); savePrefs(); });
        sortWrap.add(sl); sortWrap.add(sortBox);
        topBar.add(searchWrap, BorderLayout.WEST);
        topBar.add(sortWrap,   BorderLayout.EAST);
        main.add(topBar, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.setBackground(bg()); tabs.setForeground(txt());
        tabs.setBorder(BorderFactory.createEmptyBorder());
        tabs.setFont(FONT_BODY);
        // Καθαρα tabs εναρμονισμενα με το θεμα (το system L&F δεν τα χρωματιζει σωστα)
        tabs.setUI(new BasicTabbedPaneUI(){
            @Override protected void installDefaults(){
                super.installDefaults();
                tabInsets = new Insets(8,16,8,16);
                selectedTabPadInsets = new Insets(0,0,0,0);
                tabAreaInsets = new Insets(4,8,0,0);
                contentBorderInsets = new Insets(0,0,0,0);
            }
            @Override protected void paintTabBackground(Graphics g,int p,int i,int x,int y,int w,int h,boolean sel){
                g.setColor(sel ? surface() : bg()); g.fillRect(x,y,w,h);
                if (sel){ g.setColor(accent()); g.fillRect(x,y+h-3,w,3); }
            }
            @Override protected void paintTabBorder(Graphics g,int p,int i,int x,int y,int w,int h,boolean sel){}
            @Override protected void paintContentBorder(Graphics g,int p,int sel){}
            @Override protected void paintFocusIndicator(Graphics g,int p,Rectangle[] r,int i,Rectangle ir,Rectangle tr,boolean sel){}
            @Override protected int calculateTabAreaHeight(int p,int rc,int mh){ return super.calculateTabAreaHeight(p,rc,mh); }
        });
        tabs.setForeground(txt());

        JPanel collTab = new JPanel(new BorderLayout());
        collTab.setBackground(bg());

        filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT,6,10));
        filterBar.setBackground(bg()); filterBar.setBorder(new EmptyBorder(0,12,0,12));
        String[] filters = {"Ολα","Ταινιες","Σειρες","Βιβλια","Games"};
        String[] fKeys   = {"all","movie","series","book","game"};
        for (int i=0;i<filters.length;i++) {
            final String key=fKeys[i];
            ChipButton chip = new ChipButton(filters[i], key.equals(filterType));
            chip.addActionListener(e -> {
                filterType=key;
                for (Component c : filterBar.getComponents())
                    if (c instanceof ChipButton cb && cb!=favChip && cb!=ratingChip)
                        cb.setChipSelected(cb.key.equals(key));
                filterBar.repaint(); refreshList(); savePrefs();
            });
            filterBar.add(chip);
        }

        // Διαχωριστικο + quick filters
        JLabel sep = new JLabel("  |  "); sep.setForeground(border()); filterBar.add(sep);

        favChip = new ChipButton("♥ Αγαπημενα", filterFavorite);
        favChip.setFont(FONT_SYM);
        favChip.addActionListener(e -> {
            filterFavorite = !filterFavorite;
            favChip.setChipSelected(filterFavorite);
            refreshList();
        });
        filterBar.add(favChip);

        ratingChip = new ChipButton("4★+", filterMinRating>0);
        ratingChip.setFont(FONT_SYM);
        ratingChip.addActionListener(e -> {
            filterMinRating = (filterMinRating==0) ? 4 : 0;
            ratingChip.setChipSelected(filterMinRating>0);
            refreshList();
        });
        filterBar.add(ratingChip);

        statusFilterBox = styledCombo(new String[]{
                "Ολες οι καταστασεις","Εκκρεμεί","Σε εξέλιξη","Ολοκληρωμένο","Εγκαταλείφθηκε"});
        statusFilterBox.setPreferredSize(new Dimension(160,28));
        statusFilterBox.setMaximumSize(new Dimension(160,28));
        for (int sj=0; sj<STATUS_KEYS.length; sj++) if (STATUS_KEYS[sj].equals(filterStatus)) statusFilterBox.setSelectedIndex(sj);
        statusFilterBox.addActionListener(e -> {
            int sIdx = statusFilterBox.getSelectedIndex();
            filterStatus = sIdx<=0 ? "" : STATUS_KEYS[sIdx];
            refreshList();
        });
        filterBar.add(statusFilterBox);

        countLabel = new JLabel("0 αποτελεσματα");
        countLabel.setFont(FONT_SMALL); countLabel.setForeground(txtMuted());
        JPanel filterRow = new JPanel(new BorderLayout()); filterRow.setOpaque(false);
        JPanel filterRight = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,10));
        filterRight.setOpaque(false); filterRight.add(countLabel);
        filterRow.add(filterBar, BorderLayout.WEST); filterRow.add(filterRight, BorderLayout.EAST);
        collTab.add(filterRow, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(bg()); listPanel.setBorder(new EmptyBorder(4,16,16,16));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.setBackground(bg());
        scroll.getVerticalScrollBar().setUnitIncrement(48);
        scroll.getVerticalScrollBar().setBlockIncrement(200);
        collTab.add(scroll, BorderLayout.CENTER);
        tabs.addTab("Συλλογη", collTab);

        statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBackground(bg()); statsPanel.setBorder(new EmptyBorder(20,20,20,20));
        JScrollPane statsScroll = new JScrollPane(statsPanel);
        statsScroll.setBorder(BorderFactory.createEmptyBorder()); statsScroll.setBackground(bg());
        statsScroll.getVerticalScrollBar().setUnitIncrement(40);
        statsScroll.getVerticalScrollBar().setBlockIncrement(200);
        tabs.addTab("Στατιστικα", statsScroll);

        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex()==1) refreshStats(); });
        main.add(tabs, BorderLayout.CENTER);
        refreshList();
        return main;
    }

    private void updateDynamicFields() {
        String t = (String) typeBox.getSelectedItem();
        platPanel.setVisible(false);
        extraRow3Panel.setVisible(false);
        switch (t) {
            case "Ταινια" -> {
                extraLabel1.setText("ΣΚΗΝΟΘΕΤΗΣ");    extraLabel2.setText("ΗΘΟΠΟΙΟΙ");
                dateLabel1.setText("ΗΜ. ΠΑΡΑΚΟΛΟΥΘΗΣΗΣ");
                dateRow1Panel.setVisible(true); dateRow2Panel.setVisible(false);
            }
            case "Σειρα" -> {
                extraLabel1.setText("ΣΕΖΟΝ");          extraLabel2.setText("ΗΘΟΠΟΙΟΙ");
                dateLabel1.setText("ΗΜ. ΕΝΑΡΞΗΣ");    dateLabel2.setText("ΗΜ. ΟΛΟΚΛΗΡΩΣΗΣ");
                dateRow1Panel.setVisible(true); dateRow2Panel.setVisible(true);
            }
            case "Βιβλιο" -> {
                extraLabel1.setText("ΣΥΓΓΡΑΦΕΑΣ");     extraLabel2.setText("ΣΕΛΙΔΕΣ");
                extraLabel3.setText("ΕΚΔΟΤΗΣ");        extraRow3Panel.setVisible(true);
                dateLabel1.setText("ΗΜ. ΕΝΑΡΞΗΣ");    dateLabel2.setText("ΗΜ. ΟΛΟΚΛΗΡΩΣΗΣ");
                dateRow1Panel.setVisible(true); dateRow2Panel.setVisible(true);
            }
            case "Video Game" -> {
                extraLabel1.setText("DEVELOPER");       extraLabel2.setText("PLATFORM");
                dateLabel1.setText("ΗΜ. ΕΝΑΡΞΗΣ");    dateLabel2.setText("ΗΜ. ΟΛΟΚΛΗΡΩΣΗΣ");
                dateRow1Panel.setVisible(true); dateRow2Panel.setVisible(true);
                platPanel.setVisible(true);
                platDateField.setEnabled(platCheckBox.isSelected());
            }
        }
        dateField1.setText(""); dateField2.setText(""); platDateField.setText(""); platCheckBox.setSelected(false);
        extraField3.setText("");
        dateRow1Panel.revalidate(); dateRow2Panel.revalidate(); platPanel.revalidate(); extraRow3Panel.revalidate();
    }

    private void addItem() {
        try {
            String type   = (String) typeBox.getSelectedItem();
            String title  = titleField.getText().trim();
            String yearTx = yearField.getText().trim();
            String genre  = genreField.getText().trim();
            String extra1 = extraField1.getText().trim();
            String extra2 = extraField2.getText().trim();
            String date1  = dateField1.getText().trim();
            String date2  = dateField2.getText().trim();

            if (title.isEmpty()||yearTx.isEmpty()||genre.isEmpty()||extra1.isEmpty()||extra2.isEmpty()) {
                showToast("Συμπληρωσε ολα τα υποχρεωτικα πεδια!", false); return;
            }
            int year = Integer.parseInt(yearTx);

            // Έλεγχος Διπλοτύπων (Title + Year + Type)
            boolean exists = collection.stream().anyMatch(i ->
                    i.title.equalsIgnoreCase(title) && i.year == year && typeLabel(i).equalsIgnoreCase(type == "Video Game" ? "Game" : type)
            );
            if (exists) {
                int choice = JOptionPane.showConfirmDialog(this, "Αυτός ο τίτλος υπάρχει ήδη για το έτος αυτό. Θέλεις να τον προσθέσεις ξανά;", "Προειδοποίηση Διπλοτύπου", JOptionPane.YES_NO_OPTION);
                if (choice != JOptionPane.YES_OPTION) return;
            }

            MediaItem item;
            switch (type) {
                case "Ταινια" -> { Movie m = new Movie(title,year,genre,extra1,extra2); m.watchDate=date1; item=m; }
                case "Σειρα" -> { Series s = new Series(title,year,genre,Integer.parseInt(extra1),extra2); s.startDate=date1; s.endDate=date2; item=s; }
                case "Βιβλιο" -> { Book b = new Book(title,year,genre,extra1,Integer.parseInt(extra2)); b.publisher=extraField3.getText().trim(); b.startDate=date1; b.endDate=date2; item=b; }
                case "Video Game" -> {
                    VideoGame g = new VideoGame(title,year,genre,extra1,extra2);
                    g.startDate=date1; g.endDate=date2;
                    g.hasPlatinum = platCheckBox.isSelected();
                    g.platinumDate = g.hasPlatinum ? platDateField.getText().trim() : "";
                    item=g;
                }
                default -> { showToast("Αγνωστος τυπος!", false); return; }
            }
            item.rating = ratingPanel.getRating();
            item.imagePath = persistImage(pendingImagePath);
            item.notes = notesArea.getText().trim();
            item.status = statusKeyFromLabel((String) statusBox.getSelectedItem());
            item.favorite = favCheckBox.isSelected();
            item.tags = parseTags(tagsField.getText());
            item.added = System.currentTimeMillis();
            applyAutoComplete(item);
            pushUndo();
            collection.add(item);
            clearForm(); refreshList();
            showToast("\"" + title + "\" προστεθηκε!", true);
        } catch (NumberFormatException ex) {
            showToast("Ετος / σεζον / σελιδες: μονο αριθμοι!", false);
        }
    }

    // Αν υπαρχει ημερομηνια ολοκληρωσης (ταινια: παρακολουθηση), η κατασταση γινεται «Ολοκληρωμενο».
    private void applyAutoComplete(MediaItem i) {
        String end = "";
        if (i instanceof Movie m)        end = m.watchDate;
        else if (i instanceof Series s)  end = s.endDate;
        else if (i instanceof Book b)    end = b.endDate;
        else if (i instanceof VideoGame g) end = g.endDate;
        if (end != null && !end.trim().isEmpty()) i.status = "completed";
    }

    private void clearForm() {
        titleField.setText(""); yearField.setText(""); genreField.setText("");
        extraField1.setText(""); extraField2.setText(""); if (extraField3!=null) extraField3.setText("");
        dateField1.setText(""); dateField2.setText(""); platDateField.setText(""); platCheckBox.setSelected(false);
        pendingImagePath = "";
        if (imageNameLabel != null) { imageNameLabel.setText("Καμια εικονα"); imageNameLabel.setForeground(txtMuted()); }
        if (notesArea != null) notesArea.setText("");
        if (statusBox != null) statusBox.setSelectedIndex(0);
        if (favCheckBox != null) favCheckBox.setSelected(false);
        if (tagsField != null) tagsField.setText("");
        ratingPanel.reset(); updateDynamicFields();
    }

    private Color typeCol(MediaItem i) {
        if (i instanceof Movie)     return movieCol();
        if (i instanceof Series)    return seriesCol();
        if (i instanceof Book)      return bookCol();
        if (i instanceof VideoGame) return gameCol();
        return accent();
    }
    private Color typeBg(MediaItem i) {
        if (i instanceof Movie)     return movieBg();
        if (i instanceof Series)    return seriesBg();
        if (i instanceof Book)      return bookBg();
        if (i instanceof VideoGame) return gameBg();
        return surface2();
    }
    private String typeLabel(MediaItem i) {
        if (i instanceof Movie)     return "Ταινια";
        if (i instanceof Series)    return "Σειρα";
        if (i instanceof Book)      return "Βιβλιο";
        if (i instanceof VideoGame) return "Game";
        return "?";
    }

    // Read-only παραθυρο προβολης μιας καταχωρησης
    private void openViewDialog(MediaItem item) {
        JDialog dlg = new JDialog(this, "Προβολη", true);
        JPanel root = new ViewPane(); root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(surface()); root.setBorder(new EmptyBorder(24,28,24,28));

        // ── Κεφαλιδα: εικονα + τιτλος ──
        JPanel header = new JPanel(new BorderLayout(16,0)); header.setOpaque(false); header.setAlignmentX(LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        ImageIcon thumb = loadThumb(item.imagePath, 92, 130);
        if (thumb != null) {
            JLabel img=new JLabel(thumb); img.setVerticalAlignment(SwingConstants.TOP);
            img.setBorder(new LineBorder(border(),1,true)); header.add(img, BorderLayout.WEST);
            img.setToolTipText("Κλικ για μεγεθυνση");
            img.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            img.addMouseListener(new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent e){ showImageFull(item.imagePath, item.title); }
            });
        }
        JPanel tb = new JPanel(); tb.setOpaque(false); tb.setLayout(new BoxLayout(tb, BoxLayout.Y_AXIS));
        JLabel badge = new JLabel(typeLabel(item)); badge.setFont(FONT_LABEL); badge.setOpaque(true);
        badge.setForeground(Color.WHITE); badge.setBackground(typeCol(item)); badge.setBorder(new EmptyBorder(2,8,2,8));
        badge.setAlignmentX(LEFT_ALIGNMENT);
        JLabel title = new JLabel("<html><div style='width:400px'>"+htmlEsc(item.title)+"</div></html>");
        title.setFont(new Font("Segoe UI",Font.BOLD,18)); title.setForeground(txt()); title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = new JLabel(item.year+"   ·   "+item.genre); sub.setFont(FONT_BODY); sub.setForeground(txtMuted()); sub.setAlignmentX(LEFT_ALIGNMENT);
        JLabel stars = new JLabel(starsStr(item.rating)); stars.setFont(FONT_SYM); stars.setForeground(starOn()); stars.setAlignmentX(LEFT_ALIGNMENT);
        tb.add(badge); tb.add(vgap(8)); tb.add(title); tb.add(vgap(5)); tb.add(sub); tb.add(vgap(9)); tb.add(stars);
        JPanel sf = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0)); sf.setOpaque(false); sf.setAlignmentX(LEFT_ALIGNMENT);
        sf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        if (!item.status.isEmpty()) {
            JLabel pill=new JLabel(statusLabel(item.status)); pill.setFont(FONT_LABEL); pill.setOpaque(true);
            pill.setForeground(Color.WHITE); pill.setBackground(statusColor(item.status)); pill.setBorder(new EmptyBorder(2,8,2,8));
            sf.add(pill);
        }
        if (item.favorite) {
            JLabel hv=new JLabel("Αγαπημενο", new GlyphIcon(GlyphIcon.Kind.HEART,new Color(229,73,109),14,14), SwingConstants.LEFT);
            hv.setFont(FONT_SMALL); hv.setForeground(new Color(229,73,109)); hv.setIconTextGap(4); sf.add(hv);
        }
        tb.add(vgap(9)); tb.add(sf);
        header.add(tb, BorderLayout.CENTER);
        root.add(header); root.add(vgap(20)); root.add(viewDivider()); root.add(vgap(16));

        // ── Λεπτομερειες ανα τυπο (στοιχισμενες γραμμες) ──
        if (item instanceof Movie m) {
            root.add(infoLine("Σκηνοθετης", m.director));
            root.add(infoLine("Ηθοποιοι", m.actors));
            root.add(infoLine("Παρακολουθηση", m.watchDate));
        } else if (item instanceof Series s) {
            root.add(infoLine("Σεζον", String.valueOf(s.seasons)));
            root.add(infoLine("Ηθοποιοι", s.actors));
            root.add(infoLine("Παρακολουθηση", dateRange(s.startDate, s.endDate)));
        } else if (item instanceof Book b) {
            root.add(infoLine("Συγγραφεας", b.author));
            root.add(infoLine("Εκδοτης", b.publisher));
            root.add(infoLine("Σελιδες", String.valueOf(b.pages)));
            root.add(infoLine("Αναγνωση", dateRange(b.startDate, b.endDate)));
        } else if (item instanceof VideoGame g) {
            root.add(infoLine("Developer", g.developer));
            root.add(infoLine("Platform", g.platform));
            root.add(infoLine("Παιξιμο", dateRange(g.startDate, g.endDate)));
            if (g.hasPlatinum) root.add(infoLine("Platinum", "Ναι" + (g.platinumDate.isEmpty()?"":"  ("+g.platinumDate+")")));
        }
        if (item.tags != null && !item.tags.isEmpty())
            root.add(infoLine("Ετικετες", "#" + String.join("   #", item.tags)));

        // ── Σημειωσεις ──
        if (item.notes != null && !item.notes.isEmpty()) {
            root.add(vgap(4)); root.add(viewDivider()); root.add(vgap(12));
            JLabel nh = new JLabel("ΣΗΜΕΙΩΣΕΙΣ / ΣΧΟΛΙΑ"); nh.setFont(FONT_LABEL); nh.setForeground(txtMuted()); nh.setAlignmentX(LEFT_ALIGNMENT);
            root.add(nh); root.add(vgap(8));
            JTextArea na = new JTextArea(item.notes); na.setEditable(false); na.setLineWrap(true); na.setWrapStyleWord(true);
            na.setFont(FONT_BODY); na.setBackground(surface2()); na.setForeground(txt()); na.setBorder(new EmptyBorder(10,12,10,12));
            JScrollPane nsp = new JScrollPane(na); nsp.setBorder(new LineBorder(border(),1,true));
            nsp.setAlignmentX(LEFT_ALIGNMENT); nsp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
            nsp.setPreferredSize(new Dimension(500, 120)); nsp.getViewport().setBackground(surface2());
            nsp.getVerticalScrollBar().setUnitIncrement(48);
            root.add(nsp);
        }
        root.add(vgap(24));

        // ── Κουμπια ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); btnRow.setOpaque(false); btnRow.setAlignmentX(LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JButton editB = new JButton("Επεξεργασια"); editB.setFont(new Font("Segoe UI",Font.BOLD,12));
        editB.setBackground(accent()); editB.setForeground(Color.WHITE); editB.setBorderPainted(false); editB.setFocusPainted(false);
        editB.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editB.addActionListener(e -> { dlg.dispose(); openEditDialog(item); });
        JButton closeB = smallBtn("Κλεισιμο"); closeB.addActionListener(e -> dlg.dispose());
        btnRow.add(closeB); btnRow.add(editB);
        root.add(btnRow);

        JScrollPane sp = new JScrollPane(root,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(BorderFactory.createEmptyBorder()); sp.setBackground(surface());
        sp.getVerticalScrollBar().setUnitIncrement(48);
        sp.getVerticalScrollBar().setBlockIncrement(200);
        dlg.setContentPane(sp);
        dlg.setSize(620, 660); dlg.setLocationRelativeTo(this); dlg.setVisible(true);
    }

    private JComponent viewDivider() {
        JPanel d = new JPanel(); d.setBackground(border());
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        d.setPreferredSize(new Dimension(10,1)); d.setAlignmentX(LEFT_ALIGNMENT);
        return d;
    }
    private String dateRange(String start, String end) {
        boolean hs = start!=null && !start.isEmpty(), he = end!=null && !end.isEmpty();
        if (!hs && !he) return "";
        if (hs && !he)  return start + "   →   σε εξελιξη";
        if (!hs)        return "—   →   " + end;
        return start + "   →   " + end;
    }
    private JPanel infoLine(String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            JPanel e = new JPanel(); e.setOpaque(false);
            e.setMaximumSize(new Dimension(0,0)); e.setPreferredSize(new Dimension(0,0));
            return e;
        }
        JPanel p = new JPanel(new BorderLayout(12,0)); p.setOpaque(false); p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(0,0,11,0));
        JLabel l = new JLabel(label); l.setFont(FONT_SMALL); l.setForeground(txtMuted());
        l.setPreferredSize(new Dimension(120, 20)); l.setVerticalAlignment(SwingConstants.TOP);
        JLabel v = new JLabel("<html><div style='width:380px'>"+htmlEsc(value)+"</div></html>");
        v.setFont(FONT_BODY); v.setForeground(txt());
        p.add(l, BorderLayout.WEST); p.add(v, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private void openEditDialog(MediaItem item) {
        JDialog dlg = new JDialog(this, "Επεξεργασια", true);
        dlg.setSize(450, 770); dlg.setLocationRelativeTo(this); dlg.setResizable(false);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(surface()); root.setBorder(new EmptyBorder(20,24,20,24));

        JLabel header = new JLabel("Επεξεργασια: " + item.title);
        header.setFont(new Font("Segoe UI",Font.BOLD,15));
        header.setForeground(txt()); header.setAlignmentX(LEFT_ALIGNMENT);
        root.add(header); root.add(vgap(16));

        JTextField eTitleF = dialogField(item.title);
        JTextField eYearF  = dialogField(String.valueOf(item.year));
        JTextField eGenreF = dialogField(item.genre);
        root.add(dialogRow("ΤΙΤΛΟΣ", eTitleF)); root.add(vgap(10));
        root.add(dialogRow("ΕΤΟΣ",   eYearF));  root.add(vgap(10));
        root.add(dialogRow("ΕΙΔΟΣ",  eGenreF)); root.add(vgap(10));

        JTextField eEx1, eEx2, eD1, eD2 = null, ePlatDate = null, ePub = null;
        JCheckBox ePlatCheck = null;

        if (item instanceof Movie m) {
            eEx1=dialogField(m.director); eEx2=dialogField(m.actors); eD1=dialogField(m.watchDate);
            root.add(dialogRow("ΣΚΗΝΟΘΕΤΗΣ",      eEx1)); root.add(vgap(10));
            root.add(dialogRow("ΗΘΟΠΟΙΟΙ",         eEx2)); root.add(vgap(10));
            root.add(dialogDateRow("ΗΜ. ΠΑΡΑΚΟΛ.", eD1));  root.add(vgap(10));
        } else if (item instanceof Series s) {
            eEx1=dialogField(String.valueOf(s.seasons)); eEx2=dialogField(s.actors);
            eD1=dialogField(s.startDate); eD2=dialogField(s.endDate);
            root.add(dialogRow("ΣΕΖΟΝ",            eEx1)); root.add(vgap(10));
            root.add(dialogRow("ΗΘΟΠΟΙΟΙ",         eEx2)); root.add(vgap(10));
            root.add(dialogDateRow("ΗΜ. ΕΝΑΡΞΗΣ",      eD1));  root.add(vgap(10));
            root.add(dialogDateRow("ΗΜ. ΟΛΟΚΛΗΡΩΣΗΣ", eD2));  root.add(vgap(10));
        } else if (item instanceof Book b) {
            eEx1=dialogField(b.author); eEx2=dialogField(String.valueOf(b.pages)); ePub=dialogField(b.publisher);
            eD1=dialogField(b.startDate); eD2=dialogField(b.endDate);
            root.add(dialogRow("ΣΥΓΓΡΑΦΕΑΣ",       eEx1)); root.add(vgap(10));
            root.add(dialogRow("ΣΕΛΙΔΕΣ",          eEx2)); root.add(vgap(10));
            root.add(dialogRow("ΕΚΔΟΤΗΣ",          ePub)); root.add(vgap(10));
            root.add(dialogDateRow("ΗΜ. ΕΝΑΡΞΗΣ",      eD1));  root.add(vgap(10));
            root.add(dialogDateRow("ΗΜ. ΟΛΟΚΛΗΡΩΣΗΣ", eD2));  root.add(vgap(10));
        } else {
            VideoGame g = (VideoGame) item;
            eEx1=dialogField(g.developer); eEx2=dialogField(g.platform);
            eD1=dialogField(g.startDate); eD2=dialogField(g.endDate);
            root.add(dialogRow("DEVELOPER",        eEx1)); root.add(vgap(10));
            root.add(dialogRow("PLATFORM",         eEx2)); root.add(vgap(10));
            root.add(dialogDateRow("ΗΜ. ΕΝΑΡΞΗΣ",      eD1));  root.add(vgap(10));
            root.add(dialogDateRow("ΗΜ. ΟΛΟΚΛΗΡΩΣΗΣ", eD2));  root.add(vgap(10));

            JPanel pRow = new JPanel(new GridLayout(1,2,6,0)); pRow.setOpaque(false); pRow.setAlignmentX(LEFT_ALIGNMENT);
            ePlatCheck = new JCheckBox("Platinum Trophy;", g.hasPlatinum); ePlatCheck.setFont(FONT_SMALL); ePlatCheck.setForeground(txt()); ePlatCheck.setOpaque(false);
            ePlatDate = dialogField(g.platinumDate); ePlatDate.setEnabled(g.hasPlatinum);
            final JTextField finalEPlatDate = ePlatDate;
            ePlatCheck.addActionListener(e -> finalEPlatDate.setEnabled(((JCheckBox)e.getSource()).isSelected()));
            pRow.add(ePlatCheck); pRow.add(dateWithPicker(ePlatDate));
            root.add(pRow); root.add(vgap(10));
        }

        root.add(formLabel("ΒΑΘΜΟΛΟΓΙΑ")); root.add(vgap(6));
        RatingPanel eRating = new RatingPanel(); eRating.setRating(item.rating);
        eRating.setAlignmentX(LEFT_ALIGNMENT); root.add(eRating); root.add(vgap(16));

        // Εικονα
        root.add(formLabel("ΕΙΚΟΝΑ")); root.add(vgap(6));
        JPanel imgEditRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        imgEditRow.setOpaque(false); imgEditRow.setAlignmentX(LEFT_ALIGNMENT);
        imgEditRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        final String[] editImg = { item.imagePath };
        JLabel preview = new JLabel();
        preview.setPreferredSize(new Dimension(50, 64));
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        Runnable refreshPreview = () -> {
            ImageIcon ic = loadThumb(editImg[0], 46, 60);
            if (ic != null) { preview.setIcon(ic); preview.setText(""); }
            else { preview.setIcon(null); preview.setText("Καμια"); preview.setFont(FONT_SMALL); preview.setForeground(txtMuted()); }
        };
        refreshPreview.run();
        JButton chooseImg = smallBtn("Επιλογη...");
        chooseImg.addActionListener(ev -> { String p = chooseImageFile(); if (p != null) { editImg[0] = p; refreshPreview.run(); } });
        JButton removeImg = smallBtn("Αφαιρεση");
        removeImg.addActionListener(ev -> { editImg[0] = ""; refreshPreview.run(); });
        imgEditRow.add(preview); imgEditRow.add(chooseImg); imgEditRow.add(removeImg);
        root.add(imgEditRow); root.add(vgap(20));

        // Σημειωσεις / σχολια
        root.add(formLabel("ΣΗΜΕΙΩΣΕΙΣ / ΣΧΟΛΙΑ")); root.add(vgap(6));
        JTextArea eNotes = new JTextArea(item.notes, 5, 20);
        eNotes.setLineWrap(true); eNotes.setWrapStyleWord(true);
        eNotes.setFont(FONT_BODY); eNotes.setBackground(surface2()); eNotes.setForeground(txt());
        eNotes.setCaretColor(txt()); eNotes.setBorder(new EmptyBorder(6,8,6,8));
        JScrollPane eNotesScroll = new JScrollPane(eNotes);
        eNotesScroll.setBorder(new LineBorder(border(),1,true));
        eNotesScroll.setAlignmentX(LEFT_ALIGNMENT);
        eNotesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        eNotesScroll.setPreferredSize(new Dimension(390, 110));
        eNotesScroll.getViewport().setBackground(surface2());
        root.add(eNotesScroll); root.add(vgap(20));

        // Κατασταση + Αγαπημενο
        root.add(formLabel("ΚΑΤΑΣΤΑΣΗ")); root.add(vgap(6));
        JComboBox<String> eStatus = styledCombo(STATUS_LABELS);
        eStatus.setSelectedItem(statusLabel(item.status));
        eStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        root.add(eStatus); root.add(vgap(10));
        JCheckBox eFav = new JCheckBox("♥  Αγαπημενο", item.favorite);
        eFav.setFont(FONT_SYM); eFav.setForeground(txt()); eFav.setOpaque(false); eFav.setAlignmentX(LEFT_ALIGNMENT);
        root.add(eFav); root.add(vgap(16));

        // Ετικετες
        root.add(formLabel("ΕΤΙΚΕΤΕΣ (χωρισμενες με κομμα)")); root.add(vgap(6));
        JTextField eTags = dialogField(tagsToString(item.tags));
        root.add(eTags); root.add(vgap(20));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        btnRow.setOpaque(false); btnRow.setAlignmentX(LEFT_ALIGNMENT);
        JButton cancelBtn = smallBtn("Ακυρωση"); cancelBtn.addActionListener(e -> dlg.dispose());
        JButton saveBtn2 = new JButton("Αποθηκευση");
        saveBtn2.setFont(new Font("Segoe UI",Font.BOLD,13)); saveBtn2.setBackground(accent()); saveBtn2.setForeground(Color.WHITE);
        saveBtn2.setFocusPainted(false); saveBtn2.setBorderPainted(false); saveBtn2.setBorder(new EmptyBorder(6,16,6,16));

        final JTextField fEx1=eEx1, fEx2=eEx2, fD1=eD1, fD2=eD2, fPlatDate=ePlatDate, fPub=ePub;
        final JCheckBox fPlatCheck=ePlatCheck;
        saveBtn2.addActionListener(e -> {
            try {
                String nt=eTitleF.getText().trim(), ng=eGenreF.getText().trim();
                String ne1=fEx1.getText().trim(), ne2=fEx2.getText().trim();
                String nd1=fD1.getText().trim();
                int ny=Integer.parseInt(eYearF.getText().trim());
                if (nt.isEmpty()||ng.isEmpty()||ne1.isEmpty()||ne2.isEmpty()) {
                    showToast("Ολα τα πεδια ειναι υποχρεωτικα!", false); return;
                }
                pushUndo();
                item.title=nt; item.year=ny; item.genre=ng; item.rating=eRating.getRating();
                item.imagePath = persistImage(editImg[0]);
                item.notes = eNotes.getText().trim();
                item.status = statusKeyFromLabel((String) eStatus.getSelectedItem());
                item.favorite = eFav.isSelected();
                item.tags = parseTags(eTags.getText());
                if (item instanceof Movie m)       { m.director=ne1; m.actors=ne2; m.watchDate=nd1; }
                else if (item instanceof Series s) { s.seasons=Integer.parseInt(ne1); s.actors=ne2; s.startDate=nd1; s.endDate=fD2!=null?fD2.getText().trim():""; }
                else if (item instanceof Book b)   { b.author=ne1; b.pages=Integer.parseInt(ne2); b.publisher=fPub!=null?fPub.getText().trim():""; b.startDate=nd1; b.endDate=fD2!=null?fD2.getText().trim():""; }
                else if (item instanceof VideoGame g){
                    g.developer=ne1; g.platform=ne2; g.startDate=nd1; g.endDate=fD2!=null?fD2.getText().trim():"";
                    if(fPlatCheck != null) { g.hasPlatinum = fPlatCheck.isSelected(); g.platinumDate = g.hasPlatinum ? fPlatDate.getText().trim() : ""; }
                }
                applyAutoComplete(item);
                refreshList(); showToast("\""+nt+"\" ενημερωθηκε!", true); dlg.dispose();
            } catch (NumberFormatException ex) { showToast("Ετος / σεζον / σελιδες: μονο αριθμοι!", false); }
        });
        btnRow.add(cancelBtn); btnRow.add(saveBtn2); root.add(btnRow);

        JScrollPane sp = new JScrollPane(root); sp.setBorder(BorderFactory.createEmptyBorder()); sp.setBackground(surface());
        sp.getVerticalScrollBar().setUnitIncrement(48);
        sp.getVerticalScrollBar().setBlockIncrement(200);
        dlg.setContentPane(sp); dlg.setVisible(true);
    }

    private JPanel dialogRow(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0,4)); p.setOpaque(false); p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        p.add(formLabel(label), BorderLayout.NORTH); p.add(field, BorderLayout.CENTER);
        return p;
    }
    private JPanel dialogDateRow(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0,4)); p.setOpaque(false); p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        p.add(formLabel(label), BorderLayout.NORTH);
        p.add(dateWithPicker(field), BorderLayout.CENTER);
        return p;
    }
    private JTextField dialogField(String val) {
        JTextField f = new JTextField(val); f.setFont(FONT_BODY);
        f.setBackground(surface2()); f.setForeground(txt()); f.setCaretColor(txt());
        f.setBorder(new CompoundBorder(new LineBorder(border(),1,true), new EmptyBorder(5,8,5,8)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,34)); f.setAlignmentX(LEFT_ALIGNMENT); return f;
    }

    private void refreshList() {
        List<MediaItem> filtered = collection.stream()
                .filter(i -> switch(filterType) {
                    case "movie"  -> i instanceof Movie;
                    case "series" -> i instanceof Series;
                    case "book"   -> i instanceof Book;
                    case "game"   -> i instanceof VideoGame;
                    default       -> true;
                })
                .filter(i -> !filterFavorite || i.favorite)
                .filter(i -> i.rating >= filterMinRating)
                .filter(i -> filterStatus.isEmpty() || filterStatus.equals(i.status))
                .filter(i -> {
                    if (searchQuery.isEmpty()) return true;
                    String q = searchQuery.toLowerCase();
                    boolean baseMatch = i.title.toLowerCase().contains(q)
                            || i.genre.toLowerCase().contains(q)
                            || i.notes.toLowerCase().contains(q)
                            || i.tags.stream().anyMatch(t -> t.toLowerCase().contains(q));
                    if (i instanceof Movie m) return baseMatch || m.director.toLowerCase().contains(q) || m.actors.toLowerCase().contains(q);
                    if (i instanceof Series s) return baseMatch || s.actors.toLowerCase().contains(q);
                    if (i instanceof Book b) return baseMatch || b.author.toLowerCase().contains(q);
                    if (i instanceof VideoGame g) return baseMatch || g.developer.toLowerCase().contains(q) || g.platform.toLowerCase().contains(q);
                    return baseMatch;
                })
                .sorted(comparator())
                .collect(Collectors.toList());

        listPanel.removeAll();
        if (filtered.isEmpty()) {
            JPanel empty = new JPanel(new BorderLayout()); empty.setOpaque(false); empty.setBorder(new EmptyBorder(60,0,0,0));
            JLabel lbl = new JLabel(collection.isEmpty() ? "Δεν εχεις προσθεσει κατι ακομα" : "Δεν βρεθηκαν αποτελεσματα", SwingConstants.CENTER);
            lbl.setFont(FONT_BODY); lbl.setForeground(txtMuted()); empty.add(lbl, BorderLayout.CENTER); listPanel.add(empty);
        } else {
            for (MediaItem item : filtered) { listPanel.add(makeCard(item)); listPanel.add(vgap(8)); }
        }
        countLabel.setText(filtered.size() + " αποτελεσματα");
        listPanel.revalidate(); listPanel.repaint();
        if (tabs != null && statsPanel != null && tabs.getSelectedIndex()==1) refreshStats();
    }

    private Comparator<MediaItem> comparator() {
        int idx = sortBox==null ? 0 : sortBox.getSelectedIndex();
        return switch(idx) {
            case 1  -> Comparator.comparing((MediaItem i)->i.title.toLowerCase()).reversed();
            case 2  -> Comparator.comparingInt((MediaItem i)->i.rating).reversed()
                    .thenComparing(i->i.title.toLowerCase());
            case 3  -> Comparator.comparingInt((MediaItem i)->i.rating)
                    .thenComparing(i->i.title.toLowerCase());
            case 4  -> Comparator.comparingInt((MediaItem i)->i.year).reversed()
                    .thenComparing(i->i.title.toLowerCase());
            case 5  -> Comparator.comparingInt((MediaItem i)->i.year)
                    .thenComparing(i->i.title.toLowerCase());
            case 6  -> Comparator.comparingLong((MediaItem i)->i.added).reversed()
                    .thenComparing(i->i.title.toLowerCase());
            case 7  -> Comparator.comparing((MediaItem i)->!i.favorite)   // αγαπημενα πρωτα
                    .thenComparing(i->i.title.toLowerCase());
            default -> Comparator.comparing(i->i.title.toLowerCase());
        };
    }

    private JPanel makeCard(MediaItem item) {
        Color col   = typeCol(item); Color colBg = typeBg(item); String tStr = typeLabel(item);
        String detail, dateInfo="";
        if (item instanceof Movie m) {
            detail="Σκηνοθετης: "+m.director+"  |  Ηθοποιοι: "+m.actors;
            if (!m.watchDate.isEmpty()) dateInfo="Παρακολ.: "+m.watchDate;
        } else if (item instanceof Series s) {
            detail="Σεζον: "+s.seasons+"  |  Ηθοποιοι: "+s.actors;
            if (!s.startDate.isEmpty()||!s.endDate.isEmpty())
                dateInfo="Παρακολ.: "+(s.startDate.isEmpty()?"-":s.startDate)+" -> "+(s.endDate.isEmpty()?"σε εξελιξη":s.endDate);
        } else if (item instanceof Book b) {
            detail="Συγγραφεας: "+b.author+"  |  Σελιδες: "+b.pages;
            if (!b.startDate.isEmpty()||!b.endDate.isEmpty())
                dateInfo="Αναγνωση: "+(b.startDate.isEmpty()?"-":b.startDate)+" -> "+(b.endDate.isEmpty()?"σε εξελιξη":b.endDate);
        } else {
            VideoGame g=(VideoGame)item;
            detail="Developer: "+g.developer+"  |  Platform: "+g.platform;
            if (g.hasPlatinum) detail += "  |  🏆 Platinum: Ναι (" + g.platinumDate + ")";
            if (!g.startDate.isEmpty()||!g.endDate.isEmpty())
                dateInfo="Παιξιμο: "+(g.startDate.isEmpty()?"-":g.startDate)+" -> "+(g.endDate.isEmpty()?"σε εξελιξη":g.endDate);
        }

        HoverCard card = new HoverCard(); card.setAlignmentX(LEFT_ALIGNMENT); card.setLayout(new BorderLayout(10,0));
        card.setBorder(new CompoundBorder(new LineBorder(border(),1,true), new EmptyBorder(12,14,12,14)));
        boolean hasMeta = !dateInfo.isEmpty() || !item.status.isEmpty();
        boolean hasTags = item.tags != null && !item.tags.isEmpty();
        int cardH = 84 + (hasMeta?18:0) + (hasTags?18:0);
        if (!item.imagePath.isEmpty()) cardH = Math.max(cardH, 92);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, cardH));

        JPanel stripe = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),6,6));
            }
        };
        stripe.setOpaque(false); stripe.setPreferredSize(new Dimension(5,0)); card.add(stripe, BorderLayout.WEST);

        ImageIcon thumb = loadThumb(item.imagePath, 42, 56);
        JLabel icLbl;
        if (thumb != null) {
            icLbl = new JLabel(thumb);
            icLbl.setPreferredSize(new Dimension(46, 60));
            icLbl.setBorder(new EmptyBorder(0,4,0,8));
        } else {
            icLbl = new JLabel(tStr.substring(0,1), SwingConstants.CENTER);
            icLbl.setFont(new Font("Segoe UI",Font.BOLD,13)); icLbl.setForeground(col);
            icLbl.setOpaque(true); icLbl.setBackground(colBg);
            icLbl.setPreferredSize(new Dimension(36,36)); icLbl.setBorder(new EmptyBorder(0,4,0,8));
        }
        card.add(icLbl, BorderLayout.LINE_START);

        JPanel info = new JPanel(); info.setOpaque(false); info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0)); row1.setOpaque(false);
        JLabel badge=new JLabel(tStr); badge.setFont(FONT_LABEL); badge.setForeground(col); badge.setBackground(colBg); badge.setOpaque(true); badge.setBorder(new EmptyBorder(2,6,2,6));
        JLabel tl=new JLabel(item.title); tl.setFont(FONT_CARD); tl.setForeground(txt());
        JLabel yl=new JLabel("· "+item.year); yl.setFont(FONT_BODY); yl.setForeground(txtMuted());
        JLabel gl=new JLabel("· "+item.genre); gl.setFont(FONT_BODY); gl.setForeground(txtMuted());
        row1.add(badge); row1.add(tl); row1.add(yl); row1.add(gl);
        if (item.favorite) {
            JLabel heart = new JLabel(new GlyphIcon(GlyphIcon.Kind.HEART, new Color(229,73,109), 14, 14));
            heart.setToolTipText("Αγαπημενο"); row1.add(heart);
        }
        info.add(row1);

        JPanel row2=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); row2.setOpaque(false);
        JLabel dl=new JLabel(detail); dl.setFont(FONT_SMALL); dl.setForeground(txtMuted()); row2.add(dl); info.add(row2);

        if (hasMeta) {
            JPanel row3=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0)); row3.setOpaque(false);
            if (!item.status.isEmpty()) {
                JLabel pill = new JLabel(statusLabel(item.status));
                pill.setFont(FONT_LABEL); pill.setOpaque(true);
                pill.setForeground(Color.WHITE); pill.setBackground(statusColor(item.status));
                pill.setBorder(new EmptyBorder(2,8,2,8));
                row3.add(pill);
            }
            if (!dateInfo.isEmpty()) {
                JLabel dtl=new JLabel("\u25A6 "+dateInfo); dtl.setFont(FONT_SMALL); dtl.setForeground(col); row3.add(dtl);
            }
            info.add(row3);
        }

        JPanel row4=new JPanel(new FlowLayout(FlowLayout.LEFT,2,1)); row4.setOpaque(false);
        for (int s=1;s<=5;s++) {
            JLabel star=new JLabel(s<=item.rating?"\u2605":"\u2606");
            star.setFont(FONT_SYM); star.setForeground(s<=item.rating?starOn():starOff()); row4.add(star);
        }
        if (item.notes != null && !item.notes.isEmpty()) {
            JLabel noteLbl = new JLabel(new GlyphIcon(GlyphIcon.Kind.NOTE, accent(), 15, 15));
            noteLbl.setBorder(new EmptyBorder(0,8,0,0));
            noteLbl.setToolTipText(notesTooltip(item.notes));
            row4.add(noteLbl);
        }
        info.add(row4);

        if (hasTags) {
            JPanel row5=new JPanel(new FlowLayout(FlowLayout.LEFT,4,1)); row5.setOpaque(false);
            int shown=0;
            for (String tag : item.tags) {
                if (shown++ >= 4) { JLabel more=new JLabel("…"); more.setFont(FONT_SMALL); more.setForeground(txtMuted()); row5.add(more); break; }
                JLabel chip=new JLabel("#"+tag);
                chip.setFont(FONT_SMALL); chip.setForeground(accent());
                chip.setOpaque(true); chip.setBackground(surface2());
                chip.setBorder(new EmptyBorder(1,6,1,6));
                row5.add(chip);
            }
            info.add(row5);
        }
        card.add(info, BorderLayout.CENTER);

        JPanel btnCol = new JPanel(); btnCol.setOpaque(false); btnCol.setLayout(new BoxLayout(btnCol, BoxLayout.Y_AXIS));
        JButton editBtn = new JButton(new GlyphIcon(GlyphIcon.Kind.PENCIL, editCol(), 16, 16));
        editBtn.setBackground(editBg()); editBtn.setToolTipText("Επεξεργασια");
        editBtn.setBorderPainted(false); editBtn.setFocusPainted(false); editBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editBtn.setPreferredSize(new Dimension(34,28)); editBtn.setMaximumSize(new Dimension(34,28));
        editBtn.addActionListener(e -> openEditDialog(item));

        JButton delBtn = new JButton(new GlyphIcon(GlyphIcon.Kind.TRASH, danger(), 16, 16));
        delBtn.setBackground(surface()); delBtn.setToolTipText("Διαγραφη");
        delBtn.setBorderPainted(false); delBtn.setFocusPainted(false); delBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delBtn.setPreferredSize(new Dimension(34,28)); delBtn.setMaximumSize(new Dimension(34,28));
        delBtn.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){ delBtn.setBackground(dangerBg()); }
            public void mouseExited(MouseEvent e) { delBtn.setBackground(surface()); }
        });
        delBtn.addActionListener(e -> {
            pushUndo();
            collection.remove(item);
            refreshList();
            showToast("Διαγραφηκε \"" + item.title + "\" — πατησε Αναιρεση για επαναφορα", true);
        });
        btnCol.add(editBtn); btnCol.add(Box.createVerticalStrut(4)); btnCol.add(delBtn); card.add(btnCol, BorderLayout.EAST);

        // Κλικ οπουδηποτε στην καρτα (εκτος των κουμπιων) -> προβολη
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){ openViewDialog(item); }
        });
        return card;
    }

    private void refreshStats() {
        statsPanel.removeAll();

        // Κεφαλιδα + κουμπι εξαγωγης αναφορας
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
        head.setAlignmentX(LEFT_ALIGNMENT); head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel htitle = new JLabel("Στατιστικα συλλογης");
        htitle.setFont(new Font("Segoe UI",Font.BOLD,16)); htitle.setForeground(txt());
        JButton exportBtn = smallBtn("Εξαγωγη αναφορας (HTML)");
        exportBtn.addActionListener(e -> exportReport());
        JPanel hr = new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0)); hr.setOpaque(false); hr.add(exportBtn);
        head.add(htitle, BorderLayout.WEST); head.add(hr, BorderLayout.EAST);
        statsPanel.add(head); statsPanel.add(vgap(16));

        long movies = collection.stream().filter(i->i instanceof Movie).count();
        long series = collection.stream().filter(i->i instanceof Series).count();
        long books  = collection.stream().filter(i->i instanceof Book).count();
        long games  = collection.stream().filter(i->i instanceof VideoGame).count();
        double avg  = collection.stream().mapToInt(i->i.rating).filter(r->r>0).average().orElse(0);
        long platinums = collection.stream().filter(i -> i instanceof VideoGame && ((VideoGame) i).hasPlatinum).count();
        long favs   = collection.stream().filter(i->i.favorite).count();
        long completed = collection.stream().filter(i->"completed".equals(i.status)).count();
        long inProgress = collection.stream().filter(i->"in_progress".equals(i.status)).count();
        int completedThisYear = (int) collection.stream()
                .filter(i->"completed".equals(i.status))
                .filter(i->endYearOf(i)==LocalDate.now().getYear()).count();

        String topGenre = "Κανένα";
        if (!collection.isEmpty()) {
            topGenre = collection.stream()
                    .collect(Collectors.groupingBy(MediaItem::getGenre, Collectors.counting()))
                    .entrySet().stream().max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse("Κανένα");
        }

        JPanel cards=new JPanel(new GridLayout(1,5,10,0));
        cards.setOpaque(false); cards.setAlignmentX(LEFT_ALIGNMENT); cards.setMaximumSize(new Dimension(Integer.MAX_VALUE,100));
        cards.add(statCard("Συνολο",  String.valueOf(collection.size()), accent()));
        cards.add(statCard("Ταινιες", String.valueOf(movies),  movieCol()));
        cards.add(statCard("Σειρες",  String.valueOf(series),  seriesCol()));
        cards.add(statCard("Βιβλια",  String.valueOf(books),   bookCol()));
        cards.add(statCard("Games",   String.valueOf(games),   gameCol()));
        statsPanel.add(cards); statsPanel.add(vgap(12));

        JPanel cards2=new JPanel(new GridLayout(1,4,10,0));
        cards2.setOpaque(false); cards2.setAlignmentX(LEFT_ALIGNMENT); cards2.setMaximumSize(new Dimension(Integer.MAX_VALUE,100));
        cards2.add(statCard("Μ.Ο. βαθμ.", String.format("%.1f", avg), new Color(234,179,8)));
        cards2.add(statCard("Αγαπημενα", String.valueOf(favs), new Color(229,73,109)));
        cards2.add(statCard("Ολοκληρωμενα", String.valueOf(completed), statusColor("completed")));
        cards2.add(statCard("Σε εξελιξη", String.valueOf(inProgress), statusColor("in_progress")));
        statsPanel.add(cards2); statsPanel.add(vgap(20));

        JPanel extraStats = new JPanel(new GridLayout(1, 3, 10, 0));
        extraStats.setOpaque(false); extraStats.setAlignmentX(LEFT_ALIGNMENT);
        extraStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel genreLbl=new JLabel("Δημοφιλεστερο ειδος: " + topGenre,
                new GlyphIcon(GlyphIcon.Kind.FLAME, new Color(234,88,12), 16,16), SwingConstants.LEFT);
        genreLbl.setFont(FONT_BODY); genreLbl.setForeground(accent()); genreLbl.setIconTextGap(6);
        JLabel platLbl=new JLabel("Platinum: " + platinums,
                new GlyphIcon(GlyphIcon.Kind.TROPHY, gameCol(), 16,16), SwingConstants.LEFT);
        platLbl.setFont(FONT_BODY); platLbl.setForeground(gameCol()); platLbl.setIconTextGap(6);
        JLabel cyLbl=new JLabel("Ολοκληρωθηκαν φετος: " + completedThisYear,
                new GlyphIcon(GlyphIcon.Kind.CHECK, statusColor("completed"), 16,16), SwingConstants.LEFT);
        cyLbl.setFont(FONT_BODY); cyLbl.setForeground(statusColor("completed")); cyLbl.setIconTextGap(6);
        extraStats.add(genreLbl); extraStats.add(platLbl); extraStats.add(cyLbl);
        statsPanel.add(extraStats); statsPanel.add(vgap(20));

        // Κατανομη ανα τυπο
        JLabel ct=new JLabel("Κατανομη ανα τυπο");
        ct.setFont(new Font("Segoe UI",Font.BOLD,14)); ct.setForeground(txt()); ct.setAlignmentX(LEFT_ALIGNMENT);
        statsPanel.add(ct); statsPanel.add(vgap(10));
        int total=Math.max(collection.size(),1);
        statsPanel.add(barRow("Ταινιες",(int)movies,total,movieCol())); statsPanel.add(vgap(6));
        statsPanel.add(barRow("Σειρες", (int)series,total,seriesCol())); statsPanel.add(vgap(6));
        statsPanel.add(barRow("Βιβλια", (int)books, total,bookCol()));   statsPanel.add(vgap(6));
        statsPanel.add(barRow("Games",  (int)games, total,gameCol()));   statsPanel.add(vgap(20));

        // Ιστογραμμα βαθμολογιων
        JLabel rh=new JLabel("Κατανομη βαθμολογιων");
        rh.setFont(new Font("Segoe UI",Font.BOLD,14)); rh.setForeground(txt()); rh.setAlignmentX(LEFT_ALIGNMENT);
        statsPanel.add(rh); statsPanel.add(vgap(10));
        int[] rc = new int[6];
        for (MediaItem i : collection) if (i.rating>=1 && i.rating<=5) rc[i.rating]++;
        int maxRating = 1; for (int s=1;s<=5;s++) maxRating = Math.max(maxRating, rc[s]);
        for (int s=5;s>=1;s--) { statsPanel.add(barRow(starsStr(s), rc[s], maxRating, new Color(234,179,8), FONT_SYM)); statsPanel.add(vgap(6)); }
        statsPanel.add(vgap(14));

        // Εγγραφες ανα ετος (top)
        Map<Integer,Long> byYear = collection.stream().collect(Collectors.groupingBy(MediaItem::getYear, Collectors.counting()));
        if (!byYear.isEmpty()) {
            JLabel yh=new JLabel("Εγγραφες ανα ετος (κορυφαια)");
            yh.setFont(new Font("Segoe UI",Font.BOLD,14)); yh.setForeground(txt()); yh.setAlignmentX(LEFT_ALIGNMENT);
            statsPanel.add(yh); statsPanel.add(vgap(10));
            long maxY = byYear.values().stream().mapToLong(Long::longValue).max().orElse(1);
            byYear.entrySet().stream()
                    .sorted(Map.Entry.<Integer,Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                    .limit(6)
                    .forEach(en -> { statsPanel.add(barRow(String.valueOf(en.getKey()), en.getValue().intValue(), (int)maxY, accent())); statsPanel.add(vgap(6)); });
            statsPanel.add(vgap(14));
        }

        if (!collection.isEmpty()) {
            JLabel topT=new JLabel("Top αξιολογησεις");
            topT.setFont(new Font("Segoe UI",Font.BOLD,14)); topT.setForeground(txt()); topT.setAlignmentX(LEFT_ALIGNMENT);
            statsPanel.add(topT); statsPanel.add(vgap(8));
            collection.stream().filter(i->i.rating>0)
                    .sorted(Comparator.comparingInt((MediaItem i)->i.rating).reversed()).limit(5)
                    .forEach(i -> {
                        JLabel lbl=new JLabel(starsStr(i.rating)+"  "+i.title+"  ("+i.year+")  ["+typeLabel(i)+"]");
                        lbl.setFont(FONT_SYM); lbl.setForeground(txt()); lbl.setAlignmentX(LEFT_ALIGNMENT);
                        statsPanel.add(lbl); statsPanel.add(vgap(4));
                    });
        }
        statsPanel.add(Box.createVerticalGlue());
        statsPanel.revalidate(); statsPanel.repaint();
    }

    // Ετος ολοκληρωσης (απο endDate/watchDate) — για στατιστικα "φετος"
    private int endYearOf(MediaItem i) {
        String d = "";
        if (i instanceof Movie m) d = m.watchDate;
        else if (i instanceof Series s) d = s.endDate.isEmpty()?s.startDate:s.endDate;
        else if (i instanceof Book b) d = b.endDate.isEmpty()?b.startDate:b.endDate;
        else if (i instanceof VideoGame g) d = g.endDate.isEmpty()?g.startDate:g.endDate;
        try { return LocalDate.parse(d.trim(), DateTimeFormatter.ofPattern("dd/MM/uuuu")).getYear(); }
        catch (Exception e) { return -1; }
    }

    // ── Εξαγωγη αναφορας HTML ─────────────────────────────────────────────────
    private static String htmlEsc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n","<br>");
    }
    private void exportReport() {
        if (collection.isEmpty()) { showToast("Η συλλογη ειναι κενη.", false); return; }
        JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Εξαγωγη αναφορας (HTML)");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("HTML αρχειο","html"));
        fc.setSelectedFile(new File("mediahub_report.html"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".html")) f = new File(f.getParentFile(), f.getName()+".html");
        try {
            Files.writeString(f.toPath(), buildReportHtml(), StandardCharsets.UTF_8);
            showToast("Αναφορα: " + f.getName(), true);
        } catch (IOException ex) { showToast("Σφαλμα εξαγωγης!", false); }
    }
    private String buildReportHtml() {
        long movies = collection.stream().filter(i->i instanceof Movie).count();
        long series = collection.stream().filter(i->i instanceof Series).count();
        long books  = collection.stream().filter(i->i instanceof Book).count();
        long games  = collection.stream().filter(i->i instanceof VideoGame).count();
        double avg  = collection.stream().mapToInt(i->i.rating).filter(r->r>0).average().orElse(0);
        long favs   = collection.stream().filter(i->i.favorite).count();
        long completed = collection.stream().filter(i->"completed".equals(i.status)).count();
        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html lang='el'><head><meta charset='UTF-8'>");
        h.append("<title>MediaHub — Αναφορα</title><style>");
        h.append("body{font-family:'Segoe UI',Arial,sans-serif;margin:0;background:#f4f4f1;color:#1a1a22;}");
        h.append(".wrap{max-width:900px;margin:0 auto;padding:32px 24px;}");
        h.append("h1{margin:0 0 4px;} .sub{color:#777;margin-bottom:24px;}");
        h.append(".cards{display:flex;flex-wrap:wrap;gap:12px;margin-bottom:28px;}");
        h.append(".card{background:#fff;border:1px solid #e2e2dd;border-radius:10px;padding:14px 18px;min-width:120px;}");
        h.append(".card .v{font-size:26px;font-weight:700;} .card .l{color:#888;font-size:12px;}");
        h.append("h2{margin:26px 0 10px;border-bottom:2px solid #0b8286;padding-bottom:4px;}");
        h.append(".item{background:#fff;border:1px solid #e2e2dd;border-radius:10px;padding:12px 16px;margin-bottom:10px;}");
        h.append(".item .t{font-weight:700;font-size:15px;} .meta{color:#777;font-size:13px;margin:2px 0;}");
        h.append(".stars{color:#eab308;} .pill{display:inline-block;color:#fff;border-radius:10px;padding:1px 8px;font-size:11px;}");
        h.append(".tag{display:inline-block;background:#eef;color:#0b8286;border-radius:8px;padding:1px 7px;font-size:11px;margin-right:4px;}");
        h.append(".fav{color:#e5496d;} .notes{margin-top:6px;padding:8px 10px;background:#faf9f6;border-left:3px solid #0b8286;white-space:pre-wrap;font-size:13px;}");
        h.append("</style></head><body><div class='wrap'>");
        h.append("<h1>MediaHub — Η συλλογη μου</h1>");
        h.append("<div class='sub'>Δημιουργηθηκε: ").append(now).append("</div>");

        h.append("<div class='cards'>");
        h.append(reportCard("Συνολο", String.valueOf(collection.size())));
        h.append(reportCard("Ταινιες", String.valueOf(movies)));
        h.append(reportCard("Σειρες", String.valueOf(series)));
        h.append(reportCard("Βιβλια", String.valueOf(books)));
        h.append(reportCard("Games", String.valueOf(games)));
        h.append(reportCard("Μ.Ο. βαθμ.", String.format("%.1f", avg)));
        h.append(reportCard("Αγαπημενα", String.valueOf(favs)));
        h.append(reportCard("Ολοκληρωμενα", String.valueOf(completed)));
        h.append("</div>");

        String[] order = {"Ταινια","Σειρα","Βιβλιο","Game"};
        for (String t : order) {
            List<MediaItem> group = collection.stream()
                    .filter(i -> typeLabel(i).equals(t))
                    .sorted(Comparator.comparing((MediaItem i)->i.title.toLowerCase()))
                    .collect(Collectors.toList());
            if (group.isEmpty()) continue;
            h.append("<h2>").append(htmlEsc(t)).append(" (").append(group.size()).append(")</h2>");
            for (MediaItem i : group) h.append(reportItem(i));
        }
        h.append("</div></body></html>");
        return h.toString();
    }
    private String reportCard(String label, String value) {
        return "<div class='card'><div class='v'>"+htmlEsc(value)+"</div><div class='l'>"+htmlEsc(label)+"</div></div>";
    }
    private String reportItem(MediaItem i) {
        StringBuilder b = new StringBuilder("<div class='item'>");
        b.append("<div class='t'>").append(htmlEsc(i.title)).append(" <span style='color:#999;font-weight:400'>· ").append(i.year).append(" · ").append(htmlEsc(i.genre)).append("</span>");
        if (i.favorite) b.append(" <span class='fav'>&#9829;</span>");
        b.append("</div>");
        // λεπτομερειες ανα τυπο
        String detail;
        if (i instanceof Movie m) detail = "Σκηνοθετης: "+htmlEsc(m.director)+" | Ηθοποιοι: "+htmlEsc(m.actors)+dateStr("Παρακολ.", m.watchDate, "");
        else if (i instanceof Series s) detail = "Σεζον: "+s.seasons+" | Ηθοποιοι: "+htmlEsc(s.actors)+dateStr("Παρακολ.", s.startDate, s.endDate);
        else if (i instanceof Book bk) detail = "Συγγραφεας: "+htmlEsc(bk.author)+(bk.publisher.isEmpty()?"":" | Εκδοτης: "+htmlEsc(bk.publisher))+" | Σελιδες: "+bk.pages+dateStr("Αναγνωση", bk.startDate, bk.endDate);
        else { VideoGame g=(VideoGame)i; detail = "Developer: "+htmlEsc(g.developer)+" | Platform: "+htmlEsc(g.platform)+(g.hasPlatinum?" | 🏆 Platinum":"" )+dateStr("Παιξιμο", g.startDate, g.endDate); }
        b.append("<div class='meta'>").append(detail).append("</div>");
        // αστερια + κατασταση
        b.append("<div class='meta'><span class='stars'>").append(starsStr(i.rating)).append("</span>");
        if (!i.status.isEmpty()) {
            Color sc = statusColor(i.status);
            String hex = String.format("#%02x%02x%02x", sc.getRed(), sc.getGreen(), sc.getBlue());
            b.append(" &nbsp; <span class='pill' style='background:").append(hex).append("'>").append(htmlEsc(statusLabel(i.status))).append("</span>");
        }
        b.append("</div>");
        if (i.tags != null && !i.tags.isEmpty()) {
            b.append("<div class='meta'>");
            for (String tag : i.tags) b.append("<span class='tag'>#").append(htmlEsc(tag)).append("</span>");
            b.append("</div>");
        }
        if (i.notes != null && !i.notes.isEmpty())
            b.append("<div class='notes'>").append(htmlEsc(i.notes)).append("</div>");
        b.append("</div>");
        return b.toString();
    }
    private String dateStr(String label, String start, String end) {
        if ((start==null||start.isEmpty()) && (end==null||end.isEmpty())) return "";
        if (end==null||end.isEmpty()) return " | "+label+": "+htmlEsc(start);
        return " | "+label+": "+htmlEsc(start.isEmpty()?"-":start)+" → "+htmlEsc(end);
    }

    private String starsStr(int r) {
        StringBuilder sb=new StringBuilder(); for(int i=1;i<=5;i++) sb.append(i<=r?"\u2605":"\u2606"); return sb.toString();
    }
    private JPanel statCard(String label, String value, Color col) {
        JPanel p=new JPanel(new BorderLayout(0,6)); p.setBackground(surface());
        p.setBorder(new CompoundBorder(new LineBorder(border(),1,true),new EmptyBorder(14,16,14,16)));
        JLabel vl=new JLabel(value,SwingConstants.LEFT); vl.setFont(new Font("Segoe UI",Font.BOLD,28)); vl.setForeground(col);
        JLabel nl=new JLabel(label); nl.setFont(FONT_SMALL); nl.setForeground(txtMuted());
        p.add(vl,BorderLayout.CENTER); p.add(nl,BorderLayout.SOUTH); return p;
    }
    private JPanel barRow(String label, int count, int total, Color col) {
        return barRow(label, count, total, col, FONT_BODY);
    }
    private JPanel barRow(String label, int count, int total, Color col, Font labelFont) {
        JPanel row=new JPanel(new BorderLayout(10,0)); row.setOpaque(false); row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));
        JLabel lbl=new JLabel(label); lbl.setFont(labelFont); lbl.setForeground(txt()); lbl.setPreferredSize(new Dimension(80,20));
        float pct=(float)count/total;
        JPanel barBg=new JPanel(new BorderLayout()); barBg.setBackground(surface2()); barBg.setBorder(new LineBorder(border(),1,true));
        JPanel barFg=new JPanel(){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),4,4));
            }
        };
        barFg.setOpaque(false); barFg.setPreferredSize(new Dimension((int)(400*pct),20)); barBg.add(barFg,BorderLayout.WEST);
        JLabel cl=new JLabel(count+" ",SwingConstants.RIGHT); cl.setFont(FONT_SMALL); cl.setForeground(txtMuted()); cl.setPreferredSize(new Dimension(28,20));
        row.add(lbl,BorderLayout.WEST); row.add(barBg,BorderLayout.CENTER); row.add(cl,BorderLayout.EAST); return row;
    }

    private void saveToFile() {
        JFileChooser fc=new JFileChooser(); fc.setDialogTitle("Εξαγωγη συλλογης (JSON)");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("JSON αρχειο","json"));
        fc.setSelectedFile(new File("collection.json"));
        if (fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File f=fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".json"))
            f=new File(f.getParentFile(), f.getName()+".json");
        try {
            Files.writeString(f.toPath(), collectionToJson(), StandardCharsets.UTF_8);
            showToast("Αποθηκευτηκε: "+f.getName(), true);
        } catch (IOException ex) { showToast("Σφαλμα αποθηκευσης!", false); }
    }

    private void loadFromFile() {
        JFileChooser fc=new JFileChooser(); fc.setDialogTitle("Φορτωση / Εισαγωγη συλλογης");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new FileNameExtensionFilter("JSON ή CSV","json","csv"));
        if (fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File f=fc.getSelectedFile();
        try {
            pushUndo();   // ωστε μια κατα λαθος εισαγωγη να αναιρειται
            int loaded;
            if (f.getName().toLowerCase().endsWith(".json"))
                loaded = importJsonText(Files.readString(f.toPath(), StandardCharsets.UTF_8));
            else
                loaded = importCsvLines(Files.readAllLines(f.toPath(), StandardCharsets.UTF_8));
            refreshList();
            showToast("Φορτωθηκαν "+loaded+" εγγραφες!", true);
        } catch (Exception ex) { showToast("Σφαλμα: "+ex.getMessage(), false); }
    }

    // ── JSON (αυτονομο, χωρις εξωτερικες βιβλιοθηκες) ──────────────────────────
    private String collectionToJson() {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i=0; i<collection.size(); i++) {
            sb.append("  { ").append(itemJson(collection.get(i))).append(" }");
            if (i < collection.size()-1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private String itemJson(MediaItem i) {
        StringBuilder b = new StringBuilder();
        String type = i instanceof Movie ? "movie" : i instanceof Series ? "series"
                                                     : i instanceof Book ? "book" : "game";
        b.append(js("type", type)).append(", ");
        b.append(js("title", i.title)).append(", ");
        b.append(jn("year", i.year)).append(", ");
        b.append(js("genre", i.genre)).append(", ");
        b.append(jn("rating", i.rating)).append(", ");
        if (i instanceof Movie m) {
            b.append(js("director", m.director)).append(", ");
            b.append(js("actors", m.actors)).append(", ");
            b.append(js("watchDate", m.watchDate)).append(", ");
        } else if (i instanceof Series s) {
            b.append(jn("seasons", s.seasons)).append(", ");
            b.append(js("actors", s.actors)).append(", ");
            b.append(js("startDate", s.startDate)).append(", ");
            b.append(js("endDate", s.endDate)).append(", ");
        } else if (i instanceof Book bk) {
            b.append(js("author", bk.author)).append(", ");
            b.append(jn("pages", bk.pages)).append(", ");
            b.append(js("publisher", bk.publisher)).append(", ");
            b.append(js("startDate", bk.startDate)).append(", ");
            b.append(js("endDate", bk.endDate)).append(", ");
        } else {
            VideoGame g=(VideoGame)i;
            b.append(js("developer", g.developer)).append(", ");
            b.append(js("platform", g.platform)).append(", ");
            b.append(js("startDate", g.startDate)).append(", ");
            b.append(js("endDate", g.endDate)).append(", ");
            b.append(jb("hasPlatinum", g.hasPlatinum)).append(", ");
            b.append(js("platinumDate", g.platinumDate)).append(", ");
        }
        b.append(js("imagePath", i.imagePath)).append(", ");
        b.append(js("notes", i.notes)).append(", ");
        b.append(js("status", i.status)).append(", ");
        b.append(jb("favorite", i.favorite)).append(", ");
        b.append(jl("added", i.added)).append(", ");
        b.append(ja("tags", i.tags));
        return b.toString();
    }
    private String js(String k, String v){ return "\""+k+"\": \""+escapeJson(v)+"\""; }
    private String jn(String k, int v){ return "\""+k+"\": "+v; }
    private String jl(String k, long v){ return "\""+k+"\": "+v; }
    private String jb(String k, boolean v){ return "\""+k+"\": "+v; }
    private String ja(String k, List<String> list){
        StringBuilder b=new StringBuilder("\""+k+"\": [");
        for (int i=0;i<list.size();i++){ b.append("\"").append(escapeJson(list.get(i))).append("\""); if(i<list.size()-1) b.append(", "); }
        b.append("]");
        return b.toString();
    }

    private String escapeJson(String v) {
        if (v == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i=0; i<v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '"'  -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default   -> { if (c < 0x20) b.append(String.format("\\u%04x",(int)c)); else b.append(c); }
            }
        }
        return b.toString();
    }

    private int importJsonText(String text) {
        int loaded = 0;
        Object root = Json.parse(text);
        if (root instanceof List<?> arr) {
            for (Object o : arr) {
                if (o instanceof Map<?,?> raw) {
                    @SuppressWarnings("unchecked")
                    MediaItem it = itemFromJson((Map<String,Object>) raw);
                    if (it != null) { collection.add(it); loaded++; }
                }
            }
        }
        return loaded;
    }

    private MediaItem itemFromJson(Map<String,Object> m) {
        String type = gs(m,"type");
        String title=gs(m,"title"); int year=gi(m,"year"); String genre=gs(m,"genre");
        MediaItem item;
        switch (type) {
            case "movie"  -> { Movie x=new Movie(title,year,genre,gs(m,"director"),gs(m,"actors")); x.watchDate=gs(m,"watchDate"); item=x; }
            case "series" -> { Series x=new Series(title,year,genre,gi(m,"seasons"),gs(m,"actors")); x.startDate=gs(m,"startDate"); x.endDate=gs(m,"endDate"); item=x; }
            case "book"   -> { Book x=new Book(title,year,genre,gs(m,"author"),gi(m,"pages")); x.publisher=gs(m,"publisher"); x.startDate=gs(m,"startDate"); x.endDate=gs(m,"endDate"); item=x; }
            case "game"   -> { VideoGame x=new VideoGame(title,year,genre,gs(m,"developer"),gs(m,"platform")); x.startDate=gs(m,"startDate"); x.endDate=gs(m,"endDate"); x.hasPlatinum=gb(m,"hasPlatinum"); x.platinumDate=gs(m,"platinumDate"); item=x; }
            default -> { return null; }
        }
        item.rating=gi(m,"rating"); item.imagePath=gs(m,"imagePath"); item.notes=gs(m,"notes");
        item.status=gs(m,"status"); item.favorite=gb(m,"favorite"); item.added=gL(m,"added"); item.tags=gl(m,"tags");
        return item;
    }
    private static String gs(Map<String,Object> m, String k){ Object o=m.get(k); return o==null?"":o.toString(); }
    private static int gi(Map<String,Object> m, String k){
        Object o=m.get(k);
        if (o instanceof Number n) return n.intValue();
        try { return o==null?0:(int)Double.parseDouble(o.toString()); } catch (Exception e){ return 0; }
    }
    private static boolean gb(Map<String,Object> m, String k){
        Object o=m.get(k);
        return o instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(o));
    }
    private static long gL(Map<String,Object> m, String k){
        Object o=m.get(k);
        if (o instanceof Number n) return n.longValue();
        try { return o==null?0L:(long)Double.parseDouble(o.toString()); } catch (Exception e){ return 0L; }
    }
    private static List<String> gl(Map<String,Object> m, String k){
        List<String> out=new ArrayList<>();
        Object o=m.get(k);
        if (o instanceof List<?> a) for (Object e : a) if (e!=null) out.add(e.toString());
        return out;
    }

    // ── CSV import (για παλια αρχεια / χειροκινητη εισαγωγη) ───────────────────
    private int importCsvLines(List<String> lines) {
        int loaded = 0;
        for (int idx=1; idx<lines.size(); idx++) {
            String line=lines.get(idx).trim(); if (line.isEmpty()) continue;
            MediaItem it = csvToItem(parseCsvLine(line));
            if (it != null) { collection.add(it); loaded++; }
        }
        return loaded;
    }
    private MediaItem csvToItem(String[] p) {
        if (p.length < 7) return null;
        try {
            String type=p[0].trim(), title=p[1];
            int year=Integer.parseInt(p[2].trim());
            String genre=p[3], e1=p[4], e2=p[5];
            int rating=Integer.parseInt(p[6].trim());
            String d1=p.length>7?p[7]:"", d2=p.length>8?p[8]:"";
            MediaItem item;
            switch (type) {
                case "movie"  -> { Movie m=new Movie(title,year,genre,e1,e2); m.watchDate=d1; item=m; }
                case "series" -> { Series s=new Series(title,year,genre,Integer.parseInt(e1.trim()),e2); s.startDate=d1; s.endDate=d2; item=s; }
                case "book"   -> { Book b=new Book(title,year,genre,e1,Integer.parseInt(e2.trim())); b.startDate=d1; b.endDate=d2; item=b; }
                case "game"   -> {
                    VideoGame g=new VideoGame(title,year,genre,e1,e2); g.startDate=d1; g.endDate=d2;
                    if (p.length > 9) g.hasPlatinum = Boolean.parseBoolean(p[9]);
                    if (p.length > 10) g.platinumDate = p[10];
                    item=g;
                }
                default -> { return null; }
            }
            item.rating=rating;
            if (p.length > 11) item.imagePath = p[11];
            if (p.length > 12) item.notes = decodeNotes(p[12]);
            return item;
        } catch (Exception ex) { return null; }
    }

    // Μικρος, αυτονομος JSON parser — object/array/string/number/bool/null.
    static final class Json {
        private final String s; private int i;
        private Json(String s){ this.s=s; }
        static Object parse(String s){ Json j=new Json(s); j.ws(); Object v=j.value(); return v; }
        private void ws(){ while (i<s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private Object value(){
            char c=s.charAt(i);
            return switch (c) {
                case '{' -> obj();
                case '[' -> arr();
                case '"' -> str();
                case 't' -> { i+=4; yield Boolean.TRUE; }
                case 'f' -> { i+=5; yield Boolean.FALSE; }
                case 'n' -> { i+=4; yield null; }
                default  -> num();
            };
        }
        private Map<String,Object> obj(){
            Map<String,Object> m=new LinkedHashMap<>(); i++; ws();
            if (s.charAt(i)=='}'){ i++; return m; }
            while (true) {
                ws(); String k=str(); ws(); i++; /* ':' */ ws();
                m.put(k, value()); ws();
                char c=s.charAt(i++);
                if (c=='}') break;          // αλλιως ',' -> συνεχεια
            }
            return m;
        }
        private List<Object> arr(){
            List<Object> a=new ArrayList<>(); i++; ws();
            if (s.charAt(i)==']'){ i++; return a; }
            while (true) {
                ws(); a.add(value()); ws();
                char c=s.charAt(i++);
                if (c==']') break;
            }
            return a;
        }
        private String str(){
            StringBuilder b=new StringBuilder(); i++; // ανοιγμα "
            while (true) {
                char c=s.charAt(i++);
                if (c=='"') break;
                if (c=='\\') {
                    char e=s.charAt(i++);
                    switch (e) {
                        case '"' -> b.append('"');
                        case '\\'-> b.append('\\');
                        case '/' -> b.append('/');
                        case 'n' -> b.append('\n');
                        case 't' -> b.append('\t');
                        case 'r' -> b.append('\r');
                        case 'b' -> b.append('\b');
                        case 'f' -> b.append('\f');
                        case 'u' -> { b.append((char)Integer.parseInt(s.substring(i,i+4),16)); i+=4; }
                        default  -> b.append(e);
                    }
                } else b.append(c);
            }
            return b.toString();
        }
        private Double num(){
            int start=i;
            while (i<s.length() && "+-0123456789.eE".indexOf(s.charAt(i))>=0) i++;
            return Double.parseDouble(s.substring(start,i));
        }
    }

    // Στιβαρό CSV Parsing που υποστηρίζει κείμενα με κόμματα (μέσα σε εισαγωγικά)
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString()); sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    // Κωδικοποιηση/αποκωδικοποιηση σημειωσεων ωστε οι αλλαγες γραμμης να μενουν
    // σε μια φυσικη γραμμη CSV (το U+2028 δεν θεωρειται αλλαγη γραμμης απο τον reader).
    private String encodeNotes(String s) {
        if (s == null) return "";
        return s.replace("\r\n","\u2028").replace("\n","\u2028").replace("\r","\u2028");
    }

    private String decodeNotes(String s) {
        if (s == null) return "";
        return s.replace("\u2028","\n");
    }

    // Μέθοδος διαφυγής χαρακτήρων για ασφαλή εγγραφή σε CSV
    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    // Ετικετες: μετατροπη μεταξυ κειμενου (χωρισμενο με κομμα) και λιστας
    private List<String> parseTags(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String t : text.split(",")) {
            String s = t.trim();
            if (!s.isEmpty() && !out.contains(s)) out.add(s);
        }
        return out;
    }
    private String tagsToString(List<String> tags) {
        return tags == null ? "" : String.join(", ", tags);
    }

    // Tooltip προεπισκοπησης σημειωσεων (HTML, με αναδιπλωση & περικοπη)
    private String notesTooltip(String notes) {
        String t = notes.length() > 400 ? notes.substring(0,400) + "..." : notes;
        t = t.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n","<br>");
        return "<html><div style='width:280px;padding:2px'>" + t + "</div></html>";
    }

    // ── Συστημα Αναιρεσης (Undo) ──────────────────────────────────────────────
    // Πριν απο καθε μεταβολη (προσθηκη/διαγραφη/επεξεργασια) κραταμε ενα
    // στιγμιοτυπο (snapshot) της συλλογης. Η αναιρεση επαναφερει το τελευταιο.
    private void pushUndo() {
        List<MediaItem> snap = new ArrayList<>();
        for (MediaItem i : collection) snap.add(cloneItem(i));
        undoStack.push(snap);
        while (undoStack.size() > 30) undoStack.removeLast();   // οριο ιστορικου
        updateUndoState();
        dirty = true;
    }

    private void undo() {
        if (undoStack.isEmpty()) { showToast("Δεν υπαρχει κατι για αναιρεση", false); return; }
        List<MediaItem> prev = undoStack.pop();
        collection.clear();
        collection.addAll(prev);
        refreshList();
        updateUndoState();
        dirty = true;
        showToast("Η τελευταια ενεργεια αναιρεθηκε", true);
    }

    private void updateUndoState() {
        if (undoBtn != null) undoBtn.setEnabled(!undoStack.isEmpty());
    }

    // Βαθυ αντιγραφο ενος στοιχειου (για το snapshot της αναιρεσης)
    private MediaItem cloneItem(MediaItem i) {
        MediaItem c;
        if (i instanceof Movie m) {
            c = new Movie(m.title, m.year, m.genre, m.director, m.actors);
        } else if (i instanceof Series s) {
            Series x = new Series(s.title, s.year, s.genre, s.seasons, s.actors);
            x.startDate = s.startDate; x.endDate = s.endDate; c = x;
        } else if (i instanceof Book b) {
            Book x = new Book(b.title, b.year, b.genre, b.author, b.pages);
            x.publisher = b.publisher; x.startDate = b.startDate; x.endDate = b.endDate; c = x;
        } else {
            VideoGame g = (VideoGame) i;
            VideoGame x = new VideoGame(g.title, g.year, g.genre, g.developer, g.platform);
            x.startDate = g.startDate; x.endDate = g.endDate;
            x.hasPlatinum = g.hasPlatinum; x.platinumDate = g.platinumDate; c = x;
        }
        c.rating = i.rating; c.watchDate = i.watchDate; c.imagePath = i.imagePath; c.notes = i.notes;
        c.status = i.status; c.favorite = i.favorite; c.tags = new ArrayList<>(i.tags); c.added = i.added;
        return c;
    }

    // ── Εικονες ───────────────────────────────────────────────────────────────
    // Αντιγραφει μια εξωτερικη εικονα στον φακελο της εφαρμογης, ωστε να μην
    // εξαρταται απο το πρωτοτυπο αρχειο. Επιστρεφει τη μονιμη διαδρομη.
    // Αν ειναι ηδη δικο μας αντιγραφο ή κενη, την επιστρεφει ως εχει.
    private String persistImage(String path) {
        if (path == null || path.isEmpty()) return "";
        try {
            File src = new File(path);
            if (!src.exists()) return path;   // δεν υπαρχει πια — κρατα ο,τι ειχαμε
            File dirAbs = IMAGE_DIR.getAbsoluteFile();
            File parent = src.getParentFile() == null ? null : src.getParentFile().getAbsoluteFile();
            if (dirAbs.equals(parent)) return src.getAbsolutePath();   // ηδη δικο μας αντιγραφο
            if (!IMAGE_DIR.exists()) IMAGE_DIR.mkdirs();
            String name = src.getName();
            int dot = name.lastIndexOf('.');
            String ext = dot >= 0 ? name.substring(dot) : "";
            String unique = "img_" + System.currentTimeMillis() + "_"
                    + Math.abs(new java.util.Random().nextInt(100000)) + ext;
            File dst = new File(IMAGE_DIR, unique);
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dst.getAbsolutePath();
        } catch (Exception ex) {
            return path;   // σε σφαλμα, κρατα την αρχικη διαδρομη
        }
    }

    private String chooseImageFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Επιλογη εικονας");
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileNameExtensionFilter("Εικονες (jpg, png, gif, bmp)", "jpg","jpeg","png","gif","bmp"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        return fc.getSelectedFile().getAbsolutePath();
    }

    private void pickImageForForm() {
        String path = chooseImageFile();
        if (path != null) {
            pendingImagePath = path;
            imageNameLabel.setText(new File(path).getName());
            imageNameLabel.setForeground(accent());
        }
    }

    // Φορτωνει εικονα σε «contain» μεγεθος (ολοκληρη ορατη, κρατα αναλογιες),
    // με υψηλη ποιοτητα κλιμακωσης, για το παραθυρο μεγεθυνσης.
    private ImageIcon loadFitted(String path, int maxW, int maxH) {
        if (path == null || path.isEmpty()) return null;
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            BufferedImage src = ImageIO.read(f);
            if (src == null) return null;
            double sc = Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight());
            if (sc > 1.6) sc = 1.6;   // ηπιο οριο μεγεθυνσης (αποφυγη pixelation)
            int w = Math.max(1, (int) Math.round(src.getWidth()  * sc));
            int h = Math.max(1, (int) Math.round(src.getHeight() * sc));
            return new ImageIcon(highQualityScale(src, w, h));
        } catch (Exception ex) { return null; }
    }

    // Κλιμακωση υψηλης ποιοτητας: προοδευτικη υποδιπλασιαση οταν σμικρυνουμε πολυ,
    // με bicubic interpolation — δινει πιο ευκρινες αποτελεσμα απο μονη bilinear.
    private BufferedImage highQualityScale(BufferedImage src, int tw, int th) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage cur = src;
        while (w > tw * 2 && h > th * 2) {
            w = Math.max(tw, w / 2); h = Math.max(th, h / 2);
            cur = scaleStep(cur, w, h);
        }
        return scaleStep(cur, tw, th);
    }
    private BufferedImage scaleStep(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return dst;
    }

    // Παραθυρο μεγεθυνσης εικονας (κλικ ή Κλεισιμο για να κλεισει)
    private void showImageFull(String path, String titleText) {
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        int mw = (int)(scr.width * 0.72), mh = (int)(scr.height * 0.82);
        ImageIcon big = loadFitted(path, mw, mh);
        if (big == null) { showToast("Δεν φορτωθηκε η εικονα", false); return; }
        JDialog dlg = new JDialog(this, titleText == null ? "Εικονα" : titleText, true);
        JLabel pic = new JLabel(big); pic.setHorizontalAlignment(SwingConstants.CENTER);
        pic.setBackground(new Color(20,20,24)); pic.setOpaque(true);
        pic.setBorder(new EmptyBorder(12,12,12,12));
        pic.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pic.setToolTipText("Κλικ για κλεισιμο");
        pic.addMouseListener(new MouseAdapter(){ @Override public void mouseClicked(MouseEvent e){ dlg.dispose(); } });
        dlg.setContentPane(pic);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // Φορτωνει & κανει cache μια μικρογραφια (cover-fit, κρατα αναλογιες).
    // Επιστρεφει null αν δεν υπαρχει/δεν διαβαζεται το αρχειο.
    private ImageIcon loadThumb(String path, int w, int h) {
        if (path == null || path.isEmpty()) return null;
        String key = path + "@" + w + "x" + h;
        if (thumbCache.containsKey(key)) return thumbCache.get(key);
        ImageIcon result = null;
        try {
            File f = new File(path);
            if (f.exists()) {
                BufferedImage src = ImageIO.read(f);
                if (src != null) {
                    BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = dst.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    double sc = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
                    int sw = (int) Math.round(src.getWidth() * sc), sh = (int) Math.round(src.getHeight() * sc);
                    g2.drawImage(src, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
                    g2.dispose();
                    result = new ImageIcon(dst);
                }
            }
        } catch (Exception ignored) {}
        thumbCache.put(key, result);
        return result;
    }

    // ── Date picker ───────────────────────────────────────────────────────────
    private JButton datePickerButton(JTextField target) {
        JButton b = new JButton(new GlyphIcon(GlyphIcon.Kind.CALENDAR, txtMuted(), 16, 16));
        b.setToolTipText("Επιλογη ημερομηνιας");
        b.setBackground(surface2()); b.setBorder(new LineBorder(border(),1,true));
        b.setFocusPainted(false); b.setContentAreaFilled(true);
        b.setPreferredSize(new Dimension(34, 30));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> showCalendar(b, target));
        return b;
    }
    // Τυλιγει ενα πεδιο ημερομηνιας μαζι με κουμπι ημερολογιου
    private JPanel dateWithPicker(JTextField field) {
        JPanel p = new JPanel(new BorderLayout(4,0)); p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        p.add(field, BorderLayout.CENTER);
        p.add(datePickerButton(field), BorderLayout.EAST);
        return p;
    }
    private void showCalendar(JComponent anchor, JTextField target) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(new LineBorder(border(),1));
        popup.setBackground(surface());
        LocalDate init;
        try { init = LocalDate.parse(target.getText().trim(), DateTimeFormatter.ofPattern("dd/MM/uuuu")); }
        catch (Exception ex) { init = LocalDate.now(); }
        CalendarPanel cal = new CalendarPanel(init, d -> {
            target.setText(d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            popup.setVisible(false);
        });
        popup.add(cal);
        popup.show(anchor, -180, anchor.getHeight());
    }

    // Συμπαγες ημερολογιο επιλογης ημερας
    private class CalendarPanel extends JPanel {
        private YearMonth shown;
        private final java.util.function.Consumer<LocalDate> onPick;
        private final JLabel headerLbl = new JLabel("", SwingConstants.CENTER);
        private final JPanel grid = new JPanel(new GridLayout(0,7,2,2));
        CalendarPanel(LocalDate init, java.util.function.Consumer<LocalDate> onPick) {
            this.shown = YearMonth.from(init); this.onPick = onPick;
            setLayout(new BorderLayout(0,6)); setBackground(surface()); setBorder(new EmptyBorder(8,8,8,8));
            setPreferredSize(new Dimension(230, 230));

            JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
            JButton prev = navBtn("‹"); prev.addActionListener(e -> { shown=shown.minusMonths(1); rebuild(); });
            JButton next = navBtn("›"); next.addActionListener(e -> { shown=shown.plusMonths(1); rebuild(); });
            headerLbl.setFont(new Font("Segoe UI",Font.BOLD,13)); headerLbl.setForeground(txt());
            head.add(prev, BorderLayout.WEST); head.add(headerLbl, BorderLayout.CENTER); head.add(next, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);

            grid.setOpaque(false);
            add(grid, BorderLayout.CENTER);

            JButton today = navBtn("Σημερα");
            today.addActionListener(e -> onPick.accept(LocalDate.now()));
            JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER,0,0)); south.setOpaque(false); south.add(today);
            add(south, BorderLayout.SOUTH);

            rebuild();
        }
        private JButton navBtn(String t) {
            JButton b = new JButton(t); b.setFont(FONT_SMALL); b.setForeground(txt());
            b.setBackground(surface2()); b.setBorder(new EmptyBorder(3,8,3,8));
            b.setFocusPainted(false); b.setContentAreaFilled(true); b.setOpaque(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }
        private void rebuild() {
            headerLbl.setText(shown.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.of("el")) + " " + shown.getYear());
            grid.removeAll();
            String[] dn = {"Δε","Τρ","Τε","Πε","Πα","Σα","Κυ"};
            for (String d : dn) { JLabel l=new JLabel(d,SwingConstants.CENTER); l.setFont(FONT_LABEL); l.setForeground(txtMuted()); grid.add(l); }
            LocalDate first = shown.atDay(1);
            int lead = first.getDayOfWeek().getValue() - 1; // Δευτερα=0
            for (int i=0;i<lead;i++) grid.add(new JLabel(""));
            int days = shown.lengthOfMonth();
            LocalDate todayD = LocalDate.now();
            for (int d=1; d<=days; d++) {
                final LocalDate date = shown.atDay(d);
                JButton b = new JButton(String.valueOf(d));
                b.setFont(FONT_SMALL);
                boolean isToday = date.equals(todayD);
                b.setBackground(isToday ? accent() : surface2());
                b.setForeground(isToday ? Color.WHITE : txt());
                b.setBorder(new EmptyBorder(4,0,4,0)); b.setFocusPainted(false);
                b.setContentAreaFilled(true); b.setOpaque(true);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addActionListener(e -> onPick.accept(date));
                grid.add(b);
            }
            grid.revalidate(); grid.repaint();
        }
    }

    private void refreshTheme() {
        SwingUtilities.invokeLater(()->{getContentPane().removeAll();buildUI();revalidate();repaint();});
    }
    private void showToast(String msg, boolean success) {
        JWindow toast=new JWindow(this);
        JLabel lbl=new JLabel(msg,SwingConstants.CENTER); lbl.setFont(FONT_BODY); lbl.setForeground(Color.WHITE);
        lbl.setBackground(success?new Color(22,163,74):new Color(220,38,38)); lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(10,20,10,20)); toast.add(lbl); toast.pack();
        Point loc=getLocationOnScreen(); toast.setLocation(loc.x+getWidth()/2-toast.getWidth()/2,loc.y+getHeight()-60);
        toast.setVisible(true); new javax.swing.Timer(2200,e->toast.dispose()).start();
    }

    private JLabel formLabel(String t) {
        JLabel l=new JLabel(t); l.setFont(FONT_LABEL); l.setForeground(txtMuted()); l.setAlignmentX(LEFT_ALIGNMENT); return l;
    }
    private JTextField styledField(String ph) {
        JTextField f=new JTextField(); f.setFont(FONT_BODY); f.setBackground(surface2()); f.setForeground(txt()); f.setCaretColor(txt());
        f.setBorder(new CompoundBorder(new LineBorder(border(),1,true),new EmptyBorder(5,8,5,8)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,34)); f.setAlignmentX(LEFT_ALIGNMENT); return f;
    }
    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb=new JComboBox<>(items); cb.setFont(FONT_BODY);
        cb.setBackground(surface2()); cb.setForeground(txt());
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE,34)); cb.setAlignmentX(LEFT_ALIGNMENT);
        cb.setBorder(new LineBorder(border(),1,true));
        // Renderer που ελεγχει το χρωμα τοσο της επιλεγμενης τιμης οσο και της λιστας
        cb.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean isSelected, boolean cellHasFocus) {
                JLabel l=(JLabel)super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
                l.setBorder(new EmptyBorder(4,8,4,8));
                l.setOpaque(true);
                l.setBackground(isSelected ? accent()   : surface2());
                l.setForeground(isSelected ? Color.WHITE : txt());
                return l;
            }
        });
        // Σκουρο βελακι (το default UI του συστηματος μενει ανοιχτοχρωμο)
        cb.setUI(new BasicComboBoxUI(){
            @Override protected JButton createArrowButton(){
                JButton b=new BasicArrowButton(BasicArrowButton.SOUTH,
                        surface2(), surface2(), txtMuted(), surface2());
                b.setBorder(new EmptyBorder(0,0,0,0));
                return b;
            }
            // Το BasicComboBoxUI γεμιζει την περιοχη τιμης με το λευκο default του
            // συστηματος· το αντικαθιστουμε με το χρωμα του θεματος.
            @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus){
                g.setColor(surface2());
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        cb.setBackground(surface2()); cb.setForeground(txt());
        cb.setBorder(new CompoundBorder(new LineBorder(border(),1,true), new EmptyBorder(0,4,0,0)));
        return cb;
    }
    private JButton smallBtn(String text) {
        JButton b=new JButton(text){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                g2.setColor(border());
                g2.draw(new RoundRectangle2D.Float(0.5f,0.5f,getWidth()-1.5f,getHeight()-1.5f,8,8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_SMALL); b.setBackground(surface2()); b.setForeground(txt());
        b.setBorder(new EmptyBorder(5,10,5,10));
        b.setContentAreaFilled(false); b.setOpaque(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addChangeListener(e -> b.setBackground(b.getModel().isRollover() ? border() : surface2()));
        return b;
    }
    private JSeparator separator() {
        JSeparator s=new JSeparator(); s.setForeground(border()); s.setMaximumSize(new Dimension(Integer.MAX_VALUE,1)); s.setAlignmentX(LEFT_ALIGNMENT); return s;
    }
    private Component vgap(int h) { return Box.createRigidArea(new Dimension(0,h)); }

    // Φορμα που γεμιζει το πλατος του viewport και κυλαει μονο κατακορυφα.
    static class ScrollableForm extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 80; }
        public boolean getScrollableTracksViewportWidth()  { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // Οπως η ScrollableForm αλλα με γρηγορο βημα ροδας (ιδιο με το main).
    static class ViewPane extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 48; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 200; }
        public boolean getScrollableTracksViewportWidth()  { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // Διανυσματικα εικονιδια — εμφανιζονται παντα, ανεξαρτητα απο τη γραμματοσειρα.
    static class GlyphIcon implements Icon {
        enum Kind { PENCIL, TRASH, NOTE, HEART, CALENDAR, FLAME, TROPHY, CHECK }
        private final Kind kind; private final Color color; private final int w, h;
        GlyphIcon(Kind kind, Color color, int w, int h) { this.kind=kind; this.color=color; this.w=w; this.h=h; }
        public int getIconWidth()  { return w; }
        public int getIconHeight() { return h; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            // κανονικοποιηση σε καμβα 16x16, κεντραρισμενο
            g2.translate(x + (w - 16) / 2.0, y + (h - 16) / 2.0);
            g2.setColor(color);
            switch (kind) {
                case PENCIL   -> paintPencil(g2);
                case TRASH    -> paintTrash(g2);
                case NOTE     -> paintNote(g2);
                case HEART    -> paintHeart(g2);
                case CALENDAR -> paintCalendar(g2);
                case FLAME    -> paintFlame(g2);
                case TROPHY   -> paintTrophy(g2);
                case CHECK    -> paintCheck(g2);
            }
            g2.dispose();
        }
        private void paintFlame(Graphics2D g2) {
            Path2D f = new Path2D.Double();
            f.moveTo(8.0, 15.0);
            f.curveTo(3.5, 13.5, 3.0, 9.5, 6.0, 6.5);
            f.curveTo(7.0, 5.5, 7.2, 3.5, 6.4, 1.6);
            f.curveTo(9.2, 3.0, 11.2, 5.2, 11.6, 8.2);
            f.curveTo(12.8, 11.2, 11.0, 14.0, 8.0, 15.0);
            f.closePath();
            g2.fill(f);
            g2.setColor(new Color(255,255,255,150));
            Path2D inner = new Path2D.Double();
            inner.moveTo(8.0, 14.0);
            inner.curveTo(6.0, 13.0, 6.0, 10.8, 7.6, 9.2);
            inner.curveTo(8.2, 10.4, 9.6, 10.6, 9.4, 12.2);
            inner.curveTo(9.2, 13.4, 8.6, 13.8, 8.0, 14.0);
            inner.closePath();
            g2.fill(inner);
        }
        private void paintTrophy(Graphics2D g2) {
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D cup = new Path2D.Double();
            cup.moveTo(4.5,2.0); cup.lineTo(11.5,2.0); cup.lineTo(11.0,6.0);
            cup.curveTo(11.0,8.5, 9.6,9.6, 8.0,9.6);
            cup.curveTo(6.4,9.6, 5.0,8.5, 5.0,6.0);
            cup.closePath();
            g2.draw(cup);
            g2.draw(new Arc2D.Double(2.0,2.2,3.4,4.2,90,180,Arc2D.OPEN));
            g2.draw(new Arc2D.Double(10.6,2.2,3.4,4.2,270,180,Arc2D.OPEN));
            g2.draw(new Line2D.Double(8.0,9.6,8.0,11.8));
            g2.draw(new Line2D.Double(6.5,11.8,9.5,11.8));
            g2.draw(new Line2D.Double(6.5,11.8,5.5,13.6));
            g2.draw(new Line2D.Double(9.5,11.8,10.5,13.6));
            g2.draw(new Line2D.Double(5.5,13.6,10.5,13.6));
        }
        private void paintCheck(Graphics2D g2) {
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D ck = new Path2D.Double();
            ck.moveTo(3.0,8.5); ck.lineTo(6.6,12.0); ck.lineTo(13.0,4.0);
            g2.draw(ck);
        }
        private void paintHeart(Graphics2D g2) {
            Path2D h = new Path2D.Double();
            h.moveTo(8.0, 14.0);
            h.curveTo(2.0, 9.8, 1.4, 6.0, 3.8, 4.0);
            h.curveTo(5.6, 2.5, 7.4, 3.4, 8.0, 5.0);
            h.curveTo(8.6, 3.4, 10.4, 2.5, 12.2, 4.0);
            h.curveTo(14.6, 6.0, 14.0, 9.8, 8.0, 14.0);
            h.closePath();
            g2.fill(h);
        }
        private void paintCalendar(Graphics2D g2) {
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new RoundRectangle2D.Double(2.0,3.0,12.0,11.0,2.0,2.0));   // σωμα
            g2.draw(new Line2D.Double(2.0,6.5,14.0,6.5));                      // γραμμη κεφαλιδας
            g2.draw(new Line2D.Double(5.0,1.6,5.0,4.0));                       // κρικος αριστερα
            g2.draw(new Line2D.Double(11.0,1.6,11.0,4.0));                     // κρικος δεξια
            g2.setStroke(new BasicStroke(1.1f));
            g2.fill(new Rectangle2D.Double(4.3,8.4,1.6,1.6));                  // κουκιδες ημερων
            g2.fill(new Rectangle2D.Double(7.2,8.4,1.6,1.6));
            g2.fill(new Rectangle2D.Double(10.1,8.4,1.6,1.6));
            g2.fill(new Rectangle2D.Double(4.3,11.0,1.6,1.6));
            g2.fill(new Rectangle2D.Double(7.2,11.0,1.6,1.6));
        }
        private void paintNote(Graphics2D g2) {
            // σελιδα με τσακισμενη γωνια + γραμμες κειμενου
            Path2D page = new Path2D.Double();
            page.moveTo(3.0,2.0); page.lineTo(10.0,2.0); page.lineTo(13.5,5.5);
            page.lineTo(13.5,14.0); page.lineTo(3.0,14.0); page.closePath();
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(page);
            // τσακισμενη γωνια
            g2.draw(new Line2D.Double(10.0,2.0,10.0,5.5));
            g2.draw(new Line2D.Double(10.0,5.5,13.5,5.5));
            // γραμμες κειμενου
            g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(5.0,8.0,11.5,8.0));
            g2.draw(new Line2D.Double(5.0,10.3,11.5,10.3));
            g2.draw(new Line2D.Double(5.0,12.6,9.0,12.6));
        }
        private void paintPencil(Graphics2D g2) {
            // σωμα μολυβιου (παραλληλογραμμο) κατα μηκος της διαγωνιου
            Path2D body = new Path2D.Double();
            body.moveTo(4.8,13.8); body.lineTo(13.8,4.8); body.lineTo(11.2,2.2); body.lineTo(2.2,11.2); body.closePath();
            g2.fill(body);
            // μυτη (γραφιτης)
            Path2D tip = new Path2D.Double();
            tip.moveTo(2.2,11.2); tip.lineTo(4.8,13.8); tip.lineTo(1.6,14.4); tip.closePath();
            g2.fill(tip);
            // γραμμη που χωριζει το σωμα απο τη μυτη
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255,255,255,150));
            g2.draw(new Line2D.Double(3.3,12.0,3.9,11.4));
        }
        private void paintTrash(Graphics2D g2) {
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // χερουλι καπακιου
            g2.draw(new RoundRectangle2D.Double(6.0,1.6,4.0,1.8,1.4,1.4));
            // χειλος (καπακι)
            g2.draw(new Line2D.Double(2.6,4.3,13.4,4.3));
            // σωμα καδου (trapezoid)
            Path2D bodyT = new Path2D.Double();
            bodyT.moveTo(4.0,4.3); bodyT.lineTo(4.9,14.4); bodyT.lineTo(11.1,14.4); bodyT.lineTo(12.0,4.3);
            g2.draw(bodyT);
            // ραβδωσεις
            g2.draw(new Line2D.Double(6.4,6.4,6.7,12.4));
            g2.draw(new Line2D.Double(8.0,6.4,8.0,12.4));
            g2.draw(new Line2D.Double(9.6,6.4,9.3,12.4));
        }
    }

    static class HoverCard extends JPanel {
        private float alpha=0f;
        HoverCard() {
            setOpaque(true);
            addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){alpha=1;repaint();}
                public void mouseExited(MouseEvent e) {alpha=0;repaint();}
            });
        }
        protected void paintComponent(Graphics g) {
            setBackground(darkMode ? (alpha > 0 ? new Color(42, 42, 54) : new Color(28, 28, 36)) : (alpha > 0 ? new Color(245,244,241) : Color.WHITE));
            super.paintComponent(g);
        }
    }

    static class ChipButton extends JButton {
        final String key; private boolean chipSelected;
        ChipButton(String label, boolean sel) {
            super(label); this.key=label; this.chipSelected=sel;
            setFont(FONT_BODY); setFocusPainted(false); setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setOpaque(true); updateStyle();
        }
        void setChipSelected(boolean s){chipSelected=s;updateStyle();repaint();}
        void updateStyle(){
            if(chipSelected){setBackground(accent());setForeground(Color.WHITE);}
            else{setBackground(surface2());setForeground(txt());}
            setBorder(new EmptyBorder(5,14,5,14));
        }
        protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground()); g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),20,20));
            g2.dispose(); super.paintComponent(g);
        }
    }

    static class RatingPanel extends JPanel {
        private int rating=0; private final JLabel[] stars=new JLabel[5];
        RatingPanel(){
            setOpaque(false); setLayout(new FlowLayout(FlowLayout.LEFT,4,0)); setMaximumSize(new Dimension(Integer.MAX_VALUE,28));
            for(int i=1;i<=5;i++){
                final int val=i; JLabel s=new JLabel("\u2606"); s.setFont(FONT_SYM_L); s.setForeground(starOff());
                s.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                s.addMouseListener(new MouseAdapter(){
                    public void mouseClicked(MouseEvent e){rating=val;updateStars();}
                    public void mouseEntered(MouseEvent e){highlight(val);}
                    public void mouseExited(MouseEvent e){updateStars();}
                });
                stars[i-1]=s; add(s);
            }
            JLabel hint=new JLabel("  κλικ για βαθμολογηση"); hint.setFont(FONT_SMALL); hint.setForeground(txtMuted()); add(hint);
        }
        private void highlight(int u){for(int i=0;i<5;i++){stars[i].setText(i<u?"\u2605":"\u2606");stars[i].setForeground(i<u?starOn():starOff());}}
        private void updateStars(){for(int i=0;i<5;i++){stars[i].setText(i<rating?"\u2605":"\u2606");stars[i].setForeground(i<rating?starOn():starOff());}}
        void setRating(int r){rating=r;updateStars();}
        int  getRating(){return rating;}
        void reset(){rating=0;updateStars();}
    }
}