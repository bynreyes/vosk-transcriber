package com.example.asr;

import com.example.asr.transcriber.FileTranscriber;
import com.example.asr.transcriber.MicTranscriber;
import com.example.asr.vosk.VoskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;
import java.util.function.Consumer;

/**
 * Aplicación de transcripción de audio usando Vosk.
 * Soporta transcripción desde archivos de audio (.m4a, .wav) y desde micrófono en tiempo real.
 * 
 * Uso:
 *   --file <ruta>  : Transcribe un archivo de audio
 *   --mic          : Transcribe audio desde micrófono en tiempo real
 *   --dir <ruta>   : Transcribe todos los archivos de audio en la carpeta
 *   --gui          : Interfaz grafica 
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    public static void main(String[] args) {
        logger.info("=== Vosk Transcriber - Iniciando ===");
        
        // Validar argumentos
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        String mode = args[0];
        
        try {
            switch (mode) {
                case "--file":
                    if (args.length < 2) {
                        logger.error("Error: Debes especificar la ruta del archivo");
                        printUsage();
                        System.exit(1);
                    }
                    handleFileMode(args[1]);
                    break;
                    
                case "--mic":
                    handleMicMode();
                    break;

                case "--dir":
                    if (args.length < 2) {
                        logger.error("Error: Debes especificar la ruta del directorio");
                        printUsage();
                        System.exit(1);
                    }
                    handleDirMode(args[1]);
                    break;
                    
                case "--gui":
                    SwingUtilities.invokeLater(() -> {
                        new MainFrame().setVisible(true);
                    });
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
     * Maneja el modo de transcripción desde archivo
     */
    private static void handleFileMode(String filePath) throws IOException {
        logger.info("Modo: Transcripción desde archivo");
        logger.info("Archivo de entrada: {}", filePath);

        Path inputPath = Paths.get(filePath);

        // Validar que el archivo existe
        if (!Files.exists(inputPath)) {
            logger.error("El archivo no existe: {}", filePath);
            System.exit(1);
        }

        // Inicializar servicio Vosk
        VoskService voskService = null;
        try {
            voskService = new VoskService();

            // Crear transcriptor y procesar archivo
            FileTranscriber transcriber = new FileTranscriber(voskService);
            String transcription = transcriber.transcribe(inputPath);

            // Mostrar resultado
            System.out.println("\n========== TRANSCRIPCIÓN ==========");
            System.out.println(transcription);
            System.out.println("====================================\n");

            // Guardar en carpeta central <project-root>/transcriptions/
            Path projectRoot = Paths.get(".").toAbsolutePath().normalize();
            Path outDir = projectRoot.resolve("transcriptions");
            Files.createDirectories(outDir);

            // Nombre de salida sin extensión
            String baseName = inputPath.getFileName().toString();
            int idx = baseName.lastIndexOf('.');
            String nameNoExt = (idx > 0) ? baseName.substring(0, idx) : baseName;

            Path outputPath = outDir.resolve(nameNoExt + ".txt");
            Files.writeString(outputPath, transcription);
            logger.info("Transcripción guardada en: {}", outputPath);

        } finally {
            if (voskService != null) {
                voskService.close();
            }
        }
    }
    
    /**
     * Maneja el modo de transcripción desde micrófono
     */
    private static void handleMicMode() {
        logger.info("Modo: Transcripción desde micrófono");
        logger.info("Presiona Ctrl+C para detener la transcripción");
        
        // Inicializar servicio Vosk
        VoskService voskService = null;
        try {
            voskService = new VoskService();
            
            // Crear transcriptor de micrófono
            MicTranscriber transcriber = new MicTranscriber(voskService);
            
            System.out.println("\n========== TRANSCRIPCIÓN EN TIEMPO REAL ==========");
            System.out.println("Habla ahora... (Ctrl+C para detener)");
            System.out.println("==================================================\n");
            
            // Preparar carpeta y archivo de salida
            Path projectRoot = Paths.get(".").toAbsolutePath().normalize();
            Path outDir = projectRoot.resolve("transcriptions");
            Files.createDirectories(outDir);
            Path outputFile = outDir.resolve("live_transcription.txt");

            // Consumidor que escribe cada transcripción final en el archivo
            Consumer<String> fileWriter = text -> {
                if (text == null || text.isEmpty()) return;
                try {
                    Files.writeString(outputFile, text + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    logger.error("Error escribiendo transcripción en archivo: {}", outputFile, e);
                }
            };

            // Iniciar transcripción (bloqueante hasta Ctrl+C)
            transcriber.startTranscription(fileWriter);

        } catch (Exception e) {
            logger.error("Error durante transcripción desde micrófono", e);
        } finally {
            if (voskService != null) {
                voskService.close();
            }
        }
    }

    /**
     * Maneja el modo de transcripción por directorio: procesa todos los archivos de audio
     * en la carpeta indicada (extensiones .m4a, .mp3, .wav, .aac)
     */
    private static void handleDirMode(String dirPath) throws IOException {
        logger.info("Modo: Transcripción por directorio");
        logger.info("Directorio de entrada: {}", dirPath);

        Path inputDir = Paths.get(dirPath);
        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            logger.error("El directorio no existe o no es una carpeta: {}", dirPath);
            System.exit(1);
        }

        VoskService voskService = null;
        try {
            voskService = new VoskService();
            FileTranscriber transcriber = new FileTranscriber(voskService);

            // Carpeta central de salida en la raíz del proyecto
            Path projectRoot = Paths.get(".").toAbsolutePath().normalize();
            Path outDir = projectRoot.resolve("transcriptions");
            Files.createDirectories(outDir);

            try (Stream<Path> files = Files.list(inputDir)) {
                files.filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.endsWith(".m4a") || n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".aac");
                }).forEach(p -> {
                    try {
                        logger.info("Procesando archivo: {}", p);
                        String text = transcriber.transcribe(p);

                        // Nombre de salida sin extensión
                        String baseName = p.getFileName().toString();
                        int idx = baseName.lastIndexOf('.');
                        String nameNoExt = (idx > 0) ? baseName.substring(0, idx) : baseName;

                        Path output = outDir.resolve(nameNoExt + ".txt");
                        Files.writeString(output, text);
                        logger.info("Transcripción guardada en: {}", output);
                    } catch (Exception e) {
                        logger.error("Error procesando {}", p, e);
                    }
                });
            }

        } finally {
            if (voskService != null) {
                voskService.close();
            }
        }
    }

    /**
     * Imprime instrucciones de uso
     */
    private static void printUsage() {
        System.out.println("\nUso:");
        System.out.println("  Transcribir archivo:");
        System.out.println("    gradlew run --args=\"--file C:\\ruta\\audio.m4a\"");
        System.out.println();
        System.out.println("  Transcribir desde micrófono:");
        System.out.println("    gradlew run --args=\"--mic\"");
        System.out.println();
        System.out.println("  Transcribir todos los audios de una carpeta:");
        System.out.println("    gradlew run --args=\"--dir src\\main\\resources\\audio\"");
        System.out.println();
    }
}
