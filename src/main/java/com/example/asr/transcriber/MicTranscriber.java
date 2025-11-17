package com.example.asr.transcriber;

import com.example.asr.config.AppConfig;
import com.example.asr.exception.TranscriptionException;
import com.example.asr.vosk.VoskService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * Transcriptor de audio en tiempo real desde micrófono.
 * <p>
 * Implementa la interfaz {@link Transcriber} para proporcionar transcripción
 * desde el micrófono del sistema en tiempo real.
 * Captura audio continuamente del micrófono usando Java Sound API
 * y lo procesa en tiempo real con el motor de reconocimiento Vosk.
 * </p>
 * <p>
 * Características:
 * <ul>
 *   <li>Captura de audio en 16 kHz mono (configurado en application.yml)</li>
 *   <li>Reconocimiento en tiempo real con resultados parciales y finales</li>
 *   <li>Soporte para callbacks personalizados</li>
 *   <li>Inicio/parada segura desde múltiples threads</li>
 * </ul>
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 * @see Transcriber
 * @see VoskService
 * @see AppConfig
 */
public class MicTranscriber implements Transcriber {
    private static final Logger logger = LoggerFactory.getLogger(MicTranscriber.class);
    
    private final VoskService voskService;
    
    // Configuración de audio desde AppConfig
    /** Tasa de muestreo: 16 kHz (requerida por Vosk) */
    private static final float SAMPLE_RATE = AppConfig.AUDIO_SAMPLE_RATE;
    /** Profundidad de bits: 16 bits (requerida por Vosk) */
    private static final int SAMPLE_SIZE_BITS = AppConfig.AUDIO_SAMPLE_SIZE_BITS;
    /** Canales de audio: mono (requerida por Vosk) */
    private static final int CHANNELS = AppConfig.AUDIO_CHANNELS;
    /** Formato signed: true (requerida por Vosk) */
    private static final boolean SIGNED = true;
    /** Orden de bytes: little-endian (requerida por Vosk) */
    private static final boolean BIG_ENDIAN = false;

    /** Tamaño del buffer de lectura de audio */
    private static final int BUFFER_SIZE = AppConfig.AUDIO_BUFFER_SIZE;

    /** Sleep cuando no hay datos del micrófono (milisegundos) */
    private static final int READ_SLEEP_NANOS = AppConfig.MICROPHONE_READ_SLEEP_MS * 1_000_000;

    /** Estado de ejecución - usado para control de threads */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Línea de captura del micrófono */
    private TargetDataLine microphone;
    /** Reconocedor Vosk actual */
    private Recognizer recognizer;

    /**
     * Crea una instancia de transcriptor de micrófono.
     *
     * @param voskService instancia del servicio Vosk para crear reconocedores
     * @throws NullPointerException si voskService es null
     */
    public MicTranscriber(VoskService voskService) {
        this.voskService = voskService;
    }
    
    /**
     * Inicia la transcripción en tiempo real desde el micrófono.
     * <p>
     * Este método implementa la interfaz {@link Transcriber}.
     * El parámetro `source` se ignora para micrófono (siempre null).
     * </p>
     *
     * @param source ignorado para micrófono (puede ser null)
     * @return vacío (la transcripción ocurre mediante callbacks)
     * @throws IOException si no se puede acceder al micrófono
     * @throws TranscriptionException si ocurren errores durante la transcripción
     */
    @Override
    public String transcribe(Path source) throws IOException, TranscriptionException {
        try {
            startTranscription();
            return ""; // La transcripción en micrófono es asíncrona via callbacks
        } catch (LineUnavailableException | IOException e) {
            // Ambos errores se consideran errores de micrófono/inicialización
            throw new TranscriptionException("Error iniciando transcripción de micrófono",
                TranscriptionException.ErrorType.MICROPHONE_ERROR, null, e);
        }
    }

    /**
     * Inicia la transcripción en tiempo real desde el micrófono.
     * <p>
     * Este método es un wrapper que delega a {@link #startTranscription(Consumer)}
     * con un callback que imprime en consola.
     * </p>
     *
     * @throws LineUnavailableException si no se puede acceder al micrófono
     * @throws IOException si ocurren errores de E/S
     * @throws IllegalStateException si la transcripción ya está en ejecución
     *
     * @see #startTranscription(Consumer)
     */
    public void startTranscription() throws LineUnavailableException, IOException {
        // Mantener compatibilidad: delegar a la versión con Consumer que imprime en consola
        startTranscription(text -> {
            if (text != null && !text.isEmpty()) {
                System.out.println(">>> " + text);
            }
        });
    }

    /**
     * Inicia la transcripción en tiempo real desde el micrófono con callback personalizado.
     * <p>
     * El método captura audio continuamente hasta que se llame a {@link #stop()}.
     * Invoca el callback para cada frase completa detectada.
     * </p>
     * <p>
     * Thread-safety: Este método es seguro para ser llamado desde múltiples threads.
     * Utiliza {@link AtomicBoolean} para sincronización sin locks bloqueantes.
     * </p>
     *
     * @param textConsumer consumidor que recibe las transcripciones completas
     * @throws LineUnavailableException si no se puede acceder al micrófono
     * @throws IOException si ocurren errores de E/S durante la inicialización
     * @throws IllegalStateException si la transcripción ya está en ejecución
     *
     * @see #stop()
     * @see Consumer
     */
    public void startTranscription(Consumer<String> textConsumer) throws LineUnavailableException, IOException {
        // Usar compareAndSet para atomicidad: solo retorna true si era false
        if (!running.compareAndSet(false, true)) {
            logger.warn("startTranscription llamado pero ya se está ejecutando");
            return;
        }

        // Configurar formato de audio
        AudioFormat format = new AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE_BITS,
            CHANNELS,
            SIGNED,
            BIG_ENDIAN
        );
        
        logger.info("Configuración de audio: {} Hz, {} bits, {} canal(es)", 
            SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS);
        
        // Obtener línea de captura del micrófono
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        
        if (!AudioSystem.isLineSupported(info)) {
            String error = String.format(
                "El formato de audio no está soportado por tu tarjeta de sonido.%n" +
                "Formato requerido: %.0f Hz, %d bits, mono.%n" +
                "Posibles soluciones:%n" +
                "  1. Verifica que el micrófono esté conectado y habilitado%n" +
                "  2. Actualiza los drivers de audio%n" +
                "  3. Prueba con otro dispositivo de entrada",
                SAMPLE_RATE, SAMPLE_SIZE_BITS
            );
            logger.error(error);
            throw new LineUnavailableException(error);
        }
        
        microphone = null;
        recognizer = null;
        
        try {
            // Abrir línea del micrófono
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            // abrir con un buffer interno algo mayor para evitar lecturas parciales
            microphone.open(format, BUFFER_SIZE * 2);
            microphone.start();
            
            logger.info("Micrófono abierto y capturando...");
            
            // Crear reconocedor Vosk (puede lanzar IOException)
            recognizer = voskService.createRecognizer(SAMPLE_RATE);
            
            // Buffer para leer audio
            byte[] buffer = new byte[BUFFER_SIZE];
            
            // Loop principal de captura y transcripción
            while (running.get()) {
                // Leer datos del micrófono
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                
                if (bytesRead <= 0) {
                    // evitar busy loop si no hay datos; usar parkNanos en lugar de Thread.sleep
                    LockSupport.parkNanos(READ_SLEEP_NANOS); // Sleep configurable
                    continue;
                }

                try {
                    // Alimentar bytes al reconocedor
                    boolean phraseCompleted = recognizer.acceptWaveForm(buffer, bytesRead);
                    if (phraseCompleted) {
                        // Resultado parcial disponible (frase completa detectada)
                        String result = recognizer.getResult();
                        String text = extractText(result);
                        
                        if (!text.isEmpty()) {
                            // Notificar al consumidor y mostrar en consola
                            try {
                                textConsumer.accept(text);
                            } catch (Exception e) {
                                logger.error("Error en el consumidor al procesar la transcripción", e);
                            }
                            System.out.println(">>> " + text);
                        }
                    } else {
                        // Resultado parcial (palabra/fragmento)
                        String partialResult = recognizer.getPartialResult();
                        String partialText = extractPartialText(partialResult);
                        
                        if (!partialText.isEmpty()) {
                            // Mostrar en la misma línea (se sobrescribe)
                            System.out.print("\r... " + partialText);
                            System.out.flush();
                        }
                    }
                } catch (Throwable t) {
                    // Capturamos cualquier error nativo inesperado y terminamos la sesión de forma limpia
                    logger.error("Error interno en recognizer durante acceptWaveForm: {}", t.toString());
                    // Intentar detener de forma segura
                    running.set(false);
                    break;
                }
            }
            
        } catch (LineUnavailableException e) {
            logger.error("Error al acceder al micrófono", e);
            throw e;
            
        } finally {
            // Limpiar recursos
            // Limpieza en finally
            if (microphone != null && microphone.isOpen()) {
                try {
                    microphone.stop();
                    microphone.close();
                } catch (Exception e) {
                    logger.debug("Error cerrando microfono en finalmente", e);
                }
                logger.info("Micrófono cerrado");
            }
            
            if (recognizer != null) {
                try {
                    recognizer.close();
                } catch (Exception e) {
                    logger.debug("Error cerrando recognizer", e);
                }
                recognizer = null;
            }
            running.set(false);
        }
    }

    /**
     * Solicita la detención de la transcripción en curso y cierra recursos.
     * <p>
     * Este método detiene de forma segura la captura de audio y libera recursos.
     * El orden es importante:
     * 1. Marcar como no ejecutando (detiene el loop principal)
     * 2. Cerrar micrófono (detiene captura de audio)
     * 3. Obtener resultado final del recognizer
     * 4. Cerrar recognizer
     * </p>
     */
    public void stop() {
        logger.info("Deteniendo transcripción...");
        running.set(false);

        // Dar un tiempo mínimo para que el loop principal se detenga
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        closeMicrophone();
        closeRecognizer();
        logger.info("Transcripción detenida");
    }

    /**
     * Cierra el micrófono de forma segura.
     * <p>
     * Siempre se ejecuta primero para detener la captura de audio.
     * </p>
     */
    private void closeMicrophone() {
        if (microphone != null) {
            try {
                if (microphone.isOpen()) {
                    microphone.stop();
                    microphone.close();
                    logger.debug("Micrófono cerrado");
                }
            } catch (Exception e) {
                logger.debug("Error cerrando micrófono: {}", e.getMessage());
            } finally {
                microphone = null;
            }
        }
    }

    /**
     * Cierra el reconocedor de forma segura y obtiene resultado final.
     * <p>
     * IMPORTANTE: Solo se llama DESPUÉS de que el micrófono está cerrado
     * para evitar intentar procesar datos que ya no existen.
     * </p>
     */
    private void closeRecognizer() {
        if (recognizer != null) {
            try {
                // Obtener resultado final SOLO si el recognizer está disponible
                try {
                    String finalResult = recognizer.getFinalResult();
                    String finalText = extractText(finalResult);
                    if (!finalText.isEmpty()) {
                        System.out.println("\nTexto final: " + finalText);
                    }
                } catch (Exception e) {
                    // Si falla obtener resultado final, solo loguear sin bloquear
                    logger.debug("No se pudo obtener resultado final: {}", e.getMessage());
                }

                // Cerrar recognizer
                try {
                    recognizer.close();
                    logger.debug("Reconocedor cerrado");
                } catch (Exception e) {
                    logger.debug("Error cerrando recognizer: {}", e.getMessage());
                }
            } finally {
                // SIEMPRE limpiar la referencia
                recognizer = null;
            }
        }
    }

    /**
     * Consulta si la transcripción está en ejecución.
     *
     * @return true si la transcripción está activa, false en caso contrario
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Extrae el texto del resultado JSON completo de Vosk.
     * 
     * @param jsonResult resultado en formato JSON
     * @return texto extraído
     */
    private String extractText(String jsonResult) {
        try {
            JSONObject json = new JSONObject(jsonResult);
            return json.optString("text", "");
        } catch (Exception e) {
            logger.debug("Error al parsear resultado: {}", jsonResult);
            return "";
        }
    }
    
    /**
     * Extrae el texto del resultado parcial JSON de Vosk.
     * 
     * @param jsonPartialResult resultado parcial en formato JSON
     * @return texto parcial extraído
     */
    private String extractPartialText(String jsonPartialResult) {
        try {
            JSONObject json = new JSONObject(jsonPartialResult);
            return json.optString("partial", "");
        } catch (Exception e) {
            logger.debug("Error al parsear resultado parcial: {}", jsonPartialResult);
            return "";
        }
    }
}
