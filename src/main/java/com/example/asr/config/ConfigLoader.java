package com.example.asr.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * Cargador de configuración desde YAML.
 * <p>
 * Lee `application.yml` desde resources y proporciona acceso a propiedades
 * de forma segura con fallback a valores por defecto.
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public final class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static Map<String, Object> config;

    static {
        loadConfig();
    }

    /**
     * Carga la configuración desde application.yml en resources.
     */
    private static void loadConfig() {
        try {
            InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("application.yml");

            if (input == null) {
                logger.warn("application.yml no encontrado en resources, usando valores por defecto");
                config = Map.of();
                return;
            }

            Yaml yaml = new Yaml();
            config = yaml.load(input);
            logger.info("Configuración cargada desde application.yml");

        } catch (Exception e) {
            logger.error("Error cargando application.yml", e);
            config = Map.of();
        }
    }

    /**
     * Obtiene un valor de configuración anidado.
     * <p>
     * Ejemplo: getConfig("audio.sampleRate") obtiene config.audio.sampleRate
     * </p>
     *
     * @param path ruta anidada (ej: "audio.sampleRate")
     * @param defaultValue valor por defecto si no existe
     * @return valor encontrado o defaultValue
     */
    public static Object getConfig(String path, Object defaultValue) {
        String[] parts = path.split("\\.");
        Object current = config;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
                if (current == null) {
                    return defaultValue;
                }
            } else {
                return defaultValue;
            }
        }

        return current;
    }

    /**
     * Obtiene un valor string de configuración.
     */
    public static String getString(String path, String defaultValue) {
        Object value = getConfig(path, defaultValue);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Obtiene un valor double de configuración.
     */
    public static double getDouble(String path, double defaultValue) {
        Object value = getConfig(path, defaultValue);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Obtiene un valor int de configuración.
     */
    public static int getInt(String path, int defaultValue) {
        Object value = getConfig(path, defaultValue);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Obtiene un valor boolean de configuración.
     */
    public static boolean getBoolean(String path, boolean defaultValue) {
        Object value = getConfig(path, defaultValue);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Constructor privado - clase de utilidad.
     */
    private ConfigLoader() {
        throw new AssertionError("ConfigLoader no debería ser instanciada");
    }
}

