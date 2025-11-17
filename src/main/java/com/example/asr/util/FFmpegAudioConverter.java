package com.example.asr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementación de AudioConverter usando FFmpeg.
 * <p>
 * Convierte archivos de audio en diversos formatos (M4A, MP3, AAC) a WAV
 * utilizando el comando ffmpeg del sistema.
 * </p>
 * <p>
 * Requisitos:
 * <ul>
 *   <li>FFmpeg instalado y disponible en PATH</li>
 *   <li>Permisos de lectura en archivos de entrada</li>
 *   <li>Permisos de escritura en el directorio de salida</li>
 * </ul>
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 * @see AudioConverter
 */
public class FFmpegAudioConverter implements AudioConverter {
    private static final Logger logger = LoggerFactory.getLogger(FFmpegAudioConverter.class);

    private static final String FFMPEG_COMMAND = "ffmpeg";

    private static final String[] SUPPORTED_FORMATS = {
        ".m4a", ".m4b", ".mp4",
        ".mp3",
        ".aac",
        ".flac",
        ".ogg",
        ".wma"
    };

    /**
     * Crea una nueva instancia del convertidor FFmpeg.
     */
    public FFmpegAudioConverter() {
        logger.debug("FFmpegAudioConverter inicializado");
    }

    @Override
    public Path convertToWav(Path inputPath) throws IOException {
        logger.info("Convirtiendo {} a WAV...", inputPath.getFileName());

        // Validar que el archivo existe
        if (!Files.exists(inputPath)) {
            throw new IllegalArgumentException("El archivo no existe: " + inputPath);
        }

        // Comprobar que ffmpeg está disponible en PATH antes de construir procesos
        ensureFfmpegAvailable();

        // Crear ruta de salida temporal
        String baseName = inputPath.getFileName().toString().replaceAll("\\.[^.]+$", "");
        String tempFileName = baseName + ".temp.wav";
        Path outputPath = inputPath.getParent().resolve(tempFileName);

        // Si ya es WAV, devolver la misma ruta
        if (inputPath.toString().toLowerCase().endsWith(".wav")) {
            logger.debug("El archivo ya es WAV, no se requiere conversión");
            return inputPath;
        }

        try {
            convertFile(inputPath, outputPath);
            logger.info("Conversión exitosa: {} → {}", inputPath.getFileName(), outputPath.getFileName());
            return outputPath;

        } catch (Exception e) {
            logger.error("Error durante la conversión: {}", e.getMessage());
            // Limpiar archivo parcialmente convertido
            try {
                Files.deleteIfExists(outputPath);
            } catch (IOException ignored) {
            }
            throw new IOException("Error convirtiendo archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean requiresConversion(Path filePath) {
        if (!Files.exists(filePath)) {
            return false;
        }

        String fileName = filePath.toString().toLowerCase();
        return !fileName.endsWith(".wav");
    }

    @Override
    public String[] getSupportedFormats() {
        return SUPPORTED_FORMATS.clone();
    }

    /**
     * Ejecuta la conversión usando FFmpeg.
     *
     * @param inputPath archivo de entrada
     * @param outputPath archivo de salida (WAV)
     * @throws IOException si ffmpeg falla
     * @throws InterruptedException si el proceso es interrumpido
     */
    private void convertFile(Path inputPath, Path outputPath) throws IOException, InterruptedException {
        // Comando: ffmpeg -i input.m4a -acodec pcm_s16le -ar 16000 output.wav
        ProcessBuilder pb = new ProcessBuilder(
            FFMPEG_COMMAND,
            "-i", inputPath.toString(),
            "-acodec", "pcm_s16le",      // Codec WAV estándar
            "-ar", "16000",              // 16 kHz (requerido por Vosk)
            "-ac", "1",                  // Mono (requerido por Vosk)
            "-y",                        // Overwrite output file
            outputPath.toString()
        );

        // Redirigir stderr a stdout para capturar logs
        pb.redirectErrorStream(true);

        logger.debug("Ejecutando: {}", String.join(" ", pb.command()));

        Process process = pb.start();

        // Consumir la salida del proceso para evitar bloqueos por buffer lleno
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug(line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("FFmpeg retornó código de error: " + exitCode);
        }
    }

    /**
     * Comprueba si ffmpeg está disponible en PATH ejecutando `ffmpeg -version`.
     * Lanza IOException con instrucciones si no está disponible.
     */
    private void ensureFfmpegAvailable() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(FFMPEG_COMMAND, "-version");
        pb.redirectErrorStream(true);
        Process p = null;
        try {
            p = pb.start();
            boolean finished = p.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new IOException("ffmpeg no respondió dentro de 3 segundos. Comprueba la instalación de ffmpeg y el PATH.");
            }
            int code = p.exitValue();
            if (code != 0) {
                String output = "";
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    output = r.lines().limit(50).collect(Collectors.joining(System.lineSeparator()));
                } catch (Exception ex) {
                    // Ignorar
                }
                throw new IOException("Se encontró ffmpeg, pero devolvió código " + code + ". Salida: " + output);
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IOException("ffmpeg no está disponible en PATH. Instala ffmpeg y asegúrate de que el ejecutable esté en la variable de entorno PATH.", ex);
        } finally {
            if (p != null) {
                try { p.getInputStream().close(); } catch (Exception ignored) {}
                try { p.getErrorStream().close(); } catch (Exception ignored) {}
            }
        }
    }
}
