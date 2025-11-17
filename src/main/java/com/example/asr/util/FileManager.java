package com.example.asr.util;

import com.example.asr.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Gestor centralizado de archivos para transcripciones.
 * <p>
 * Encapsula toda la lógica de gestión de directorios y archivos,
 * eliminando duplicación en la aplicación.
 * </p>
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>Crear directorios de transcripciones</li>
 *   <li>Generar nombres de archivo consistentes</li>
 *   <li>Guardar y recuperar transcripciones</li>
 *   <li>Manejar errores de E/S</li>
 * </ul>
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 */
public final class FileManager {
    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    /**
     * Obtiene el directorio de transcripciones, creándolo si no existe.
     *
     * @return Path al directorio de transcripciones
     * @throws IOException si no se puede crear el directorio
     */
    public static Path getTranscriptionsDir() throws IOException {
        Path dir = AppConfig.TRANSCRIPTIONS_DIR;
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            logger.info("Directorio de transcripciones creado: {}", dir);
        }
        return dir;
    }

    /**
     * Resuelve la ruta de salida para una transcripción basada en un archivo de entrada.
     * <p>
     * Genera un nombre consistente: archivo_sin_extension.txt
     * </p>
     *
     * @param inputFileName nombre o ruta del archivo de entrada
     * @return Path completo para guardar la transcripción
     * @throws IOException si no se puede crear el directorio de salida
     */
    public static Path resolveTranscriptionFile(String inputFileName) throws IOException {
        Path outDir = getTranscriptionsDir();

        // Extraer nombre sin extensión
        String baseName = new java.io.File(inputFileName).getName();
        int idx = baseName.lastIndexOf('.');
        String nameNoExt = (idx > 0) ? baseName.substring(0, idx) : baseName;

        return outDir.resolve(nameNoExt + ".txt");
    }

    /**
     * Resuelve la ruta de un archivo de sesión personalizado.
     * <p>
     * Útil para el modo micrófono cuando el usuario especifica un nombre de sesión.
     * </p>
     *
     * @param sessionName nombre de la sesión (sin extensión)
     * @return Path completo para guardar la sesión
     * @throws IOException si no se puede crear el directorio
     */
    public static Path resolveSessionFile(String sessionName) throws IOException {
        Path outDir = getTranscriptionsDir();

        // Sanitizar nombre - eliminar caracteres inválidos en nombres de archivo
        String safeName = sessionName.replaceAll("[<>:\"/|?*]", "_");

        return outDir.resolve(safeName + ".txt");
    }

    /**
     * Obtiene la ruta del archivo de transcripción en vivo por defecto.
     *
     * @return Path al archivo live_transcription.txt
     * @throws IOException si no se puede crear el directorio
     */
    public static Path getLiveTranscriptionFile() throws IOException {
        return getTranscriptionsDir().resolve(AppConfig.DEFAULT_LIVE_TRANSCRIPTION_FILE);
    }

    /**
     * Guarda un texto de transcripción en un archivo.
     *
     * @param filePath ruta del archivo a guardar
     * @param transcription texto a guardar
     * @throws IOException si ocurren errores de E/S
     */
    public static void saveTranscription(Path filePath, String transcription) throws IOException {
        Files.writeString(filePath, transcription);
        logger.info("Transcripción guardada: {}", filePath);
    }

    /**
     * Añade un texto al final de un archivo (append).
     * <p>
     * Útil para acumular transcripciones en tiempo real.
     * </p>
     *
     * @param filePath ruta del archivo
     * @param text texto a añadir
     * @throws IOException si ocurren errores de E/S
     */
    public static void appendTranscription(Path filePath, String text) throws IOException {
        Files.writeString(filePath, text + System.lineSeparator(),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Constructor privado - clase de utilidad pura (no instanciable).
     */
    private FileManager() {
        throw new AssertionError("FileManager no debería ser instanciada");
    }
}
