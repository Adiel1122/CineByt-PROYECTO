package mx.unam.fi.cine.modelo;

import java.io.*;

/**
 * Representa la unidad atómica de ocupación dentro de una sala en el ecosistema <b>CineByt</b>.
 * <p>
 * Esta clase modela un asiento individual identificable por coordenadas (fila y número).
 * Mantiene un estado binario de disponibilidad que es consultado y modificado durante
 * el proceso de venta de boletos.
 * </p>
 * <b>Relación Arquitectónica:</b>
 * <ul>
 * <li>Es parte del <b>Modelo</b> y es serializable.</li>
 * <li>Es gestionado directamente por la clase {@link Sala} (Relación de Composición).</li>
 * <li>Su estado ({@code ocupado}) es volátil y específico por cada instancia de {@link Funcion}.
 * Cuando se crea una Función, se clona la estructura de la Sala, permitiendo que el mismo
 * asiento físico tenga estados diferentes en horarios diferentes.</li>
 * </ul>
 *
 * @author Equipo CineByt
 * @version 1.0
 * @see mx.unam.fi.cine.modelo.Sala
 * @see mx.unam.fi.cine.controlador.ControladorCompra
 */
public class Asiento implements Serializable {

    /**
     * Identificador de versión para asegurar la consistencia durante la serialización/deserialización.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Identificador de la fila a la que pertenece el asiento (ej. 'A', 'B', 'C').
     * Generalmente representa la distancia respecto a la pantalla.
     */
    private char fila;

    /**
     * Número consecutivo del asiento dentro de su fila.
     * Identifica la posición horizontal del espectador.
     */
    private int numero;

    /**
     * Bandera de estado que indica la disponibilidad del asiento.
     * <ul>
     * <li>{@code true}: El asiento ha sido vendido y no puede ser seleccionado nuevamente.</li>
     * <li>{@code false}: El asiento está libre y disponible para compra.</li>
     * </ul>
     * Este atributo es vital para la validación de concurrencia en el {@code ControladorCompra}.
     */
    private boolean ocupado;

    /**
     * Constructor para inicializar un asiento en una ubicación específica.
     * <p>
     * Por defecto, el asiento se inicializa como <b>Libre</b> ({@code ocupado = false}).
     *
     * @param fila   Letra que indica la fila (A-Z).
     * @param numero Número entero que indica la posición en la fila.
     */
    public Asiento(char fila, int numero) {
        this.fila = fila;
        this.numero = numero;
        this.ocupado = false; // El estado inicial predeterminado es libre
    }

    /**
     * Obtiene la letra de la fila.
     * @return El carácter identificador de la fila.
     */
    public char getFila() {
        return fila;
    }

    /**
     * Obtiene el número del asiento.
     * @return El entero identificador de la posición.
     */
    public int getNumero() {
        return numero;
    }

    /**
     * Verifica el estado actual de disponibilidad del asiento.
     * Utilizado por la Vista para determinar si pintar el asiento de rojo (ocupado) o verde (libre).
     *
     * @return {@code true} si el asiento está ocupado/vendido; {@code false} en caso contrario.
     */
    public boolean isOcupado() {
        return ocupado;
    }

    /**
     * Modifica el estado de ocupación del asiento.
     * <p>
     * Este método es invocado principalmente por el {@code ControladorCompra}
     * una vez que la transacción de venta ha sido finalizada exitosamente.
     *
     * @param ocupado El nuevo estado del asiento (true para ocupar, false para liberar).
     */
    public void setOcupado(boolean ocupado) {
        this.ocupado = ocupado;
    }

    /**
     * Representación en cadena del estado del asiento.
     * Formato: "FilaNumero [Estado]".
     * Útil para logs de auditoría o depuración de la matriz de sala.
     *
     * @return Cadena descriptiva del asiento.
     */
    @Override
    public String toString() {
        return String.format("%c%d [%s]", fila, numero, (ocupado ? "Ocupado" : "Libre"));
    }
}
