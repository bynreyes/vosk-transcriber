package com.example.asr.transcriber;

import com.example.asr.util.AudioConverter;
import com.example.asr.util.AudioUtils;
import com.example.asr.vosk.VoskService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.Recognizer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Transcriptor para archivos de audio.
 * Soporta conversión automática de .m4a a .wav y transcripción con Vosk.
 */
public class FileTranscriber {
    private static final Logger logger = LoggerFactory.getLogger(FileTranscriber.class);
    
    private final VoskService voskService;
    private final AudioConverter audioConverter;
    
    // Tamaño del buffer para leer el audio (4KB)
    private static final int BUFFER_SIZE = 4096;
    
    public FileTranscriber(VoskService voskService) {
        this.voskService = voskService;
        this.audioConverter = new AudioConverter();
    }

    /**
     * Transcribe un archivo de audio a texto.
     * Si el archivo es .m4a, lo convierte automáticamente a .wav.
     * 
     * @param audioPath ruta al archivo de audio
     * @return texto transcrito
     * @throws IOException si hay errores de lectura/conversión
     */
    public String transcribe(Path audioPath) throws IOException {
        logger.info("Iniciando transcripción de: {}", audioPath);

        Path wavPath = audioPath;
        boolean needsCleanup = false;

        // Verificar si necesita conversión
        String fileName = audioPath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".m4a") || fileName.endsWith(".mp4") ||
            fileName.endsWith(".mp3") || fileName.endsWith(".aac")) {

            logger.info("Detectado formato no-WAV, convirtiendo a WAV...");

            // Convertir a WAV temporal
            wavPath = Paths.get(audioPath.toString() + ".temp.wav");
            audioConverter.convertToWav(audioPath, wavPath);
            needsCleanup = true;

            logger.info("Conversión completada: {}", wavPath);
        }

        try {
            // Validar formato WAV
            if (!AudioUtils.isValidWavFormat(wavPath)) {
                logger.warn("El archivo WAV no tiene el formato correcto, intentando re-convertir...");

                Path reconvertedPath = Paths.get(wavPath.toString() + ".reconverted.wav");
                audioConverter.convertToWav(wavPath, reconvertedPath);

                // Limpiar archivo intermedio si fue convertido
                if (needsCleanup) {
                    wavPath.toFile().delete();
                }

                wavPath = reconvertedPath;
                needsCleanup = true;
            }

            // Realizar transcripción
            return performTranscription(wavPath);

        } finally {
            // Limpiar archivo temporal si fue creado
            if (needsCleanup && wavPath.toFile().exists()) {
                logger.debug("Eliminando archivo temporal: {}", wavPath);
                wavPath.toFile().delete();
            }
        }
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

        Recognizer recognizer = voskService.createRecognizer();
        StringBuilder transcription = new StringBuilder();

        try {
            // Leer audio en chunks y alimentar al reconocedor
            byte[] buffer = new byte[BUFFER_SIZE];
            int totalBytesRead = 0;

            try (var audioStream = AudioUtils.openWavInputStream(wavPath)) {
                int bytesRead;

                while ((bytesRead = audioStream.read(buffer)) != -1) {
                    totalBytesRead += bytesRead;

                    // Alimentar bytes al reconocedor
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        // Resultado parcial (frase completada)
                        String result = recognizer.getResult();
                        String text = extractText(result);

                        if (!text.isEmpty()) {
                            transcription.append(text).append(" ");
                            logger.debug("Resultado parcial: {}", text);
                        }
                    }
                }

                // Obtener resultado final (último fragmento)
                String finalResult = recognizer.getFinalResult();
                String finalText = extractText(finalResult);

                if (!finalText.isEmpty()) {
                    transcription.append(finalText);
                }

                logger.info("Audio procesado: {} bytes leídos", totalBytesRead);
            }

            String result = transcription.toString().trim();

            if (result.isEmpty()) {
                logger.warn("No se detectó ningún texto en el audio");
                return "[Sin transcripción - no se detectó voz o modelo incompatible con el idioma del audio]";
            }

            return result;

        } finally {
            recognizer.close();
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
            logger.warn("Error al parsear resultado JSON: {}", jsonResult, e);
            return "";
        }
    }
}
