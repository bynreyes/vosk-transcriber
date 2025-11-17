package com.example.asr.util;

import java.nio.file.Path;
import java.io.IOException;

/**
 * Interfaz que define el contrato para convertidores de audio.
 * <p>
 * Las implementaciones son responsables de convertir archivos de audio
 * de diversos formatos a WAV (formato nativo de Vosk).
 * </p>
 * <p>
 * Ejemplo de uso:
 * <pre>
 *   AudioConverter converter = new FFmpegAudioConverter();
 *   Path wavFile = converter.convertToWav(Paths.get("audio.m4a"));
 * </pre>
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 * @see com.example.asr.util.AudioConverter
 */
public interface AudioConverter {

    /**
     * Convierte un archivo de audio al formato WAV.
     * <p>
     * Si el archivo ya es WAV, puede devolver la ruta original o una copia
     * dependiendo de la implementación.
     * </p>
     *
     * @param inputPath ruta del archivo de audio a convertir
     * @return ruta del archivo WAV convertido
     * @throws IOException si ocurren errores de lectura, escritura o conversión
     * @throws IllegalArgumentException si el archivo no existe o formato no es soportado
     * @throws SecurityException si no se tienen permisos de archivo
     */
    Path convertToWav(Path inputPath) throws IOException;

    /**
     * Verifica si el archivo requiere conversión.
     * <p>
     * Devuelve true si el archivo no está en formato WAV y necesita conversión.
     * </p>
     *
     * @param filePath ruta del archivo a verificar
     * @return true si necesita conversión, false si ya es WAV
     */
    boolean requiresConversion(Path filePath);

    /**
     * Obtiene los formatos soportados para entrada.
     *
     * @return array de extensiones soportadas (ej: {".m4a", ".mp3", ".aac"})
     */
    String[] getSupportedFormats();
}

