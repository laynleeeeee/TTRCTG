package src;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class TamiyaTournament1_2 extends JFrame {
    private DefaultListModel<String> racerListModel;
    private JList<String> racerList;
    private JTextField nameField, entriesField, findField;
    private JTable raceTable;
    private DefaultTableModel tableModel;
    private Map<String,Integer> racerEntries = new LinkedHashMap<>();
    private JButton addButton, generateButton, removeButton, findButton, exportButton, recountButton, lockButton;
    private JLabel statusLabel, findCountLabel;
    private Map<Point,String> originalRacerMap = new HashMap<>();
    private boolean locked = false;
    private String currentQuery = "";
    private int hoveredRow = -1;

    public TamiyaTournament1_2() {
        setTitle("Tamiya Tournament Generator - TTRC Moy");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200,700);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10,10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        setContentPane(mainPanel);

        // --- Left Panel: Racers ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(380,0));
        leftPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Racers", TitledBorder.CENTER, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 16)));
        leftPanel.setBackground(new Color(250,250,250));

        racerListModel = new DefaultListModel<>();
        racerList = new JList<>(racerListModel);
        racerList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        leftPanel.add(new JScrollPane(racerList), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new GridLayout(3,2,5,5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        inputPanel.setBackground(new Color(250,250,250));

        nameField = new JTextField(); entriesField = new JTextField();
        addButton = new JButton("Add / Update"); removeButton = new JButton("Remove");

        inputPanel.add(new JLabel("Name:")); inputPanel.add(nameField);
        inputPanel.add(new JLabel("Entries:")); inputPanel.add(entriesField);
        inputPanel.add(addButton); inputPanel.add(removeButton);

        leftPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(leftPanel, BorderLayout.WEST);

        // --- Right Panel: Race Results ---
        JPanel rightPanel = new JPanel(new BorderLayout(10,10));
        rightPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Race Results", TitledBorder.CENTER, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 16)));
        rightPanel.setBackground(new Color(250,250,250));

        // Find panel
        JPanel findPanel = new JPanel(new BorderLayout(5,5));
        findPanel.setBackground(new Color(250,250,250));
        findField = new JTextField(); findButton = new JButton("Find");
        findCountLabel = new JLabel(" "); findCountLabel.setOpaque(true); findCountLabel.setBackground(new Color(245,245,245));
        findCountLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        JPanel findRight = new JPanel(new BorderLayout(5,5)); findRight.setBackground(new Color(250,250,250));
        findRight.add(findButton, BorderLayout.WEST); findRight.add(findCountLabel, BorderLayout.CENTER);
        findPanel.add(findField, BorderLayout.CENTER); findPanel.add(findRight, BorderLayout.EAST);
        rightPanel.add(findPanel, BorderLayout.NORTH);

        // Table setup
        String[] columns = {"Race #","Racer 1","Racer 2","Racer 3"};
        tableModel = new DefaultTableModel(columns,0){
            @Override public boolean isCellEditable(int row,int col){ return col>=1; }
            @Override public Class<?> getColumnClass(int col){ return col>=1?Boolean.class:String.class; }
        };

        raceTable = new JTable(tableModel){
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
                Component c = super.prepareRenderer(renderer,row,column);

                // Alternating row colors + hover
                if(!isRowSelected(row)){
                    c.setBackground(row==hoveredRow ? new Color(220,240,255) : row%2==0?new Color(250,250,250):new Color(240,240,240));
                }

                Point p = new Point(row,column);
                String racer = originalRacerMap.get(p);

                if(racer!=null && !racer.isEmpty() && column>=1){
                    Boolean winner = (Boolean)tableModel.getValueAt(row,column);
                    if(winner!=null && winner) c.setBackground(new Color(200,255,200));
                    // Highlight find matches if not winner
                    if(currentQuery != null && !currentQuery.isEmpty() && racer.toLowerCase().contains(currentQuery) && (winner==null || !winner)){
                        c.setBackground(new Color(255,255,180));
                    }
                }
                if(c instanceof JComponent){
                    ((JComponent)c).setBorder(BorderFactory.createEmptyBorder(2,5,2,5));
                }
                return c;
            }
        };
        raceTable.setRowHeight(28);
        raceTable.setShowGrid(false);
        raceTable.setIntercellSpacing(new Dimension(0,0));
        raceTable.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,14));
        raceTable.getTableHeader().setBackground(new Color(245,245,245));
        raceTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.LIGHT_GRAY));

        // Modern checkbox renderer/editor
        raceTable.setDefaultRenderer(Boolean.class, (table,value,isSelected,hasFocus,row,col)->{
            String racerName = originalRacerMap.get(new Point(row,col));
            if(racerName==null || racerName.isEmpty()) return new JLabel("");
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0)); panel.setOpaque(true);
            Color bg = row==hoveredRow ? new Color(220,240,255) : row%2==0?new Color(250,250,250):new Color(240,240,240);
            panel.setBackground(bg);
            JCheckBox cb = new JCheckBox(racerName); cb.setOpaque(false); cb.setSelected(value!=null && (Boolean)value);
            cb.setFont(new Font("Segoe UI",Font.PLAIN,14));
            if(cb.isSelected()) panel.setBackground(new Color(200,255,200));
            panel.add(cb); return panel;
        });
        raceTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JCheckBox()){
            @Override public Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int column){
                String racerName = originalRacerMap.get(new Point(row,column));
                if(racerName==null || racerName.isEmpty()) return new JLabel("");
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0)); panel.setOpaque(true);
                JCheckBox cb = (JCheckBox)super.getTableCellEditorComponent(table,value,isSelected,row,column);
                cb.setText(racerName); cb.setFont(new Font("Segoe UI",Font.PLAIN,14)); cb.setOpaque(false);
                panel.add(cb);
                Color bg = row==hoveredRow ? new Color(220,240,255) : row%2==0?new Color(250,250,250):new Color(240,240,240);
                if(cb.isSelected()) bg = new Color(200,255,200);
                panel.setBackground(bg);
                return panel;
            }
        });

        raceTable.addMouseMotionListener(new MouseMotionAdapter(){
            public void mouseMoved(MouseEvent e){ int row = raceTable.rowAtPoint(e.getPoint());
                if(row!=hoveredRow){ hoveredRow=row; raceTable.repaint(); }
            }
        });

        JScrollPane tableScroll = new JScrollPane(raceTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        rightPanel.add(tableScroll, BorderLayout.CENTER);

        // Buttons bottom panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,10,5)); bottomPanel.setBackground(new Color(250,250,250));
        generateButton = new JButton("Generate Races"); exportButton = new JButton("Export CSV"); exportButton.setEnabled(false);
        recountButton = new JButton("Recount Winners"); lockButton = new JButton("Lock Generate");
        statusLabel = new JLabel("Ready"); statusLabel.setOpaque(true); statusLabel.setBackground(new Color(245,245,245)); statusLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JButton[] buttons = {addButton, removeButton, generateButton, exportButton, recountButton, lockButton, findButton};
        for(JButton b:buttons) styleButton(b);

        bottomPanel.add(generateButton); bottomPanel.add(exportButton);
        bottomPanel.add(recountButton); bottomPanel.add(lockButton); bottomPanel.add(statusLabel);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(rightPanel, BorderLayout.CENTER);

        // Listeners
        racerList.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                String selected = racerList.getSelectedValue();
                if(selected!=null){
                    String[] parts = selected.split(" - ");
                    if(parts.length==2){ nameField.setText(parts[0]); entriesField.setText(parts[1].replace(" entries","")); }
                }
            }
        });
        addButton.addActionListener(e->onAddUpdate());
        removeButton.addActionListener(e->onRemove());
        generateButton.addActionListener(e->{ onGenerate(); adjustTableColumnWidths(raceTable); });
        exportButton.addActionListener(e->exportCSV());
        findButton.addActionListener(e->findInTable());
        recountButton.addActionListener(e->recountWinners());
        lockButton.addActionListener(e->toggleLock());

        nameField.addActionListener(e->addButton.doClick());
        entriesField.addActionListener(e->addButton.doClick());
        findField.addActionListener(e->findButton.doClick());

        setupButtonKeyBindings(addButton); setupButtonKeyBindings(removeButton);
        setupButtonKeyBindings(generateButton); setupButtonKeyBindings(findButton);
        setupButtonKeyBindings(exportButton); setupButtonKeyBindings(recountButton);
        setupButtonKeyBindings(lockButton);

        tableModel.addTableModelListener(e -> { // enforce one winner
            int row = e.getFirstRow(); int col = e.getColumn();
            if(col>=1 && col<=3){
                Boolean checked = (Boolean)tableModel.getValueAt(row,col);
                if(checked!=null && checked){
                    for(int c=1;c<=3;c++) if(c!=col) tableModel.setValueAt(false,row,c);
                }
            }
        });

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        raceTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
    }

    private void styleButton(JButton btn){
        btn.setFocusPainted(false); btn.setBackground(new Color(230,230,230)); btn.setForeground(Color.BLACK);
        btn.setBorder(BorderFactory.createEmptyBorder(5,15,5,15));
        btn.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){ btn.setBackground(new Color(200,200,255)); }
            public void mouseExited(MouseEvent e){ btn.setBackground(new Color(230,230,230)); }
        });
    }

    private void toggleLock(){ locked=!locked; lockButton.setText(locked?"Unlock Generate":"Lock Generate"); generateButton.setEnabled(!locked); }

    private void onAddUpdate(){
        String name = nameField.getText().trim();
        int entries;
        try{ entries=Integer.parseInt(entriesField.getText().trim()); }
        catch(Exception ex){ JOptionPane.showMessageDialog(this,"Enter valid number"); return; }
        if(name.isEmpty()){ JOptionPane.showMessageDialog(this,"Enter name"); return; }
        if(entries<1) racerEntries.remove(name); else racerEntries.put(name,entries);
        refreshRacerList(); nameField.setText(""); entriesField.setText("");
    }

    private void onRemove(){
        String selected = racerList.getSelectedValue();
        if(selected!=null){ String name = selected.split(" - ")[0]; racerEntries.remove(name); refreshRacerList(); }
    }

    private void refreshRacerList(){ racerListModel.clear();
        for(Map.Entry<String,Integer> e:racerEntries.entrySet()) racerListModel.addElement(e.getKey()+" - "+e.getValue()+" entries"); }

    private void onGenerate(){
        if(locked){ JOptionPane.showMessageDialog(this,"Generate is locked!"); return; }
        if(racerEntries.isEmpty()){ JOptionPane.showMessageDialog(this,"No racers!"); return; }

        // Clear table
        tableModel.setRowCount(0); originalRacerMap.clear();

        // Build list of entries
        List<String> entriesList = new ArrayList<>();
        for(Map.Entry<String,Integer> e:racerEntries.entrySet())
            for(int i=0;i<e.getValue();i++) entriesList.add(e.getKey());

        Collections.shuffle(entriesList);
        int raceNum=1; int idx=0;
        while(idx<entriesList.size()){
            Object[] row = new Object[4]; row[0]="Race "+raceNum;
            for(int c=1;c<=3;c++){
                if(idx<entriesList.size()){ row[c]=false; originalRacerMap.put(new Point(tableModel.getRowCount(),c),entriesList.get(idx)); idx++; }
                else row[c]=null;
            }
            tableModel.addRow(row); raceNum++;
        }

        adjustTableColumnWidths(raceTable);
        exportButton.setEnabled(true);
        statusLabel.setText("Races generated.");
    }

    private void findInTable(){
        currentQuery = findField.getText().trim().toLowerCase();
        int count=0;
        for(int row=0; row<tableModel.getRowCount(); row++)
            for(int col=1; col<=3; col++){
                String racer = originalRacerMap.get(new Point(row,col));
                if(racer!=null && racer.toLowerCase().contains(currentQuery)) count++;
            }
        findCountLabel.setText(count==0?"No matches":count+" matches found");
        raceTable.repaint();
    }

    private void recountWinners(){
        for(Map.Entry<String,Integer> e:racerEntries.entrySet()) racerEntries.put(e.getKey(),0);
        for(int row=0; row<tableModel.getRowCount(); row++)
            for(int col=1; col<=3; col++){
                Boolean winner = (Boolean)tableModel.getValueAt(row,col);
                String racer = originalRacerMap.get(new Point(row,col));
                if(winner!=null && winner && racer!=null) racerEntries.put(racer,racerEntries.getOrDefault(racer,0)+1);
            }
        refreshRacerList();
        statusLabel.setText("Winner entries recounted.");
    }

    private void exportCSV(){
        JFileChooser fc = new JFileChooser(); int ret = fc.showSaveDialog(this);
        if(ret!=JFileChooser.APPROVE_OPTION) return;
        try(FileWriter fw=new FileWriter(fc.getSelectedFile()+".csv")){
            for(int row=0; row<tableModel.getRowCount(); row++){
                for(int col=0; col<tableModel.getColumnCount(); col++){
                    String val="";
                    if(col==0) val=tableModel.getValueAt(row,col).toString();
                    else{
                        String racer = originalRacerMap.get(new Point(row,col));
                        Boolean winner = (Boolean)tableModel.getValueAt(row,col);
                        if(racer!=null) val = racer+(winner!=null && winner?" (Winner)":"");
                    }
                    fw.write(val+(col<tableModel.getColumnCount()-1?",":""));
                }
                fw.write("\n");
            }
            statusLabel.setText("CSV exported.");
        } catch(IOException ex){ JOptionPane.showMessageDialog(this,"Error exporting CSV: "+ex.getMessage()); }
    }

    private void setupButtonKeyBindings(JButton btn){
        btn.registerKeyboardAction(e->btn.doClick(),KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),JComponent.WHEN_FOCUSED);
    }

    private void adjustTableColumnWidths(JTable table){
        for(int col=0; col<table.getColumnCount(); col++){
            int maxWidth = 50;
            TableColumn column = table.getColumnModel().getColumn(col);
            for(int row=0; row<table.getRowCount(); row++){
                Component comp = table.prepareRenderer(table.getCellRenderer(row,col),row,col);
                maxWidth = Math.max(maxWidth, comp.getPreferredSize().width+10);
            }
            column.setPreferredWidth(maxWidth);
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(()->new TamiyaTournament1_2().setVisible(true));
    }
}
