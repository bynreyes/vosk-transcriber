package com.example.asr.config;

import java.awt.Color;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuración centralizada de la aplicación.
 * <p>
 * Carga todas las configuraciones desde `application.yml`.
 * Proporciona acceso type-safe a las propiedades con valores por defecto.
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 */
public final class AppConfig {

    // ==================== RUTAS ====================
    /** Ruta al directorio raíz del proyecto */
    public static final Path PROJECT_ROOT = Paths.get(".").toAbsolutePath().normalize();

    /** Ruta al modelo Vosk */
    public static final String VOSK_MODEL_PATH = ConfigLoader.getString(
        "paths.model",
        "./models/vosk-model-es-0.42"
    );

    /** Ruta al directorio de transcripciones */
    public static final Path TRANSCRIPTIONS_DIR = PROJECT_ROOT.resolve(
        ConfigLoader.getString("paths.transcriptions", "transcriptions")
    );

    /** Nombre del archivo de transcripción en vivo por defecto */
    public static final String DEFAULT_LIVE_TRANSCRIPTION_FILE = ConfigLoader.getString(
        "paths.defaultLiveFile",
        "live_transcription.txt"
    );

    // ==================== COLORES - PALETA ====================

    /** Color de fondo primario */
    public static final Color COLOR_BG_1 = parseColor(
        ConfigLoader.getString("ui.colors.bg1", "212,224,155")
    );

    /** Color de fondo secundario */
    public static final Color COLOR_BG_2 = parseColor(
        ConfigLoader.getString("ui.colors.bg2", "246,244,210")
    );

    /** Color de fondo terciario */
    public static final Color COLOR_BG_3 = parseColor(
        ConfigLoader.getString("ui.colors.bg3", "203,223,189")
    );

    /** Color de acento (botones) */
    public static final Color COLOR_ACCENT = parseColor(
        ConfigLoader.getString("ui.colors.accent", "241,156,121")
    );

    /** Color de texto */
    public static final Color COLOR_TEXT = parseColor(
        ConfigLoader.getString("ui.colors.text", "70,63,58")
    );

    // ==================== AUDIO ====================
    /** Tasa de muestreo de audio (16 kHz - requerida por Vosk) */
    public static final float AUDIO_SAMPLE_RATE = (float) ConfigLoader.getDouble(
        "audio.sampleRate",
        16000.0
    );

    /** Profundidad de bits de audio (16 bits - requerida por Vosk) */
    public static final int AUDIO_SAMPLE_SIZE_BITS = ConfigLoader.getInt(
        "audio.sampleBits",
        16
    );

    /** Número de canales de audio (mono - requerida por Vosk) */
    public static final int AUDIO_CHANNELS = ConfigLoader.getInt(
        "audio.channels",
        1
    );

    /** Tamaño del buffer de lectura de audio */
    public static final int AUDIO_BUFFER_SIZE = ConfigLoader.getInt(
        "audio.bufferSize",
        4096
    );

    // ==================== UI ====================
    /** Ancho de la ventana principal */
    public static final int WINDOW_WIDTH = ConfigLoader.getInt(
        "ui.windowWidth",
        480
    );

    /** Alto de la ventana principal */
    public static final int WINDOW_HEIGHT = ConfigLoader.getInt(
        "ui.windowHeight",
        420
    );

    /** Título de la aplicación */
    public static final String APP_TITLE = ConfigLoader.getString(
        "ui.title",
        "Vosk Transcriber"
    );

    // ==================== TIMEOUT ====================
    /** Timeout máximo para esperar a que SwingWorker termine (segundos) */
    public static final int SWING_WORKER_TIMEOUT_SECONDS = ConfigLoader.getInt(
        "timeouts.swingWorkerSeconds",
        5
    );

    /** Timeout para inicializar Vosk (segundos) */
    public static final int VOSK_INIT_TIMEOUT_SECONDS = ConfigLoader.getInt(
        "timeouts.voskInitSeconds",
        10
    );

    /** Sleep cuando no hay datos del micrófono (milisegundos) */
    public static final int MICROPHONE_READ_SLEEP_MS = ConfigLoader.getInt(
        "timeouts.microphoneReadMs",
        10
    );

    // ==================== TRANSCRIPCIÓN ====================
    /** Guardar automáticamente en archivo */
    public static final boolean TRANSCRIPTION_AUTO_APPEND = ConfigLoader.getBoolean(
        "transcription.autoAppend",
        true
    );

    /** Mostrar resultados parciales */
    public static final boolean SHOW_PARTIAL_RESULTS = ConfigLoader.getBoolean(
        "transcription.showPartialResults",
        true
    );

    /** Guardar resultado final */
    public static final boolean SAVE_FINAL_RESULT = ConfigLoader.getBoolean(
        "transcription.saveFinalResult",
        true
    );

    /**
     * Constructor privado - clase de configuración pura (no instanciable).
     */
    private AppConfig() {
        throw new AssertionError("AppConfig no debería ser instanciada");
    }

    /**
     * Parsea un color en formato "R,G,B" a java.awt.Color.
     *
     * @param colorString formato "R,G,B" ej: "212,224,155"
     * @return Color parseado o Color.BLACK si el formato es inválido
     */
    private static Color parseColor(String colorString) {
        try {
            String[] parts = colorString.split(",");
            if (parts.length != 3) {
                return Color.BLACK;
            }
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            return new Color(r, g, b);
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }
}
