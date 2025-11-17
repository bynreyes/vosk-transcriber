/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.example.asr.gui;

import com.example.asr.vosk.VoskService;
import com.example.asr.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana principal de la aplicación.
 * <p>
 * Orquesta la sesión en vivo de transcripción desde micrófono.
 * Gestiona el ciclo de vida del servicio Vosk.
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 */
public class MainFrame extends javax.swing.JFrame {
    
    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
    private static VoskService voskService;

    private LiveSessionPanel livePanel;
    private JLabel statusLabel;

    /**
     * Creates new form MainFrame
     */
    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    }

    public MainFrame() {
        initComponents();
        this.setLayout(new BorderLayout());
        this.setBackground(AppConfig.COLOR_BG_2);

        // Toolbar superior
        JPanel topBar = buildToolbar();
        this.add(topBar, BorderLayout.NORTH);

        // Panel de sesión en vivo
        livePanel = new LiveSessionPanel(this);
        this.add(livePanel, BorderLayout.CENTER);

        this.setSize(480, 420);
        this.setLocationRelativeTo(null);
        this.setTitle("Vosk Transcriber");

        // Manejo de cierre
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeApplication();
            }
        });

        // Inicializar Vosk en background
        initVoskAsync();
    }

    /**
     * Construye el toolbar con botones de control.
     */
    private JPanel buildToolbar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(AppConfig.COLOR_BG_1);
        topBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // Label de estado
        statusLabel = new JLabel("Inicializando Vosk...");
        statusLabel.setForeground(AppConfig.COLOR_TEXT);
        topBar.add(statusLabel, BorderLayout.CENTER);

        // Botones de control
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(AppConfig.COLOR_BG_1);

        JButton btnClear = createButton("Limpiar", e -> livePanel.clearTranscription());
        JButton btnSave = createButton("Guardar", e -> saveSession());
        JButton btnExit = createButton("Salir", e -> closeApplication());

        buttons.add(btnClear);
        buttons.add(btnSave);
        buttons.add(btnExit);
        topBar.add(buttons, BorderLayout.EAST);

        return topBar;
    }

    /**
     * Crea un botón con estilos consistentes.
     */
    private JButton createButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setBackground(AppConfig.COLOR_ACCENT);
        button.setForeground(AppConfig.COLOR_TEXT);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        button.addActionListener(listener);
        return button;
    }

    /**
     * Guarda la sesión actual con nombre personalizado.
     */
    private void saveSession() {
        String name = JOptionPane.showInputDialog(this,
            "Nombre de la sesión:",
            "Guardar sesión",
            JOptionPane.PLAIN_MESSAGE);

        if (name != null && !name.trim().isEmpty()) {
            boolean ok = livePanel.saveSession(name.trim());
            if (ok) {
                JOptionPane.showMessageDialog(this,
                    "Sesión guardada: " + name,
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "No se pudo guardar la sesión.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Cierra la aplicación de forma ordenada.
     */
    private void closeApplication() {
        try {
            livePanel.stopSession();
        } catch (Exception e) {
            logger.warn("Error deteniendo sesión", e);
        }

        if (voskService != null) {
            try {
                logger.info("Cerrando VoskService desde GUI...");
                voskService.close();
                voskService = null;
            } catch (Exception e) {
                logger.error("Error cerrando VoskService", e);
            }
        }

        dispose();
        System.exit(0);
    }

    /**
     * Inicializa Vosk en un hilo background.
     */
    private void initVoskAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    getVoskService();
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Vosk listo"));
                } catch (Exception e) {
                    logger.error("No se pudo inicializar Vosk", e);
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Error inicializando Vosk");
                        JOptionPane.showMessageDialog(MainFrame.this,
                            "Error inicializando Vosk: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
                return null;
            }
        };
        worker.execute();
    }

    /**
     * Obtiene la instancia única de VoskService (lazy initialization).
     */
    public static VoskService getVoskService() throws Exception {
        if (voskService == null) {
            synchronized (MainFrame.class) {
                if (voskService == null) {
                    voskService = VoskService.getInstance();
                }
            }
        }
        return voskService;
    }

    public static void main(String... args) {
        // Set Nimbus look and feel
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.warn("No se pudo establecer look and feel", ex);
        }

        java.awt.EventQueue.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
