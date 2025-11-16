package com.example.asr.transcriber;

import com.example.asr.vosk.VoskService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Transcriptor de audio en tiempo real desde micrófono.
 * Captura audio usando Java Sound API y lo procesa con Vosk.
 */
public class MicTranscriber {
    private static final Logger logger = LoggerFactory.getLogger(MicTranscriber.class);
    
    private final VoskService voskService;
    
    // Configuración de audio para captura desde micrófono
    private static final float SAMPLE_RATE = 16000.0f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1; // Mono
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false; // Little-endian
    
    // Tamaño del buffer de captura (0.1 segundos)
    private static final int BUFFER_SIZE = (int)(SAMPLE_RATE * SAMPLE_SIZE_BITS / 8 * CHANNELS * 0.1);
    
    // Estado de ejecución para permitir detener la transcripción
    private volatile boolean running = false;

    // Guardamos las referencias para poder detener desde otra hebra
    private TargetDataLine microphone;
    private Recognizer recognizer;

    public MicTranscriber(VoskService voskService) {
        this.voskService = voskService;
    }
    
    /**
     * Inicia la transcripción en tiempo real desde el micrófono.
     * Este método es bloqueante y continúa hasta que se detiene el programa (Ctrl+C).
     * 
     * @throws LineUnavailableException si no se puede acceder al micrófono
     * @throws IOException si no se puede crear el Recognizer
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
     * Versión de startTranscription que acepta un consumidor que recibe cada transcripción completa
     * (y la transcripción final cuando se detiene).
     */
    public void startTranscription(Consumer<String> textConsumer) throws LineUnavailableException, IOException {
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
                "El formato de audio no está soportado por tu tarjeta de sonido.\n" +
                "Formato requerido: %.0f Hz, %d bits, mono.\n" +
                "Posibles soluciones:\n" +
                "  1. Verifica que el micrófono esté conectado y habilitado\n" +
                "  2. Actualiza los drivers de audio\n" +
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
            microphone.open(format, BUFFER_SIZE);
            microphone.start();
            
            logger.info("Micrófono abierto y capturando...");
            
            // Crear reconocedor Vosk (puede lanzar IOException)
            recognizer = voskService.createRecognizer(SAMPLE_RATE);
            
            // Buffer para leer audio
            byte[] buffer = new byte[BUFFER_SIZE];
            
            // Registrar hook para limpieza con Ctrl+C
            final Consumer<String> finalConsumer = textConsumer;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("\nDeteniendo transcripción por shutdown hook...");
                try {
                    stop();
                } catch (Exception e) {
                    logger.debug("Error durante stop en shutdown hook", e);
                }
                if (recognizer != null) {
                    try {
                        String finalResult = recognizer.getFinalResult();
                        String finalText = extractText(finalResult);
                        if (!finalText.isEmpty()) {
                            try {
                                finalConsumer.accept(finalText);
                            } catch (Exception e) {
                                logger.error("Error en el consumidor al escribir la transcripción final", e);
                            }
                            System.out.println("\nTexto final: " + finalText);
                        }
                    } catch (Exception e) {
                        logger.debug("No se pudo obtener resultado final", e);
                    }
                }
            }));
            
            // Loop principal de captura y transcripción
            running = true;
            while (running) {
                // Leer datos del micrófono
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                    // Alimentar bytes al reconocedor
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
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
                }
            }
            
        } catch (LineUnavailableException e) {
            logger.error("Error al acceder al micrófono", e);
            throw e;
            
        } finally {
            // Limpiar recursos
            // Limpieza en finally
            if (microphone != null && microphone.isOpen()) {
                microphone.stop();
                microphone.close();
                logger.info("Micrófono cerrado");
            }
            
            if (recognizer != null) {
                try {
                    recognizer.close();
                } catch (Exception e) {
                    logger.debug("Error cerrando recognizer", e);
                }
            }
        }
    }

    /**
     * Solicita la detención de la transcripción en curso y cierra recursos.
     */
    public void stop() {
        running = false;

        // Cerrar la línea del micrófono si está abierta
        try {
            if (microphone != null && microphone.isOpen()) {
                microphone.stop();
                microphone.close();
            }
        } catch (Exception e) {
            logger.debug("Error cerrando micrófono en stop()", e);
        }

        // Obtener resultado final y cerrar recognizer
        try {
            if (recognizer != null) {
                String finalResult = recognizer.getFinalResult();
                String finalText = extractText(finalResult);
                if (!finalText.isEmpty()) {
                    System.out.println("\nTexto final: " + finalText);
                }
                recognizer.close();
                recognizer = null;
            }
        } catch (Exception e) {
            logger.debug("Error cerrando recognizer en stop()", e);
        }
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
