package com.example.asr.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor centralizado del estado de la aplicación.
 * <p>
 * Responsable de:
 * <ul>
 *   <li>Mantener el estado actual de la aplicación</li>
 *   <li>Validar transiciones de estado</li>
 *   <li>Notificar cambios a observers</li>
 *   <li>Prevenir operaciones inválidas según el estado</li>
 * </ul>
 * </p>
 * <p>
 * Ejemplo de uso:
 * <pre>
 *   ApplicationStateManager stateManager = new ApplicationStateManager();
 *   stateManager.addStateChangeListener((old, newState) -> {
 *       System.out.println("Estado: " + old + " → " + newState);
 *   });
 *   stateManager.transitionTo(AppState.PROCESSING);
 * </pre>
 * </p>
 *
 * @author by-nreyes, nelson ruiz
 * @version 1.0
 * @see AppState
 */
public class ApplicationStateManager {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStateManager.class);

    /** Estado actual de la aplicación */
    private final AtomicReference<AppState> currentState = new AtomicReference<>(AppState.INITIALIZING);

    /** Listeners para cambios de estado */
    private final List<StateChangeListener> listeners = new ArrayList<>();

    /**
     * Interfaz para escuchar cambios de estado.
     */
    @FunctionalInterface
    public interface StateChangeListener {
        /**
         * Se invoca cuando cambia el estado de la aplicación.
         *
         * @param previousState estado anterior
         * @param newState estado nuevo
         */
        void onStateChange(AppState previousState, AppState newState);
    }

    /**
     * Obtiene el estado actual de la aplicación.
     *
     * @return estado actual
     */
    public AppState getCurrentState() {
        return currentState.get();
    }

    /**
     * Intenta transicionar a un nuevo estado.
     * <p>
     * Si la transición es válida, se notifica a todos los listeners.
     * Si es inválida, se registra un warning y no ocurre cambio.
     * </p>
     *
     * @param newState estado destino
     * @return true si la transición fue exitosa, false si fue rechazada
     * @throws IllegalArgumentException si newState es null
     */
    public boolean transitionTo(AppState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("newState no puede ser null");
        }

        AppState previousState = currentState.get();

        // Validar transición
        if (!previousState.canTransitionTo(newState)) {
            logger.warn("Transición inválida: {} → {}", previousState, newState);
            return false;
        }

        // Aplicar transición
        currentState.set(newState);
        logger.info("Transición de estado: {} → {}", previousState, newState);

        // Notificar listeners
        notifyStateChange(previousState, newState);

        return true;
    }

    /**
     * Añade un listener para cambios de estado.
     *
     * @param listener listener a añadir
     * @throws IllegalArgumentException si listener es null
     */
    public void addStateChangeListener(StateChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener no puede ser null");
        }
        listeners.add(listener);
    }

    /**
     * Elimina un listener de cambios de estado.
     *
     * @param listener listener a eliminar
     */
    public void removeStateChangeListener(StateChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Verifica si la aplicación está lista para procesar.
     *
     * @return true si el estado es READY
     */
    public boolean isReady() {
        return currentState.get() == AppState.READY;
    }

    /**
     * Verifica si la aplicación está procesando.
     *
     * @return true si el estado es PROCESSING
     */
    public boolean isProcessing() {
        return currentState.get() == AppState.PROCESSING;
    }

    /**
     * Verifica si la aplicación está en estado de error.
     *
     * @return true si el estado es ERROR
     */
    public boolean hasError() {
        return currentState.get() == AppState.ERROR;
    }

    /**
     * Verifica si la aplicación está cerrando.
     *
     * @return true si el estado es SHUTTING_DOWN
     */
    public boolean isShuttingDown() {
        return currentState.get() == AppState.SHUTTING_DOWN;
    }

    /**
     * Notifica a todos los listeners sobre el cambio de estado.
     *
     * @param previousState estado anterior
     * @param newState estado nuevo
     */
    private void notifyStateChange(AppState previousState, AppState newState) {
        for (StateChangeListener listener : listeners) {
            try {
                listener.onStateChange(previousState, newState);
            } catch (Exception e) {
                logger.error("Error en listener de cambio de estado", e);
            }
        }
    }
}

