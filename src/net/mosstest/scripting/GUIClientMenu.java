package net.mosstest.scripting;

import net.mosstest.launcher.SingleplayerBindingBean;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class GUIClientMenu {
    private JTabbedPane tabbedPane1;
    private JPanel panel1;
    private JPanel panel2;
    private JButton playButton;
    private JButton newButton;
    private JButton createButton;
    private JButton deleteButton;
    private JButton quitButton;
    private JTable table1;


    public void setData(SingleplayerBindingBean data) {
    }

    public void getData(SingleplayerBindingBean data) {
    }

    public boolean isModified(SingleplayerBindingBean data) {
        return false;
    }


    class SingleplayerListTableModel extends AbstractTableModel {

        /**
         * The column names.
         */
        private String[] columnNames = {
                Messages.getString("GUIClientsideLauncher.COL_WORLD_NAME"), Messages.getString("GUIClientsideLauncher.COL_WORLD_DESC"), //$NON-NLS-1$ //$NON-NLS-2$
                Messages.getString("GUIClientsideLauncher.COL_GAME_PRESET")}; //$NON-NLS-1$

        /**
         * The entries.
         */
        private ArrayList<SingleplayerListEntry> entries = new ArrayList<>();

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        public int getColumnCount() {
            return this.columnNames.length;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public int getRowCount() {
            return this.entries.size();
        }

        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#getColumnName(int)
         */
        public String getColumnName(int col) {
            return this.columnNames[col];
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        public Object getValueAt(int row, int col) {
            SingleplayerListEntry entry = this.entries.get(row);
            switch (col) {
                case 0:
                    return entry.name;
                case 1:
                    return entry.description;
                case 2:
                    return entry.gamePreset;
                default:
                    return null;
            }
        }

        /*
         * All entries are strings *at the moment*.
         */
        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
		 */
        public Class<?> getColumnClass(int c) {
            return String.class;
        }

        /*
         * Don't need to implement this method unless your table's editable.
         */
        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
		 */
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        /**
         * Instantiates a new singleplayer list table model.
         *
         * @param entries the entries
         */
        public SingleplayerListTableModel(
                ArrayList<SingleplayerListEntry> entries) {
            this.entries = entries;
        }
    }

    public static class SingleplayerListEntry {

        /**
         * The name.
         */
        public String name;

        /**
         * The description.
         */
        public String description;

        /**
         * The game preset.
         */
        public String gamePreset;

        /**
         * Instantiates a new singleplayer list entry.
         *
         * @param name        the name
         * @param description the description
         * @param gamePreset  the game preset
         */
        public SingleplayerListEntry(String name, String description,
                                     String gamePreset) {
            this.name = name;
            this.description = description;
            this.gamePreset = gamePreset;
        }

    }

}


