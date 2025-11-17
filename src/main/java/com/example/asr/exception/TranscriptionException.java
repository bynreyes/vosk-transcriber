package com.example.asr.exception;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Excepción lanzada cuando ocurren errores durante la transcripción de audio.
 * <p>
 * Esta excepción proporciona contexto adicional sobre el tipo de error
 * y el recurso de audio involucrado.
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 */
public class TranscriptionException extends IOException {

    /**
     * Tipo de error que ocurrió durante la transcripción.
     */
    public enum ErrorType {
        /** Error de formato de audio no soportado */
        UNSUPPORTED_FORMAT,
        /** Error de conversión de audio */
        CONVERSION_ERROR,
        /** Error del motor de reconocimiento */
        RECOGNITION_ERROR,
        /** Error de E/S del archivo */
        IO_ERROR,
        /** Error de micrófono o dispositivo de audio */
        MICROPHONE_ERROR
    }

    private final ErrorType errorType;
    private final Path audioPath;

    /**
     * Crea una nueva excepción de transcripción.
     *
     * @param message el mensaje descriptivo del error
     * @param errorType el tipo de error ocurrido
     * @param audioPath la ruta del archivo de audio que causó el error (puede ser null)
     * @param cause la excepción original que causó este error
     */
    public TranscriptionException(String message, ErrorType errorType,
                                  Path audioPath, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.audioPath = audioPath;
    }

    /**
     * Crea una nueva excepción de transcripción sin causa raíz.
     *
     * @param message el mensaje descriptivo del error
     * @param errorType el tipo de error ocurrido
     * @param audioPath la ruta del archivo de audio que causó el error (puede ser null)
     */
    public TranscriptionException(String message, ErrorType errorType, Path audioPath) {
        super(message);
        this.errorType = errorType;
        this.audioPath = audioPath;
    }

    /**
     * Obtiene el tipo de error que ocurrió.
     *
     * @return el tipo de error
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Obtiene la ruta del archivo de audio que causó el error.
     *
     * @return la ruta del archivo, o null si no aplica
     */
    public Path getAudioPath() {
        return audioPath;
    }

    @Override
    public String toString() {
        return "TranscriptionException{" +
                "errorType=" + errorType +
                ", audioPath=" + audioPath +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}

