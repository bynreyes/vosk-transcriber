package com.example.asr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utilidad para convertir archivos de audio usando ffmpeg.
 * Convierte cualquier formato soportado por ffmpeg a WAV mono 16kHz PCM 16-bit.
 */
public class AudioConverter {
    private static final Logger logger = LoggerFactory.getLogger(AudioConverter.class);
    
    // Configuración de salida WAV para Vosk
    private static final String OUTPUT_FORMAT = "wav";
    private static final int CHANNELS = 1; // Mono
    private static final int SAMPLE_RATE = 16000; // 16 kHz
    private static final int TIMEOUT_SECONDS = 300; // 5 minutos
    
    /**
     * Convierte un archivo de audio a formato WAV compatible con Vosk.
     * Formato de salida: WAV mono, 16 kHz, PCM 16-bit, little-endian
     * 
     * @param inputPath ruta al archivo de entrada (cualquier formato soportado por ffmpeg)
     * @param outputPath ruta al archivo WAV de salida
     * @throws IOException si hay errores durante la conversión
     */
    public void convertToWav(Path inputPath, Path outputPath) throws IOException {
        logger.info("Convirtiendo {} a {}", inputPath, outputPath);
        
        // Verificar que ffmpeg está disponible
        if (!isFfmpegAvailable()) {
            String error = "ffmpeg no está disponible en el PATH del sistema.\n" +
                          "Por favor instala ffmpeg y añádelo al PATH:\n" +
                          "  1. Descarga ffmpeg desde: https://www.gyan.dev/ffmpeg/builds/\n" +
                          "  2. Extrae el archivo ZIP\n" +
                          "  3. Añade la carpeta 'bin' al PATH de Windows\n" +
                          "  4. Reinicia la terminal/IDE";
            logger.error(error);
            throw new IOException(error);
        }
        
        // Construir comando ffmpeg
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y"); // Sobrescribir sin preguntar
        command.add("-i");
        command.add(inputPath.toString());
        command.add("-ac");
        command.add(String.valueOf(CHANNELS)); // Mono
        command.add("-ar");
        command.add(String.valueOf(SAMPLE_RATE)); // 16000 Hz
        command.add("-f");
        command.add(OUTPUT_FORMAT); // WAV
        command.add("-acodec");
        command.add("pcm_s16le"); // PCM 16-bit little-endian
        command.add(outputPath.toString());
        
        logger.debug("Ejecutando comando: {}", String.join(" ", command));
        
        try {
            // Ejecutar ffmpeg
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Combinar stdout y stderr
            
            Process process = pb.start();
            
            // Capturar salida para debugging
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.trace("ffmpeg: {}", line);
                }
            }
            
            // Esperar a que termine
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("ffmpeg timeout después de " + TIMEOUT_SECONDS + " segundos");
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode != 0) {
                String error = "ffmpeg falló con código de salida " + exitCode + "\n" +
                              "Salida:\n" + output.toString();
                logger.error(error);
                throw new IOException(error);
            }
            
            logger.info("Conversión completada exitosamente");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Conversión interrumpida", e);
        }
    }
    
    /**
     * Verifica si ffmpeg está disponible en el PATH del sistema.
     * 
     * @return true si ffmpeg está disponible, false en caso contrario
     */
    public boolean isFfmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (IOException | InterruptedException e) {
            logger.debug("ffmpeg no disponible", e);
            return false;
        }
    }
    
    /**
     * Obtiene la versión de ffmpeg instalada.
     * 
     * @return versión de ffmpeg o null si no está disponible
     */
    public String getFfmpegVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String firstLine = reader.readLine();
                if (firstLine != null) {
                    return firstLine;
                }
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            
        } catch (IOException | InterruptedException e) {
            logger.debug("Error obteniendo versión de ffmpeg", e);
        }
        
        return null;
    }
}
