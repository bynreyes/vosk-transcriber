package com.example.asr.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Validador centralizado de recursos necesarios para la aplicación.
 * <p>
 * Valida la disponibilidad y configuración correcta de:
 * <ul>
 *   <li>Modelo Vosk</li>
 *   <li>Micrófono del sistema</li>
 *   <li>FFmpeg para conversión de audio</li>
 * </ul>
 * </p>
 * <p>
 * Ejemplo de uso:
 * <pre>
 *   ResourceValidator validator = new ResourceValidator();
 *   ValidationResult modelCheck = validator.validateModel();
 *   if (!modelCheck.isValid()) {
 *       System.err.println(modelCheck.getErrorMessage());
 *   }
 * </pre>
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 */
public class ResourceValidator {
    private static final Logger logger = LoggerFactory.getLogger(ResourceValidator.class);

    private static final String DEFAULT_MODEL_PATH = "./models/vosk-model-es-0.42";
    private static final String FFMPEG_COMMAND = "ffmpeg";

    /**
     * Resultado de una validación de recurso.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String errorMessage;

        /**
         * Crea un resultado de validación exitoso.
         *
         * @param message mensaje descriptivo del recurso
         */
        private ValidationResult(String message) {
            this.valid = true;
            this.message = message;
            this.errorMessage = null;
        }

        /**
         * Crea un resultado de validación fallido.
         *
         * @param message mensaje descriptivo del recurso
         * @param errorMessage descripción del error
         */
        private ValidationResult(String message, String errorMessage) {
            this.valid = false;
            this.message = message;
            this.errorMessage = errorMessage;
        }

        /**
         * Indica si el recurso es válido.
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * Obtiene el mensaje descriptivo.
         */
        public String getMessage() {
            return message;
        }

        /**
         * Obtiene el mensaje de error (null si es válido).
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return valid ?
                "✓ " + message :
                "✗ " + message + " - " + errorMessage;
        }
    }

    /**
     * Valida la disponibilidad del modelo Vosk.
     *
     * @return resultado de la validación
     */
    public ValidationResult validateModel() {
        Path modelPath = Paths.get(DEFAULT_MODEL_PATH);

        if (!Files.exists(modelPath)) {
            String error = String.format(
                "Modelo Vosk no encontrado en %s. " +
                "Descargalo desde https://alphacephei.com/vosk/models",
                modelPath.toAbsolutePath()
            );
            logger.error(error);
            return new ValidationResult("Modelo Vosk", error);
        }

        if (!Files.isDirectory(modelPath)) {
            String error = modelPath + " no es un directorio";
            logger.error(error);
            return new ValidationResult("Modelo Vosk", error);
        }

        logger.info("Modelo Vosk validado: {}", modelPath.toAbsolutePath());
        return new ValidationResult("Modelo Vosk disponible en " + modelPath);
    }

    /**
     * Valida la disponibilidad del micrófono del sistema.
     *
     * @return resultado de la validación
     */
    public ValidationResult validateMicrophone() {
        try {
            // Intentar obtener una línea de entrada de audio
            var mixers = AudioSystem.getMixerInfo();

            if (mixers.length == 0) {
                String error = "No hay dispositivos de audio disponibles en el sistema";
                logger.warn(error);
                return new ValidationResult("Micrófono", error);
            }

            logger.info("Se encontraron {} dispositivos de audio", mixers.length);
            return new ValidationResult("Micrófono disponible - " + mixers.length + " dispositivo(s)");

        } catch (Exception e) {
            String error = "Error accediendo al micrófono: " + e.getMessage();
            logger.error(error, e);
            return new ValidationResult("Micrófono", error);
        }
    }

    /**
     * Valida la disponibilidad de FFmpeg en el PATH del sistema.
     *
     * @return resultado de la validación
     */
    public ValidationResult validateFFmpeg() {
        try {
            // Intentar ejecutar ffmpeg -version
            ProcessBuilder pb = new ProcessBuilder(FFMPEG_COMMAND, "-version");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("FFmpeg disponible en PATH");
                return new ValidationResult("FFmpeg disponible en PATH del sistema");
            } else {
                String error = "FFmpeg retornó código de error: " + exitCode;
                logger.warn(error);
                return new ValidationResult("FFmpeg", error);
            }

        } catch (IOException e) {
            String error = "FFmpeg no encontrado en PATH. " +
                "Instálalo desde https://ffmpeg.org/download.html";
            logger.warn(error);
            return new ValidationResult("FFmpeg", error);
        } catch (InterruptedException e) {
            String error = "Validación de FFmpeg interrumpida";
            logger.warn(error);
            Thread.currentThread().interrupt();
            return new ValidationResult("FFmpeg", error);
        }
    }

    /**
     * Valida todos los recursos críticos.
     * <p>
     * Retorna una lista de resultados (algunos pueden fallar sin ser críticos).
     * </p>
     *
     * @return array de resultados de validación
     */
    public ValidationResult[] validateAll() {
        return new ValidationResult[]{
            validateModel(),
            validateMicrophone(),
            validateFFmpeg()
        };
    }

    /**
     * Verifica si todos los recursos críticos son válidos.
     * <p>
     * Modelo y Micrófono son críticos. FFmpeg es importante pero no crítico
     * (puedo usar archivos WAV sin FFmpeg).
     * </p>
     *
     * @return true si todos los recursos críticos están disponibles
     */
    public boolean areAllCriticalResourcesValid() {
        ValidationResult model = validateModel();
        ValidationResult microphone = validateMicrophone();

        return model.isValid() && microphone.isValid();
    }
}

