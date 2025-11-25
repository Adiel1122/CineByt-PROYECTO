package mx.unam.fi.cine.modelo;

import java.io.*;

/**
 * Representa la entidad fundamental de información de una película dentro del modelo del sistema <b>CineByt</b>.
 * <p>
 * Esta clase funciona como una plantilla de datos (DTO - Data Transfer Object) que encapsula
 * las propiedades inmutables de una obra cinematográfica. Es utilizada principalmente por:
 * <ul>
 * <li>{@code ControladorAdministrador}: Para dar de alta nuevas funciones, donde la duración
 * de la película es crítica para validar cruces de horarios (regla de los 30 minutos).</li>
 * <li>{@code Funcion}: Clase que asocia una película específica a un horario y una sala.</li>
 * <li>{@code CineByt}: Para el despliegue de información en cartelera.</li>
 * </ul>
 * <p>
 * Implementa {@link java.io.Serializable} para permitir que sus instancias sean convertidas
 * a un flujo de bytes y almacenadas en archivos binarios (.dat) mediante el {@code GestorArchivos}.
 *
 * @author Equipo CineByt
 * @version 1.0
 * @see mx.unam.fi.cine.modelo.Funcion
 * @see mx.unam.fi.cine.modelo.GestorArchivos
 */
public class Pelicula implements Serializable {

    /**
     * Identificador de versión para la serialización.
     * Garantiza la compatibilidad entre el objeto serializado y la clase actual.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Título oficial de la película.
     * Se utiliza como identificador visual principal en los menús y reportes.
     */
    private String titulo;

    /**
     * Género o categoría cinematográfica (ej. "Acción", "Drama", "Ciencia Ficción").
     * Utilizado para filtros o descripción breve en la interfaz de usuario.
     */
    private String genero;

    /**
     * Resumen breve de la trama de la película.
     * Proporciona contexto al {@code Cliente} al consultar la cartelera.
     */
    private String sinopsis;

    /**
     * Duración total de la película expresada en minutos.
     * <p>
     * <b>Importancia Arquitectónica:</b> Este valor es fundamental para el cálculo
     * de disponibilidad de salas. El {@code ControladorAdministrador} utiliza este dato,
     * sumado a un tiempo de limpieza, para determinar cuándo una sala queda libre
     * para la siguiente {@code Funcion}.
     */
    private int duracionMinutos;

    /**
     * Constructor principal para inicializar una nueva instancia de Pelicula.
     *
     * @param titulo          Título de la película. No debe ser nulo.
     * @param genero          Género(s) de la película.
     * @param sinopsis        Breve descripción de la trama.
     * @param duracionMinutos Duración total en minutos (entero positivo).
     * Esencial para cálculos de horarios en {@code Funcion}.
     */
    public Pelicula(String titulo, String genero, String sinopsis, int duracionMinutos) {
        this.titulo = titulo;
        this.genero = genero;
        this.sinopsis = sinopsis;
        this.duracionMinutos = duracionMinutos;
    }

    // ----------------------------------------------------------------------------------
    // Métodos de Acceso (Getters y Setters)
    // ----------------------------------------------------------------------------------

    /**
     * Obtiene el título de la película.
     * @return El título como cadena de caracteres.
     */
    public String getTitulo() { return titulo; }

    /**
     * Actualiza el título de la película.
     * @param titulo El nuevo título a asignar.
     */
    public void setTitulo(String titulo) { this.titulo = titulo; }

    /**
     * Obtiene el género de la película.
     * @return El género como cadena de caracteres.
     */
    public String getGenero() { return genero; }

    /**
     * Actualiza el género de la película.
     * @param genero El nuevo género a asignar.
     */
    public void setGenero(String genero) { this.genero = genero; }

    /**
     * Obtiene la sinopsis de la película.
     * @return La sinopsis como cadena de caracteres.
     */
    public String getSinopsis() { return sinopsis; }

    /**
     * Actualiza la sinopsis de la película.
     * @param sinopsis La nueva sinopsis a asignar.
     */
    public void setSinopsis(String sinopsis) { this.sinopsis = sinopsis; }

    /**
     * Obtiene la duración en minutos brutos.
     * Este valor es el utilizado para operaciones matemáticas de tiempo.
     * @return La duración en minutos (int).
     */
    public int getDuracionMinutos() { return duracionMinutos; }

    /**
     * Actualiza la duración de la película.
     * Nota: Modificar esto podría afectar la validación de funciones ya creadas.
     * @param duracionMinutos La nueva duración en minutos.
     */
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    // ----------------------------------------------------------------------------------
    // Métodos de Lógica de Presentación
    // ----------------------------------------------------------------------------------

    /**
     * Transforma la duración almacenada en minutos a un formato legible para el usuario (HH:mm).
     * <p>
     * Realiza operaciones de división entera y módulo para desglosar el tiempo total:
     * <ul>
     * <li>Horas: {@code duracionMinutos / 60}</li>
     * <li>Minutos: {@code duracionMinutos % 60}</li>
     * </ul>
     * El resultado se formatea asegurando dos dígitos para cada campo (ej. 02:05).
     *
     * @return Una cadena con el formato "HH:mm".
     */
    public String getDuracionFormato() {
        int horas = duracionMinutos / 60;
        int minutos = duracionMinutos % 60;
        return String.format("%02d:%02d", horas, minutos);
    }

    /**
     * Proporciona una representación en cadena del estado del objeto.
     * Útil para depuración y para mostrar resúmenes rápidos en la consola del {@code CineByt}.
     *
     * @return Cadena con Título, Género y Duración formateada.
     */
    @Override
    public String toString() {
        return String.format("Título: %s | Género: %s | Duración: %s",
                titulo, genero, getDuracionFormato());
    }
}
