package mx.unam.fi.cine.modelo;

import java.io.*;

/**
 * Clase abstracta que extiende la identidad de una {@link Persona} agregando capacidades de autenticación y contacto.
 * <p>
 * Esta clase actúa como el nivel intermedio en la jerarquía de herencia, transformando a un individuo
 * genérico en un actor del sistema con credenciales de acceso. Es la clase padre directa de todos
 * los roles operativos y clientes.
 * </p>
 * <b>Responsabilidades Arquitectónicas:</b>
 * <ul>
 * <li><b>Seguridad:</b> Gestiona las credenciales (`nickname` y `password`) utilizadas por la clase principal {@code CineByt} durante el proceso de Login.</li>
 * <li><b>Contacto:</b> Centraliza la información de comunicación (`email` y `telefono`).</li>
 * <li><b>Abstracción:</b> No puede ser instanciada directamente; obliga a definir un rol concreto (ej. {@code Cliente}, {@code Administrador}).</li>
 * </ul>
 *
 * @author Equipo CineByt
 * @version 1.0
 * @see mx.unam.fi.cine.modelo.Persona
 * @see mx.unam.fi.cine.modelo.Cliente
 * @see mx.unam.fi.cine.modelo.Empleado
 */
public abstract class Usuario extends Persona implements Serializable {

    /**
     * Identificador de versión para la serialización.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Nombre de usuario único utilizado para el inicio de sesión (Login).
     * Funciona como la llave primaria lógica para la búsqueda de usuarios en la "base de datos" (archivos).
     */
    private String nickname;

    /**
     * Contraseña de acceso al sistema.
     * <p>
     * <b>Nota de Seguridad:</b> En esta versión del sistema, se almacena en texto plano
     * dentro de los archivos serializados. En iteraciones futuras, esto debería ser un hash.
     */
    private String password;

    /**
     * Dirección de correo electrónico del usuario.
     * Utilizado para el envío de confirmaciones de compra o notificaciones administrativas.
     */
    private String email;

    /**
     * Número de teléfono de contacto (Móvil o Fijo).
     */
    private String telefono;

    /**
     * Constructor principal de Usuario.
     * <p>
     * Este constructor orquesta la inicialización completa del objeto:
     * <ol>
     * <li>Invoca a {@code super(...)} para establecer los atributos demográficos en la clase {@link Persona}.</li>
     * <li>Inicializa los atributos propios de credenciales y contacto.</li>
     * </ol>
     *
     * @param nombre    Nombre de pila (Heredado).
     * @param apPaterno Apellido paterno (Heredado).
     * @param apMaterno Apellido materno (Heredado).
     * @param edad      Edad (Heredado).
     * @param nickname  Identificador de acceso único.
     * @param password  Contraseña de acceso.
     * @param email     Correo electrónico de contacto.
     * @param telefono  Número telefónico.
     */
    public Usuario(String nombre, String apPaterno, String apMaterno, int edad,
                   String nickname, String password, String email, String telefono) {
        super(nombre, apPaterno, apMaterno, edad); // Delegación al constructor de la clase base Persona
        this.nickname = nickname;
        this.password = password;
        this.email = email;
        this.telefono = telefono;
    }

    // ----------------------------------------------------------------------------------
    // Métodos de Acceso y Modificación (Getters y Setters)
    // ----------------------------------------------------------------------------------

    /**
     * Obtiene el nombre de usuario (Nick).
     * @return Cadena con el identificador de login.
     */
    public String getNickname() { return nickname; }

    /**
     * Actualiza el nombre de usuario.
     * @param nickname Nuevo identificador.
     */
    public void setNickname(String nickname) { this.nickname = nickname; }

    /**
     * Obtiene la contraseña actual.
     * @return Cadena con la contraseña.
     */
    public String getPassword() { return password; }

    /**
     * Actualiza la contraseña de acceso.
     * @param password Nueva contraseña.
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * Obtiene el correo electrónico.
     * @return Cadena con el email.
     */
    public String getEmail() { return email; }

    /**
     * Actualiza el correo electrónico.
     * @param email Nuevo email.
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Obtiene el número telefónico.
     * @return Cadena con el teléfono.
     */
    public String getTelefono() { return telefono; }

    /**
     * Actualiza el número telefónico.
     * @param telefono Nuevo número.
     */
    public void setTelefono(String telefono) { this.telefono = telefono; }

    /**
     * Representación textual extendida del usuario.
     * <p>
     * Combina la implementación de {@code super.toString()} (Nombre completo)
     * agregando el {@code nickname} entre paréntesis para facilitar la identificación
     * en listas de usuarios o logs del sistema.
     *
     * @return Cadena formato "Nombre Apellidos (Nickname)".
     */
    @Override
    public String toString() {
        return super.toString() + " (" + nickname + ")";
    }
}
