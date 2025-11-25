package mx.unam.fi.cine.modelo;

/**
 * Clase base abstracta que define el contrato operativo para todo el personal interno de <b>CineByt</b>.
 * <p>
 * Esta clase extiende a {@link Usuario} para segregar la lógica de los clientes externos de la
 * fuerza laboral interna. Introduce atributos exclusivos del ámbito laboral, como el horario de trabajo.
 * </p>
 * <b>Jerarquía y Extensibilidad:</b>
 * <ul>
 * <li>Es una clase <b>abstracta</b>: No existen "empleados genéricos", deben ser roles específicos.</li>
 * <li><b>Subclases Concretas:</b>
 * <ul>
 * <li>{@link Administrador}: Encargado de la gestión de la cartelera y reportes.</li>
 * <li>{@link VendedorDulceria}: Encargado de las operaciones de venta de alimentos.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author Equipo CineByt
 * @version 1.0
 * @see mx.unam.fi.cine.modelo.Usuario
 * @see mx.unam.fi.cine.modelo.Administrador
 * @see mx.unam.fi.cine.modelo.VendedorDulceria
 */
public abstract class Empleado extends Usuario {

    /**
     * Enumeración que define los turnos laborales válidos en la organización.
     * <p>
     * <b>Decisión de Diseño:</b> Se utiliza un {@code enum} en lugar de cadenas de texto (Strings)
     * para garantizar la seguridad de tipos (Type Safety) y restringir el dominio de valores posibles,
     * evitando inconsistencias en la base de datos o lógica condicional.
     * </p>
     * <ul>
     * <li>{@code MATUTINO}: Turno de apertura y primeras funciones.</li>
     * <li>{@code VESPERTINO}: Turno con mayor afluencia de gente.</li>
     * <li>{@code NOCTURNO}: Turno de cierre y limpieza.</li>
     * </ul>
     */
    public enum Turno { 
        MATUTINO, 
        VESPERTINO, 
        NOCTURNO 
    }

    /**
     * El turno asignado al empleado.
     * Define el horario en el que el usuario tiene permitido operar el sistema o registrar actividad.
     */
    private Turno turno;

    /**
     * Constructor protegido para la inicialización de empleados.
     * <p>
     * Recibe la totalidad de datos requeridos por la cadena de herencia ({@link Persona} -> {@link Usuario})
     * más el parámetro específico de gestión laboral.
     *
     * @param nombre    Nombre de pila.
     * @param apPaterno Apellido paterno.
     * @param apMaterno Apellido materno.
     * @param edad      Edad del empleado.
     * @param nickname  Credencial de acceso (Usuario).
     * @param password  Credencial de acceso (Contraseña).
     * @param email     Correo corporativo o personal.
     * @param telefono  Teléfono de contacto.
     * @param turno     Constante de {@link Turno} asignada (Matutino, Vespertino, Nocturno).
     */
    public Empleado(String nombre, String apPaterno, String apMaterno, int edad,
                    String nickname, String password, String email, String telefono,
                    Turno turno) {
        super(nombre, apPaterno, apMaterno, edad, nickname, password, email, telefono);
        this.turno = turno;
    }

    // ----------------------------------------------------------------------------------
    // Métodos de Acceso (Getters y Setters)
    // ----------------------------------------------------------------------------------

    /**
     * Obtiene el turno laboral actual del empleado.
     * @return El valor del enum {@link Turno}.
     */
    public Turno getTurno() { return turno; }

    /**
     * Asigna o cambia el turno laboral del empleado.
     * Utilizado por la administración para rotación de personal.
     * @param turno El nuevo turno a asignar.
     */
    public void setTurno(Turno turno) { this.turno = turno; }
}
