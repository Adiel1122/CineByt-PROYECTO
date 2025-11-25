package mx.unam.fi.cine.modelo;

import java.io.*;

/**
 * Clase base abstracta que define la identidad fundamental de cualquier individuo en el ecosistema <b>CineByt</b>.
 * <p>
 * Esta clase actúa como el nivel superior de la jerarquía de generalización del Modelo.
 * Su propósito es encapsular los atributos demográficos comunes (Nombre, Apellidos, Edad)
 * para evitar la duplicidad de código en las entidades concretas.
 * </p>
 * <b>Jerarquía de Herencia:</b>
 * <ul>
 * <li>{@code Persona} (Abstracta) -> Define identidad civil.</li>
 * <li>   ↳ {@link Usuario} (Abstracta) -> Agrega credenciales de acceso al sistema.</li>
 * <li>      ↳ {@code Cliente}, {@code Empleado}, etc.</li>
 * </ul>
 * <b>Persistencia:</b>
 * Implementa {@link Serializable} para permitir que todos los descendientes puedan
 * ser almacenados en archivos binarios (.dat) mediante el {@code GestorArchivos}.
 *
 * @author Equipo CineByt
 * @version 1.0
 * @see mx.unam.fi.cine.modelo.Usuario
 */
public abstract class Persona implements Serializable {

    /**
     * Identificador de versión para la serialización.
     * Esencial para mantener compatibilidad en la herencia durante la persistencia.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Nombre(s) de pila de la persona.
     */
    private String nombre;

    /**
     * Primer apellido (Paterno).
     * Utilizado para búsquedas y ordenamiento alfabético en reportes.
     */
    private String apPaterno;

    /**
     * Segundo apellido (Materno).
     */
    private String apMaterno;

    /**
     * Edad de la persona en años cumplidos.
     * <p>
     * <b>Nota de Negocio:</b> Este atributo puede ser utilizado por controladores
     * para validar permisos, como la venta de boletos para películas con restricción
     * de clasificación (ej. Clasificación C).
     */
    private int edad;

    /**
     * Constructor protegido para inicializar los atributos base de la Persona.
     * <p>
     * Al ser una clase abstracta, este constructor solo es invocado mediante
     * {@code super()} desde las subclases (como {@code Usuario}).
     *
     * @param nombre    Nombre(s) de la persona.
     * @param apPaterno Apellido paterno.
     * @param apMaterno Apellido materno.
     * @param edad      Edad de la persona (entero positivo).
     */
    public Persona(String nombre, String apPaterno, String apMaterno, int edad) {
        this.nombre = nombre;
        this.apPaterno = apPaterno;
        this.apMaterno = apMaterno;
        this.edad = edad;
    }

    // ----------------------------------------------------------------------------------
    // Métodos de Acceso (Getters y Setters)
    // ----------------------------------------------------------------------------------

    /**
     * Obtiene el nombre de pila.
     * @return Cadena con el nombre.
     */
    public String getNombre() { return nombre; }

    /**
     * Actualiza el nombre de pila.
     * @param nombre Nuevo nombre.
     */
    public void setNombre(String nombre) { this.nombre = nombre; }

    /**
     * Obtiene el apellido paterno.
     * @return Cadena con el apellido paterno.
     */
    public String getApPaterno() { return apPaterno; }

    /**
     * Actualiza el apellido paterno.
     * @param apPaterno Nuevo apellido paterno.
     */
    public void setApPaterno(String apPaterno) { this.apPaterno = apPaterno; }

    /**
     * Obtiene el apellido materno.
     * @return Cadena con el apellido materno.
     */
    public String getApMaterno() { return apMaterno; }

    /**
     * Actualiza el apellido materno.
     * @param apMaterno Nuevo apellido materno.
     */
    public void setApMaterno(String apMaterno) { this.apMaterno = apMaterno; }

    /**
     * Obtiene la edad actual.
     * @return Edad en años.
     */
    public int getEdad() { return edad; }

    /**
     * Actualiza la edad.
     * @param edad Nueva edad.
     */
    public void setEdad(int edad) { this.edad = edad; }

    /**
     * Genera una representación textual del nombre completo de la persona.
     * Concatena nombre y ambos apellidos con espacios simples.
     *
     * @return Cadena formato "Nombre ApPaterno ApMaterno".
     */
    @Override
    public String toString() {
        return nombre + " " + apPaterno + " " + apMaterno;
    }
}
