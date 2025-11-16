package com.example.asr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Utilidades para validar y abrir archivos WAV en el formato esperado por Vosk.
 */
public final class AudioUtils {
    private static final Logger logger = LoggerFactory.getLogger(AudioUtils.class);

    private static final float TARGET_SAMPLE_RATE = 16000.0f;
    private static final int TARGET_CHANNELS = 1;
    private static final int TARGET_SAMPLE_SIZE_BITS = 16;
    private static final boolean TARGET_BIG_ENDIAN = false; // little-endian

    private AudioUtils() {
        // util class
    }

    /**
     * Verifica si el archivo WAV tiene el formato compatible: PCM_SIGNED, 16kHz, mono, 16 bits, little-endian
     *
     * @param wavPath ruta al archivo WAV
     * @return true si el formato es compatible, false en caso contrario o si ocurre un error
     */
    public static boolean isValidWavFormat(Path wavPath) {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wavPath.toFile())) {
            AudioFormat fmt = ais.getFormat();

            boolean encodingOk = AudioFormat.Encoding.PCM_SIGNED.equals(fmt.getEncoding());
            boolean channelsOk = fmt.getChannels() == TARGET_CHANNELS;
            boolean sampleSizeOk = fmt.getSampleSizeInBits() == TARGET_SAMPLE_SIZE_BITS;
            boolean sampleRateOk = Math.abs(fmt.getSampleRate() - TARGET_SAMPLE_RATE) < 1.0f;
            boolean endianOk = fmt.isBigEndian() == TARGET_BIG_ENDIAN;

            logger.debug("WAV format: encoding={}, channels={}, sampleSize={}, sampleRate={}, bigEndian={}",
                    fmt.getEncoding(), fmt.getChannels(), fmt.getSampleSizeInBits(), fmt.getSampleRate(), fmt.isBigEndian());

            return encodingOk && channelsOk && sampleSizeOk && sampleRateOk && endianOk;
        } catch (Exception e) {
            logger.debug("Error al verificar formato WAV: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Abre un InputStream con los bytes PCM del WAV en el formato esperado por Vosk (PCM 16-bit little-endian, 16kHz, mono).
     * Si el archivo no está en ese formato intentará convertirlo usando las utilidades de Java Sound.
     *
     * @param wavPath ruta al archivo WAV
     * @return AudioInputStream listo para leer bytes PCM
     * @throws IOException si ocurre un error al abrir o convertir el archivo
     */
    public static AudioInputStream openWavInputStream(Path wavPath) throws IOException {
        File file = wavPath.toFile();

        try {
            AudioInputStream sourceAis = AudioSystem.getAudioInputStream(file);
            AudioFormat sourceFormat = sourceAis.getFormat();

            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    TARGET_SAMPLE_RATE,
                    TARGET_SAMPLE_SIZE_BITS,
                    TARGET_CHANNELS,
                    (TARGET_SAMPLE_SIZE_BITS / 8) * TARGET_CHANNELS,
                    TARGET_SAMPLE_RATE,
                    TARGET_BIG_ENDIAN
            );

            if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
                AudioInputStream converted = AudioSystem.getAudioInputStream(targetFormat, sourceAis);
                return converted;
            } else {
                // Si no se puede convertir, comprobar si ya está en el formato esperado
                if (AudioFormat.Encoding.PCM_SIGNED.equals(sourceFormat.getEncoding()) &&
                        sourceFormat.getChannels() == TARGET_CHANNELS &&
                        sourceFormat.getSampleSizeInBits() == TARGET_SAMPLE_SIZE_BITS &&
                        Math.abs(sourceFormat.getSampleRate() - TARGET_SAMPLE_RATE) < 1.0f &&
                        sourceFormat.isBigEndian() == TARGET_BIG_ENDIAN) {
                    return sourceAis;
                }

                // Intentar una conversión genérica (mayor compatibilidad en algunas JVMs)
                try {
                    AudioInputStream converted = AudioSystem.getAudioInputStream(targetFormat, sourceAis);
                    return converted;
                } catch (Exception ex) {
                    sourceAis.close();
                    throw new IOException("No se puede convertir el archivo WAV al formato requerido: " + ex.getMessage(), ex);
                }
            }

        } catch (UnsupportedAudioFileException e) {
            throw new IOException("Formato de audio no soportado: " + e.getMessage(), e);
        }
    }
}
