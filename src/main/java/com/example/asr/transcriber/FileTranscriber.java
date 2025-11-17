package com.example.asr.transcriber;

import com.example.asr.exception.TranscriptionException;
import com.example.asr.util.AudioConverter;
import com.example.asr.util.AudioUtils;
import com.example.asr.util.FFmpegAudioConverter;
import com.example.asr.vosk.VoskService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Recognizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Transcriptor para archivos de audio.
 * <p>
 * Implementa la interfaz {@link Transcriber} para proporcionar transcripción
 * desde archivos de audio. Soporta conversión automática de diversos formatos
 * (M4A, MP3, AAC, etc.) a WAV mediante FFmpeg.
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 * @see Transcriber
 * @see AudioConverter
 */
public class FileTranscriber implements Transcriber {
    private static final Logger logger = LoggerFactory.getLogger(FileTranscriber.class);
    
    private final VoskService voskService;
    private final AudioConverter audioConverter;
    
    // Tamaño del buffer para leer el audio (4KB)
    private static final int BUFFER_SIZE = 4096;
    
    /**
     * Crea una instancia de transcriptor de archivos.
     *
     * @param voskService servicio Vosk para crear reconocedores
     */
    public FileTranscriber(VoskService voskService) {
        this.voskService = voskService;
        this.audioConverter = new FFmpegAudioConverter();
    }

    /**
     * Crea una instancia de transcriptor con convertidor personalizado.
     * <p>
     * Útil para testing o si se desea usar una implementación alternativa
     * de conversor de audio.
     * </p>
     *
     * @param voskService servicio Vosk para crear reconocedores
     * @param audioConverter conversor de audio personalizado
     */
    public FileTranscriber(VoskService voskService, AudioConverter audioConverter) {
        this.voskService = voskService;
        this.audioConverter = audioConverter;
    }

    /**
     * Transcribe un archivo de audio a texto.
     * <p>
     * Si el archivo no está en formato WAV, lo convierte automáticamente
     * usando FFmpeg.
     * </p>
     *
     * @param audioPath ruta al archivo de audio a transcribir
     * @return texto transcrito del audio
     * @throws IOException si hay errores de lectura o conversión
     * @throws TranscriptionException si hay errores durante la transcripción
     */
    @Override
    public String transcribe(Path audioPath) throws IOException, TranscriptionException {
        logger.info("Iniciando transcripción de: {}", audioPath);

        Path wavPath = audioPath;
        boolean needsCleanup = false;

        // Verificar si necesita conversión
        if (audioConverter.requiresConversion(audioPath)) {
            logger.info("Detectado formato no-WAV, convirtiendo a WAV...");

            try {
                wavPath = audioConverter.convertToWav(audioPath);
                needsCleanup = true;
                logger.info("Conversión completada: {}", wavPath);
            } catch (IOException e) {
                logger.error("Error convirtiendo archivo: {}", e.getMessage());
                throw e;
            }
        }

        try {
            // Validar formato WAV
            if (!AudioUtils.isValidWavFormat(wavPath)) {
                logger.warn("El archivo WAV no tiene el formato correcto");
                throw new IOException("Formato WAV inválido después de conversión");
            }

            // Realizar transcripción
            return performTranscription(wavPath);

        } finally {
            // Limpiar archivo temporal si fue creado
            if (needsCleanup && wavPath.toFile().exists()) {
                logger.debug("Eliminando archivo temporal: {}", wavPath);
                try {
                    Files.delete(wavPath);
                } catch (IOException e) {
                    logger.warn("No se pudo eliminar archivo temporal: {}", wavPath, e);
                }
            }
        }
    }

    /**
     * Detiene la transcripción (no aplica para archivos).
     * <p>
     * Este método es un no-op para archivos ya que la transcripción
     * es síncrona y completamente controlada por el método {@link #transcribe(Path)}.
     * </p>
     */
    @Override
    public void stop() {
        // No-op para transcriptor de archivos
        logger.debug("stop() llamado en FileTranscriber (no-op)");
    }

    /**
     * Realiza la transcripción del archivo WAV usando Vosk.
     * 
     * @param wavPath ruta al archivo WAV
     * @return texto transcrito
     * @throws IOException si hay errores de lectura
     */
    private String performTranscription(Path wavPath) throws IOException {
        logger.info("Procesando audio con Vosk...");

        try (var audioStream = AudioUtils.openWavInputStream(wavPath)) {
            Recognizer recognizer = voskService.createRecognizer();

            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                StringBuilder result = new StringBuilder();
                int bytesRead;

                while ((bytesRead = audioStream.read(buffer)) != -1) {
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        // Frase completada
                        String partialResult = recognizer.getResult();
                        String text = extractText(partialResult);
                        if (!text.isEmpty()) {
                            if (result.length() > 0) result.append(" ");
                            result.append(text);
                        }
                    }
                }

                // Obtener resultado final
                String finalResult = recognizer.getFinalResult();
                String finalText = extractText(finalResult);
                if (!finalText.isEmpty()) {
                    if (result.length() > 0) result.append(" ");
                    result.append(finalText);
                }

                logger.info("Audio procesado: {} bytes leídos",
                    wavPath.toFile().length());

                return result.toString();

            } finally {
                recognizer.close();
            }

        } catch (IOException e) {
            logger.error("Error leyendo archivo WAV: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extrae el texto del resultado JSON de Vosk.
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
}
