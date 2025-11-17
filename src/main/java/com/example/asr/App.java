package com.example.asr;

import com.example.asr.transcriber.FileTranscriber;
import com.example.asr.transcriber.MicTranscriber;
import com.example.asr.vosk.VoskService;
import com.example.asr.util.FileManager;
import com.example.asr.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import com.example.asr.gui.MainFrame;

/**
 * Punto de entrada de la aplicación de transcripción automática de voz.
 * <p>
 * Aplicación de transcripción de audio usando Vosk.
 * Soporta transcripción desde archivos de audio (.m4a, .wav) y desde micrófono en tiempo real.
 * </p>
 *
 * Soporta cuatro modos operacionales:
 * <ul>
 *   <li>Transcripción de archivo único ({@code --file})</li>
 *   <li>Procesamiento por lotes de directorio ({@code --dir})</li>
 *   <li>Transcripción en tiempo real desde micrófono ({@code --mic})</li>
 *   <li>Interfaz gráfica de usuario ({@code --gui})</li>
 *   <li>Opcionalmente, en modo micrófono se puede especificar un nuevo archivo de salida con {@code --new &lt;nombre&gt;}</li>
 * </ul>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 * @see FileTranscriber
 * @see MicTranscriber
 * @see com.example.asr.gui.MainFrame
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    public static void main(String[] args) {
        logger.info("=== Vosk Transcriber - Iniciando ===");
        
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        String mode = args[0];
        
        try {
            switch (mode) {
                case "--file":
                    handleFileMode(getArgumentOrExit(args, 1, "archivo"));
                    break;
                    
                case "--mic":
                    String newFileName = (args.length >= 3 && "--new".equals(args[1])) ? args[2] : null;
                    handleMicMode(newFileName);
                    break;

                case "--dir":
                    handleDirMode(getArgumentOrExit(args, 1, "directorio"));
                    break;
                    
                case "--gui":
                    SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
                    break;

                default:
                    logger.error("Modo desconocido: {}", mode);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            logger.error("Error fatal durante la ejecución", e);
            System.exit(1);
        }
        
        logger.info("=== Transcripción completada ===");
    }
    
    /**
     * Obtiene un argumento o termina la aplicación mostrando error.
     */
    private static String getArgumentOrExit(String[] args, int index, String argumentName) {
        if (args.length <= index) {
            logger.error("Error: Debes especificar la ruta del {}", argumentName);
            printUsage();
            System.exit(1);
        }
        return args[index];
    }

    /**
     * Maneja el modo de transcripción desde archivo.
     */
    private static void handleFileMode(String filePath) throws IOException {
        logger.info("Modo: Transcripción desde archivo");
        logger.info("Archivo de entrada: {}", filePath);

        Path inputPath = Paths.get(filePath);

        if (!Files.exists(inputPath)) {
            logger.error("El archivo no existe: {}", filePath);
            System.exit(1);
        }

        try (VoskService vosk = VoskService.getInstance()) {
            FileTranscriber transcriber = new FileTranscriber(vosk);
            String transcription = transcriber.transcribe(inputPath);

            System.out.println("\n========== TRANSCRIPCIÓN ==========");
            System.out.println(transcription);
            System.out.println("====================================\n");

            Path outputPath = FileManager.resolveTranscriptionFile(filePath);
            FileManager.saveTranscription(outputPath, transcription);

        } catch (Exception e) {
            logger.error("Error durante transcripción de archivo", e);
        }
    }
    
    /**
     * Maneja el modo de transcripción desde micrófono.
     */
    private static void handleMicMode(String newFileName) {
        logger.info("Modo: Transcripción desde micrófono");
        logger.info("Presiona Ctrl+C para detener la transcripción");
        
        try (VoskService vosk = VoskService.getInstance()) {
            MicTranscriber transcriber = new MicTranscriber(vosk);

            System.out.println("\n========== TRANSCRIPCIÓN EN TIEMPO REAL ==========");
            System.out.println("Habla ahora... (Ctrl+C para detener)");
            System.out.println("==================================================\n");
            
            Path outputFile = (newFileName != null)
                ? FileManager.resolveSessionFile(newFileName)
                : FileManager.getLiveTranscriptionFile();

            transcriber.startTranscription(text -> {
                if (text == null || text.isEmpty()) return;
                try {
                    FileManager.appendTranscription(outputFile, text);
                } catch (IOException e) {
                    logger.error("Error escribiendo transcripción", e);
                }
            });

        } catch (Exception e) {
            logger.error("Error durante transcripción desde micrófono", e);
        }
    }

    /**
     * Maneja el modo de transcripción por directorio.
     */
    private static void handleDirMode(String dirPath) throws IOException {
        logger.info("Modo: Transcripción por directorio");
        logger.info("Directorio de entrada: {}", dirPath);

        Path inputDir = Paths.get(dirPath);
        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            logger.error("El directorio no existe o no es una carpeta: {}", dirPath);
            System.exit(1);
        }

        try (VoskService vosk = VoskService.getInstance()) {
            FileTranscriber transcriber = new FileTranscriber(vosk);

            try (Stream<Path> files = Files.list(inputDir)) {
                files.filter(p -> isAudioFile(p.getFileName().toString().toLowerCase()))
                    .forEach(p -> processAudioFile(p, transcriber));
            }
        } catch (Exception e) {
            logger.error("Error durante transcripción de directorio", e);
        }
    }

    /**
     * Verifica si un archivo es de audio.
     */
    private static boolean isAudioFile(String fileName) {
        return fileName.endsWith(".m4a") || fileName.endsWith(".mp3") ||
               fileName.endsWith(".wav") || fileName.endsWith(".aac") ||
               fileName.endsWith(".flac") || fileName.endsWith(".ogg");
    }

    /**
     * Procesa un archivo de audio individual.
     */
    private static void processAudioFile(Path audioFile, FileTranscriber transcriber) {
        try {
            logger.info("Procesando archivo: {}", audioFile);
            String transcription = transcriber.transcribe(audioFile);

            Path outputPath = FileManager.resolveTranscriptionFile(audioFile.getFileName().toString());
            FileManager.saveTranscription(outputPath, transcription);

        } catch (Exception e) {
            logger.error("Error procesando {}", audioFile, e);
        }
    }

    /**
     * Imprime instrucciones de uso en consola.
     */
    private static void printUsage() {
        System.out.println("\nUso:");
        System.out.println("  Transcribir archivo:");
        System.out.println("    gradlew run --args=\"--file C:\\ruta\\audio.m4a\"");
        System.out.println();
        System.out.println("  Transcribir desde micrófono (append a live_transcription.txt):");
        System.out.println("    gradlew run --args=\"--mic\"");
        System.out.println();
        System.out.println("  Transcribir desde micrófono en archivo nuevo:");
        System.out.println("    gradlew run --args=\"--mic --new nombre_sesion.txt\"");
        System.out.println();
        System.out.println("  Transcribir todos los audios de una carpeta:");
        System.out.println("    gradlew run --args=\"--dir src\\main\\resources\\audio\"");
        System.out.println();
        System.out.println("  Interfaz gráfica:");
        System.out.println("    gradlew run --args=\"--gui\"");
        System.out.println();
    }
}
