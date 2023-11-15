/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.taskmanagerapp;

/**
 *
 * @author Liam Vernon
 */

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.*;
import java.util.Vector;


public class TaskManagerApp{
    private Connection connection;
    private JTextField taskField;
    private JTextField descriptionField;
    private JCheckBox completionStatusCheckbox;
    private JTextField categoryField;
    private DefaultTableModel taskTableModel;
    private JTable taskTable;

    public TaskManagerApp(){
        initializeUI();
        connectToDatabase();
        createTableIfNotExists();
    }

    private void initializeUI(){
        JFrame frame = new JFrame("Task Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        taskTableModel = new DefaultTableModel();
        taskTable = new JTable(taskTableModel);
        JScrollPane scrollPane = new JScrollPane(taskTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        taskField = new JTextField();
        JButton addButton = new JButton("Add Task");
        JButton openButton = new JButton("Open CSV");
        JButton saveButton = new JButton("Save CSV");
        JButton searchButton = new JButton("Search");
        
        taskField = new JTextField();
        descriptionField = new JTextField();
        completionStatusCheckbox = new JCheckBox("Completion Status");
        categoryField = new JTextField();

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String taskName = taskField.getText();
                String description = descriptionField.getText();
                boolean completionStatus = completionStatusCheckbox.isSelected();
                String category = categoryField.getText();
                Vector<Object> newRowData = new Vector<>();
                newRowData.add(taskName);
                newRowData.add(description);
                newRowData.add(completionStatus);
                newRowData.add(category);
                taskTableModel.addRow(newRowData);
                taskField.setText("");
                descriptionField.setText("");
                completionStatusCheckbox.setSelected(false);
                categoryField.setText("");
            }
        });

        openButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                openCSV();
            }
        });

        saveButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                saveCSV();
            }
        });

        searchButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String keyword = JOptionPane.showInputDialog(frame, "Enter keyword to search:");
                if (keyword != null && !keyword.trim().isEmpty()) {
                    searchTasks(keyword);
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(taskField);
        buttonPanel.add(addButton);
        buttonPanel.add(openButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(searchButton);
        panel.add(buttonPanel, BorderLayout.NORTH);

        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }

    private void connectToDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/mysql?zeroDateTimeBehavior=CONVERT_TO_NULL");
        } catch (Exception e){
        }
    }

    private void createTableIfNotExists() {
        try {
            Statement stmt = connection.createStatement();
            String createTableSQL = "CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)";
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
        }
    }
    private void addTask(String taskName, String description, boolean completionStatus, String category) {
    if (!taskName.isEmpty()) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO tasks (name, description, completion_status, category) VALUES (?, ?, ?, ?)");
            preparedStatement.setString(1, taskName);
            preparedStatement.setString(2, description);
            preparedStatement.setBoolean(3, completionStatus);
            preparedStatement.setString(4, category);
            preparedStatement.executeUpdate();
        } catch (SQLException e){
        }
    }
}
    private void refreshTaskTable(){
    taskTableModel.setRowCount(0);
    try {
        Statement stmt = connection.createStatement();
        ResultSet resultSet = stmt.executeQuery("SELECT name, description, completion_status, category FROM tasks");
        while (resultSet.next()) {
            String taskName = resultSet.getString("name");
            String description = resultSet.getString("description");
            boolean completionStatus = resultSet.getBoolean("completion_status");
            String category = resultSet.getString("category");
            taskTableModel.addRow(new Object[]{taskName, description, completionStatus, category});
        }
    } catch (SQLException e){
    }
}

    private void openCSV(){
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadCSV(selectedFile);
        }
    }

    private void loadCSV(File file){
        try (BufferedReader reader = new BufferedReader(new FileReader(file))){
            String line;
            Vector<String> columnNames = new Vector<>();
            Vector<Vector<String>> data = new Vector<>();

            if ((line = reader.readLine()) != null){
                String[] columns = line.split(",");
                for (String column : columns){
                    columnNames.add(column);
                }
            }

            while ((line = reader.readLine()) != null){
                String[] values = line.split(",");
                Vector<String> rowData = new Vector<>();
                for (String value : values){
                    rowData.add(value);
                }
                data.add(rowData);
            }
            taskTableModel.setDataVector(data, columnNames);
        } catch (IOException e){
        }
    }

    private void saveCSV(){
    JFileChooser fileChooser = new JFileChooser();
    int returnValue = fileChooser.showSaveDialog(null);
    if (returnValue == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        String filePath = selectedFile.getAbsolutePath();

        if (!filePath.endsWith(".csv")) {
            selectedFile = new File(filePath + ".csv");
        }
        
        saveCSV(selectedFile);
    }
}

    private void saveCSV(File file){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))){
            for (int i = 0; i < taskTableModel.getColumnCount(); i++){
                writer.write(taskTableModel.getColumnName(i));
                if (i < taskTableModel.getColumnCount() - 1) {
                    writer.write(",");
                }
            }
            writer.newLine();
            for (int row = 0; row < taskTableModel.getRowCount(); row++){
                for (int col = 0; col < taskTableModel.getColumnCount(); col++){
                    writer.write(taskTableModel.getValueAt(row, col).toString());
                    if (col < taskTableModel.getColumnCount() - 1){
                        writer.write(",");
                    }
                }
                writer.newLine();
            }
        } catch (IOException e){
        }
    }

    private void searchTasks(String keyword){
    if (keyword.isEmpty()) {
        refreshTaskTable();
    } else {
        DefaultTableModel filteredModel = new DefaultTableModel();
        for (int col = 0; col < taskTableModel.getColumnCount(); col++){
            filteredModel.addColumn(taskTableModel.getColumnName(col));
        }

        for (int row = 0; row < taskTableModel.getRowCount(); row++){
            boolean containsKeyword = false;
            for (int col = 0; col < taskTableModel.getColumnCount(); col++){
                Object cellValue = taskTableModel.getValueAt(row, col);
                if (cellValue != null && cellValue.toString().contains(keyword)){
                    containsKeyword = true;
                    break;
                }
            }
            if (containsKeyword){
                Vector<Object> rowData = new Vector<>();
                for (int col = 0; col < taskTableModel.getColumnCount(); col++) {
                    rowData.add(taskTableModel.getValueAt(row, col));
                }
                filteredModel.addRow(rowData);
            }
        }
        taskTable.setModel(filteredModel);
    }
}
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run() {
                new TaskManagerApp();
            }
        });
    }
}
