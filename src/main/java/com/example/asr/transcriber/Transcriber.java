package com.example.asr.transcriber;

import com.example.asr.exception.TranscriptionException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Interfaz que define el contrato para transcriptores de audio.
 * <p>
 * Implementadores de esta interfaz pueden proporcionar transcripción
 * desde diferentes fuentes (archivos, micrófono, streams, etc.).
 * </p>
 * <p>
 * Ejemplo de uso:
 * <pre>
 *   Transcriber transcriber = new FileTranscriber(...);
 *   String result = transcriber.transcribe(audioPath);
 * </pre>
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 * @see FileTranscriber
 * @see MicTranscriber
 */
public interface Transcriber {

    /**
     * Transcribe audio desde la fuente específica del implementador.
     * <p>
     * El comportamiento específico depende de la implementación:
     * - FileTranscriber: transcribe un archivo completamente
     * - MicTranscriber: captura audio del micrófono hasta que se llame a stop()
     * </p>
     *
     * @param source ruta del archivo o identificador de la fuente de audio
     * @return texto transcrito del audio
     * @throws IOException si ocurren errores de E/S
     * @throws TranscriptionException si ocurren errores durante la transcripción
     * @throws IllegalArgumentException si la fuente no es válida
     * @throws IllegalStateException si la transcripción ya está en progreso
     */
    String transcribe(Path source) throws IOException, TranscriptionException;

    /**
     * Detiene la transcripción en progreso (aplica solo a fuentes continuas como micrófono).
     * <p>
     * Este método es idempotente - puede ser llamado múltiples veces
     * sin efectos adversos.
     * </p>
     *
     * @throws Exception si ocurren errores al detener la transcripción
     */
    void stop() throws Exception;
}
