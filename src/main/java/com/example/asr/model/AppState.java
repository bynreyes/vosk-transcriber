package com.example.asr.model;

/**
 * Estados posibles de la aplicación.
 * <p>
 * Representa el ciclo de vida de la aplicación y sus transiciones válidas:
 * <ul>
 *   <li>INITIALIZING → READY o ERROR</li>
 *   <li>READY → PROCESSING o SHUTTING_DOWN</li>
 *   <li>PROCESSING → READY o ERROR</li>
 *   <li>ERROR → SHUTTING_DOWN</li>
 *   <li>SHUTTING_DOWN → (final)</li>
 * </ul>
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 */
public enum AppState {
    /**
     * La aplicación se está inicializando.
     * Cargando recursos (modelo Vosk, configuración, etc.).
     */
    INITIALIZING("Inicializando"),

    /**
     * La aplicación está lista para procesar.
     * Recursos cargados, esperando entrada del usuario.
     */
    READY("Listo"),

    /**
     * La aplicación está procesando audio.
     * Transcripción en progreso, usuario no puede iniciar nuevas sesiones.
     */
    PROCESSING("Procesando"),

    /**
     * Ocurrió un error durante la operación.
     * La aplicación puede intentar recuperarse o proceder al cierre.
     */
    ERROR("Error"),

    /**
     * La aplicación se está cerrando.
     * Liberando recursos, limpiando archivos temporales.
     */
    SHUTTING_DOWN("Cerrando");

    private final String displayName;

    /**
     * Crea un nuevo estado con nombre para mostrar.
     *
     * @param displayName nombre legible del estado
     */
    AppState(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Obtiene el nombre para mostrar en UI.
     *
     * @return nombre del estado
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Verifica si la transición desde este estado a otro es válida.
     *
     * @param newState estado destino
     * @return true si la transición es válida
     */
    public boolean canTransitionTo(AppState newState) {
        if (newState == this) return false; // No transicionar a sí mismo

        return switch (this) {
            case INITIALIZING -> newState == READY || newState == ERROR;
            case READY -> newState == PROCESSING || newState == SHUTTING_DOWN;
            case PROCESSING -> newState == READY || newState == ERROR;
            case ERROR -> newState == SHUTTING_DOWN;
            case SHUTTING_DOWN -> false; // Terminal
        };
    }
}

