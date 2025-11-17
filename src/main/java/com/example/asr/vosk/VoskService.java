package com.example.asr.vosk;

import com.example.asr.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Servicio singleton que gestiona la inicialización y ciclo de vida del modelo Vosk.
 * <p>
 * Este servicio es responsable de:
 * <ul>
 *   <li>Cargar el modelo de Vosk una sola vez (patrón singleton)</li>
 *   <li>Crear instancias de Recognizer para el procesamiento de audio</li>
 *   <li>Liberar recursos al cerrar (implementa AutoCloseable)</li>
 * </ul>
 * </p>
 * <p>
 * Carga la ruta del modelo desde {@link AppConfig#VOSK_MODEL_PATH}.
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 * @see VoskService#getInstance()
 * @see org.vosk.Recognizer
 */
public class VoskService implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(VoskService.class);

    /** Instancia única del servicio (singleton) - volatile para visibilidad entre threads */
    private static volatile VoskService instance;

    /** Tasa de muestreo estándar para Vosk (16 kHz) */
    private static final float SAMPLE_RATE = AppConfig.AUDIO_SAMPLE_RATE;

    /** Modelo de Vosk cargado en memoria */
    private Model model;

    /**
     * Obtiene la instancia única del servicio Vosk (patrón Singleton).
     * <p>
     * Usa double-checked locking para evitar condiciones de carrera.
     * Si el servicio aún no ha sido inicializado, carga el modelo automáticamente.
     * </p>
     *
     * @return la instancia única de VoskService
     * @throws IOException si no se puede cargar el modelo
     */
    public static VoskService getInstance() throws IOException {
        if (instance == null) {
            synchronized (VoskService.class) {
                if (instance == null) {
                    instance = new VoskService();
                }
            }
        }
        return instance;
    }

    /**
     * Constructor privado que inicializa el modelo Vosk.
     * <p>
     * El modelo debe estar descargado en la ruta especificada por {@link AppConfig#VOSK_MODEL_PATH}.
     * </p>
     *
     * @throws IOException si no se puede cargar el modelo o la ruta no existe
     */
    private VoskService() throws IOException {
        this(AppConfig.VOSK_MODEL_PATH);
    }

    /**
     * Constructor privado que permite especificar ruta personalizada del modelo.
     *
     * @param modelPath ruta al directorio raíz del modelo Vosk
     * @throws IOException si no se puede cargar el modelo o la ruta no existe
     */
    private VoskService(String modelPath) throws IOException {
        logger.info("Inicializando Vosk Service...");

        // Validar que existe el modelo
        Path path = Paths.get(modelPath);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            String errorMsg = String.format(
                "Modelo Vosk no encontrado en: %s\n" +
                "Por favor descarga un modelo desde: https://alphacephei.com/vosk/models\n" +
                "Y colócalo en la ruta especificada.",
                path.toAbsolutePath()
            );
            logger.error(errorMsg);
            throw new IOException(errorMsg);
        }

        try {
            // Cargar modelo
            logger.info("Cargando modelo desde: {}", path.toAbsolutePath());
            this.model = new Model(modelPath);
            logger.info("Modelo Vosk cargado exitosamente");

        } catch (Exception e) {
            logger.error("Error al cargar el modelo Vosk", e);
            throw new IOException("No se pudo cargar el modelo Vosk: " + e.getMessage(), e);
        }
    }

    /**
     * Crea un nuevo reconocedor (Recognizer) configurado para la tasa de muestreo estándar.
     * <p>
     * El reconocedor devuelto es independiente y debe ser cerrado después de su uso.
     * </p>
     *
     * @return nueva instancia de Recognizer lista para aceptar audio
     * @throws IOException si no se puede crear el Recognizer
     *
     * @see Recognizer#close()
     */
    public Recognizer createRecognizer() throws IOException {
        return createRecognizer(SAMPLE_RATE);
    }

    /**
     * Crea un nuevo reconocedor con una tasa de muestreo específica.
     * <p>
     * La tasa de muestreo debe coincidir con el audio que será procesado.
     * </p>
     *
     * @param sampleRate tasa de muestreo en Hz (ej: 16000.0f para 16 kHz)
     * @return nueva instancia de Recognizer configurada con la tasa especificada
     * @throws IOException si no se puede crear el Recognizer
     * @throws IllegalArgumentException si la tasa de muestreo no es válida
     *
     * @see Recognizer#close()
     */
    public Recognizer createRecognizer(float sampleRate) throws IOException {
        logger.debug("Creando Recognizer con sample rate: {} Hz", sampleRate);
        try {
            return new Recognizer(model, sampleRate);
        } catch (IOException e) {
            logger.error("Error creando Recognizer", e);
            throw e;
        } catch (Exception e) {
            logger.error("Excepción inesperada al crear Recognizer", e);
            throw new IOException("No se pudo crear Recognizer: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el modelo Vosk cargado.
     *
     * @return modelo Vosk
     */
    public Model getModel() {
        return model;
    }

    /**
     * Libera recursos del servicio Vosk (AutoCloseable).
     */
    @Override
    public void close() {
        if (model != null) {
            try {
                model.close();
                logger.info("Modelo Vosk cerrado");
            } catch (Exception e) {
                logger.error("Error cerrando modelo Vosk", e);
            } finally {
                model = null;
                // ⭐ CRÍTICO: Reset del singleton para permitir reinicialización
                synchronized (VoskService.class) {
                    instance = null;
                }
                logger.info("VoskService singleton resetado - puede reinicializarse");
            }
        }
    }
}
