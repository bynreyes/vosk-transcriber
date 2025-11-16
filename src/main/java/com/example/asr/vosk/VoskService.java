package com.example.asr.vosk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Servicio que gestiona la inicialización y ciclo de vida del modelo Vosk
 * y los reconocedores (Recognizer).
 */
public class VoskService implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(VoskService.class);
    
    // Configuración del modelo
    // Ajustado para usar el modelo incluido en el repositorio
    private static final String DEFAULT_MODEL_PATH = "./models/vosk-model-es-0.42";
    private static final float SAMPLE_RATE = 16000.0f;
    
    private Model model;
    
    /**
     * Constructor que inicializa el modelo Vosk.
     * El modelo debe estar descargado en la ruta especificada.
     * 
     * @throws IOException si no se puede cargar el modelo
     */
    public VoskService() throws IOException {
        this(DEFAULT_MODEL_PATH);
    }
    
    /**
     * Constructor que permite especificar ruta personalizada del modelo.
     * 
     * @param modelPath ruta al directorio del modelo Vosk
     * @throws IOException si no se puede cargar el modelo
     */
    public VoskService(String modelPath) throws IOException {
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
     * 
     * @return instancia de Recognizer lista para aceptar audio
     * @throws IOException si no se puede crear el Recognizer
     */
    public Recognizer createRecognizer() throws IOException {
        return createRecognizer(SAMPLE_RATE);
    }
    
    /**
     * Crea un nuevo reconocedor con una tasa de muestreo específica.
     * 
     * @param sampleRate tasa de muestreo en Hz (debe coincidir con el audio)
     * @return instancia de Recognizer
     * @throws IOException si no se puede crear el Recognizer
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
     * Obtiene la tasa de muestreo estándar configurada.
     * 
     * @return tasa de muestreo en Hz
     */
    public float getSampleRate() {
        return SAMPLE_RATE;
    }
    
    /**
     * Cierra el modelo y libera recursos nativos.
     */
    @Override
    public void close() {
        if (model != null) {
            logger.info("Cerrando modelo Vosk...");
            model.close();
            model = null;
        }
    }
}
