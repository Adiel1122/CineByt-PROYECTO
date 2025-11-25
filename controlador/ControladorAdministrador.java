package mx.unam.fi.cine.controlador;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import mx.unam.fi.cine.modelo.*;

/**
 * Controlador encargado de la gestión administrativa y configuración del sistema <b>CineByt</b>.
 * <p>
 * Esta clase orquesta los flujos de trabajo exclusivos para el rol de {@link Administrador}.
 * Actúa como puente entre la entrada de datos del usuario (Consola) y la actualización del Modelo
 * (Listas de Películas, Funciones y Usuarios).
 * </p>
 * <b>Responsabilidades Principales:</b>
 * <ul>
 * <li><b>Gestión de Catálogo:</b> Alta de nuevas películas ({@link Pelicula}).</li>
 * <li><b>Programación:</b> Creación de funciones ({@link Funcion}) validando reglas de no-superposición de horarios y tiempos de limpieza.</li>
 * <li><b>Recursos Humanos:</b> Registro de nuevos empleados ({@link Administrador} o {@link VendedorDulceria}) con sus propiedades específicas.</li>
 * <li><b>Auditoría:</b> Visualización del historial de compras de los clientes.</li>
 * </ul>
 *
 * @author Equipo CineByt
 * @version 3.0
 * @see mx.unam.fi.cine.modelo.Administrador
 * @see mx.unam.fi.cine.modelo.GestorArchivos
 */
public class ControladorAdministrador {

    /** Referencia a la lista maestra de películas en memoria. */
    private List<Pelicula> peliculas;
    
    /** Referencia a la lista maestra de funciones (cartelera) en memoria. */
    private List<Funcion> funciones;
    
    /** Referencia a la lista maestra de usuarios (empleados y clientes) en memoria. */
    private List<Usuario> usuarios;
    
    /** Objeto para la lectura de datos desde la consola estándar. */
    private Scanner entrada;

    /**
     * Constructor del Controlador Administrativo.
     * <p>
     * Recibe las referencias a las listas maestras cargadas en {@code CineByt}.
     * <b>Nota de Diseño:</b> Se utiliza inyección de dependencias simple para asegurar que
     * los cambios realizados aquí se reflejen en toda la aplicación (Singleton implícito por referencia).
     *
     * @param peliculas Lista mutable de películas.
     * @param funciones Lista mutable de funciones.
     * @param usuarios  Lista mutable de usuarios.
     */
    public ControladorAdministrador(List<Pelicula> peliculas, List<Funcion> funciones, List<Usuario> usuarios) {
        this.peliculas = peliculas;
        this.funciones = funciones;
        this.usuarios = usuarios;
        this.entrada = new Scanner(System.in);
    }

    // ==========================================
    // 1. ALTA DE PELÍCULA
    // ==========================================
    
    /**
     * Ejecuta el flujo de registro de una nueva Película en el catálogo.
     * <p>
     * Pasos del proceso:
     * <ol>
     * <li>Solicita los metadatos (Título, Género, Sinopsis, Duración).</li>
     * <li>Instancia un nuevo objeto {@link Pelicula}.</li>
     * <li>Lo agrega a la lista en memoria.</li>
     * <li>Invoca la persistencia inmediata mediante {@link #guardarDatos()}.</li>
     * </ol>
     */
    public void darAltaPelicula() {
        System.out.println("\n--- ALTA DE PELÍCULA ---");
        try {
            System.out.print("Título: ");
            String titulo = entrada.nextLine();

            System.out.print("Género(s): ");
            String genero = entrada.nextLine();

            System.out.print("Sinopsis: ");
            String sinopsis = entrada.nextLine();

            int duracion = leerEntero("Duración en minutos (ej. 120): ");

            Pelicula nuevaPeli = new Pelicula(titulo, genero, sinopsis, duracion);
            peliculas.add(nuevaPeli);
            
            // Guardar cambios en disco inmediatamente para evitar pérdida de datos
            guardarDatos();
            System.out.println(">> Película registrada exitosamente.");

        } catch (Exception e) {
            System.out.println("Error al registrar película: " + e.getMessage());
        }
    }

    // ==========================================
    // 2. ALTA DE FUNCIÓN
    // ==========================================
    
    /**
     * Orquesta el complejo flujo de programación de una nueva Función.
     * <p>
     * Este método implementa una serie de validaciones secuenciales requeridas por el negocio:
     * <ol>
     * <li><b>Selección de Contenido:</b> Elige una {@link Pelicula} existente.</li>
     * <li><b>Selección Temporal:</b> Define una fecha ({@link LocalDate}).</li>
     * <li><b>Selección Espacial:</b> Elige una {@link Sala} (A, B o VIP).</li>
     * <li><b>Visualización:</b> Muestra la parrilla actual de esa sala en esa fecha para ayudar al admin.</li>
     * <li><b>Confirmación:</b> Solicita confirmación explícita antes de pedir la hora exacta.</li>
     * <li><b>Validación Crítica:</b> Solicita la hora de inicio y verifica cruces de horario (incluyendo 30 min de limpieza)
     * mediante {@link #validarDisponibilidadSala}.</li>
     * </ol>
     * Si la validación falla, el sistema impide el alta y permite reintentar o cancelar.
     */
    public void darAltaFuncion() {
        System.out.println("\n--- ALTA DE FUNCIÓN ---");
        
        if (peliculas.isEmpty()) {
            System.out.println("No hay películas registradas. Registre una primero.");
            return;
        }

        // 1. Seleccionar Película del catálogo
        System.out.println("Películas registradas:");
        for (int i = 0; i < peliculas.size(); i++) System.out.println((i + 1) + ". " + peliculas.get(i).getTitulo());
        
        int numPeli = leerEntero("Número de película: ") - 1;
        if (numPeli < 0 || numPeli >= peliculas.size()) return;
        Pelicula peli = peliculas.get(numPeli);

        // 2. Definir Fecha
        LocalDate fecha = leerFecha("Fecha (dd/MM/yyyy): ");
        if (fecha == null) return;

        // 3. Seleccionar Sala
        System.out.println("Sala: 1. Sala A | 2. Sala B | 3. Sala VIP");
        int opSala = leerEntero("Seleccione una opción: ");
        String nombreSala = (opSala == 1) ? "Sala A" : (opSala == 2) ? "Sala B" : (opSala == 3) ? "Sala VIP" : null;
        if (nombreSala == null) return;

        // 4. Mostrar Programación Actual (Ayuda visual para el usuario)
        System.out.println("\n--- Programación " + nombreSala + " " + fecha + " ---");
        boolean siHay = false;
        for (Funcion funcion : funciones) {
            // Filtra funciones que coincidan en Sala y Día
            if (funcion.getSala().getNombre().equals(nombreSala) && funcion.getHorario().toLocalDate().equals(fecha)) {
                LocalTime fin = funcion.getHorario().toLocalTime().plusMinutes(funcion.getPelicula().getDuracionMinutos());
                System.out.println(funcion.getHorario().toLocalTime() + " - " + fin + " | " + funcion.getPelicula().getTitulo());
                siHay = true;
            }
        }
        if (!siHay) System.out.println("(Libre)");

        // 5. Prompt Estricto de decisión
        while (true) {
            System.out.print("Escriba 'Alta' para registrar o 'Cancelar' para salir: ");
            String eleccion = entrada.nextLine();
            
            if (eleccion.equalsIgnoreCase("Cancelar")) return;
            
            if (eleccion.equalsIgnoreCase("Alta")) {
                // 6. Hora y 7. Validación en Bucle
                boolean registrada = false;
                while (!registrada) {
                    try {
                        System.out.println("Ingrese hora inicio (hh y mm separados):");
                        int hh = leerEntero("Hora (00-23): ");
                        int mm = leerEntero("Minuto (00-59): ");
                        
                        LocalDateTime inicio = LocalDateTime.of(fecha, LocalTime.of(hh, mm));
                        
                        // INVOCACIÓN DE REGLA DE NEGOCIO: Disponibilidad + Limpieza
                        if (validarDisponibilidadSala(nombreSala, inicio, peli.getDuracionMinutos())) {
                            Funcion nuevaFuncion = new Funcion(peli, nombreSala, inicio);
                            funciones.add(nuevaFuncion);
                            guardarDatos();
                            System.out.println(">> Función registrada. ID: " + nuevaFuncion.getIdFuncion());
                            registrada = true;
                            return; // Salir al menú tras éxito
                        } else {
                            System.out.println("NO ES POSIBLE dar de alta: Cruce de horarios (se requieren 30 min para limpieza de salas).");
                            // Rompe el loop interno de hora para volver a preguntar Alta/Cancelar
                            break; 
                        }
                    } catch (Exception e) {
                        System.out.println("Hora inválida.");
                    }
                }
            }
        }
    }

    // ==========================================
    // 3. REGISTRO DE EMPLEADOS
    // ==========================================

    /**
     * Gestiona el alta de personal interno, actuando como una fábrica (Factory) de usuarios.
     * <p>
     * Permite crear instancias de {@link Administrador} o {@link VendedorDulceria}.
     * Solicita los datos comunes (Persona/Usuario) y luego bifurca la lógica para pedir
     * los datos específicos de cada rol (Turno, Día de descanso o Tipo de Admin).
     */
    public void registrarEmpleado() {
        System.out.println("\n--- REGISTRO DE NUEVO EMPLEADO ---");
            // 1. Pedir datos comunes (validación de entrada numérica integrada)
            System.out.print("Nombre(s): "); String nombre = entrada.nextLine();
            System.out.print("Ap. Paterno: "); String apPaterno = entrada.nextLine();
            System.out.print("Ap. Materno: "); String apMaterno = entrada.nextLine();
            int edad = leerEntero("Edad: ");
            System.out.print("Nickname: "); String nick = entrada.nextLine();
            System.out.print("Contraseña: "); String contra = entrada.nextLine();
            System.out.print("Email: "); String email = entrada.nextLine();
            System.out.print("Celular: "); String cel = entrada.nextLine();
            
            // 2. Lógica de asignación de Turno (Enum)
            System.out.println("Turno: 1. Matutino | 2. Vespertino | 3. Nocturno");
            Empleado.Turno turno = Empleado.Turno.MATUTINO; // Default
            int numTurno = leerEntero("Seleccione una opción: ");
            if(numTurno == 2) turno = Empleado.Turno.VESPERTINO;
            if(numTurno == 3) turno = Empleado.Turno.NOCTURNO;

            // 3. Selección de Rol y Creación Polimórfica
            System.out.println("Tipo: 1. Administrador | 2. Vendedor de dulcería");
            int tipo = leerEntero("Seleccione una opción: ");
            
            if (tipo == 1) {
                // Configuración específica de Admin
                System.out.println("¿Es administrador de fin de semana? (1. Sí / 2. No)");
                boolean esFin = leerEntero("Seleccione una opción: ") == 1;
                usuarios.add(new Administrador(nombre, apPaterno, apMaterno, edad, nick, contra, email, cel, turno, esFin));
            } else {
                // Configuración específica de Vendedor
                System.out.print("Día de descanso: ");
                String descanso = entrada.nextLine();
                usuarios.add(new VendedorDulceria(nombre, apPaterno, apMaterno, edad, nick, contra, email, cel, turno, descanso));
            }
            
            // 4. Persistencia
            guardarDatos();
            System.out.println("Empleado registrado.");
    }

    // ==========================================
    // MÉTODOS AUXILIARES Y DE VALIDACIÓN
    // ==========================================
    
    /**
     * Consulta el historial de transacciones de un cliente específico.
     * <p>
     * Realiza una búsqueda en dos fases:
     * <ol>
     * <li><b>En Memoria:</b> Filtra la lista de usuarios para encontrar coincidencias por nickname.</li>
     * <li><b>En Archivo:</b> Si se selecciona un usuario, lee su archivo de tickets asociado ({@code tickets_NICK.txt})
     * generado por el {@code ControladorCompra}.</li>
     * </ol>
     */
    public void verHistorialCliente() {
        System.out.println("\n--- VER HISTORIAL DE CLIENTE ---");
        System.out.print("Ingrese nickname o parte de él: ");
        String busqueda = entrada.nextLine();

        // Filtrar clientes usando instanceof
        List<Cliente> coincidencias = new ArrayList<>();
        for (Usuario usuario : usuarios) {
            if (usuario instanceof Cliente && usuario.getNickname().contains(busqueda)) {
                coincidencias.add((Cliente) usuario);
            }
        }

        if (coincidencias.isEmpty()) {
            System.out.println("No se encontraron coincidencias.");
            return;
        }

        // Mostrar Lista Numerada de resultados
        for (int i = 0; i < coincidencias.size(); i++) {
            Cliente cliente = coincidencias.get(i);
            // Verifica existencia de historial en disco sin cargarlo todo aún
            int cantidadBoletos = contarBoletosCliente(cliente.getNickname());
            System.out.println((i + 1) + ". " + cliente.getNickname() + " (Boletos comprados: " + cantidadBoletos + ")");
        }

        int eleccion = leerEntero("Seleccione usuario (0 salir): ") - 1;
        if (eleccion < 0 || eleccion >= coincidencias.size()) return;

        Cliente clienteSelec = coincidencias.get(eleccion);
        mostrarDetalleBoletos(clienteSelec);
    }

    // MÉTODOS AUXILIARES PRIVADOS PARA HISTORIAL

    /**
     * Cuenta las líneas del archivo de historial de un cliente para mostrar un resumen.
     * @param nickname Identificador del usuario.
     * @return Número de registros encontrados, o 0 si no existe archivo.
     */
    private int contarBoletosCliente(String nickname) {
        String archivo = "tickets_" + nickname + ".txt";
        try {
            if (GestorArchivos.existeArchivo(archivo)) {
                return GestorArchivos.leerArchivoTexto(archivo).size();
            }
        } catch (IOException e) {}
        return 0;
    }

    /**
     * Lee e imprime el contenido completo del archivo de tickets del cliente.
     * @param cliente Objeto Cliente seleccionado.
     */
    private void mostrarDetalleBoletos(Cliente cliente) {
        System.out.println("\n--- Detalle para " + cliente.getNombre() + " (" + cliente.getNickname() + ") ---");
        String archivo = "tickets_" + cliente.getNickname() + ".txt";
        try {
            if (GestorArchivos.existeArchivo(archivo)) {
                List<String> lineas = GestorArchivos.leerArchivoTexto(archivo);
                for (String linea : lineas) System.out.println(linea); 
            } else {
                System.out.println("Este cliente no tiene historial de boletos.");
            }
        } catch (IOException e) {
            System.out.println("Error leyendo historial.");
        }
    }
    
    /**
     * Valida matemáticamente si es posible agendar una función sin conflictos de horario.
     * <p>
     * <b>Regla de Negocio (30 Minutos):</b>
     * Para considerar una sala "Disponible", debe existir un hueco entre funciones que considere
     * la duración de la película MÁS 30 minutos de limpieza/preparación.
     * </p>
     * <b>Lógica de Colisión:</b>
     * Existe colisión si los intervalos de tiempo [InicioA, FinA+30] y [InicioB, FinB+30] se superponen.
     * Matemáticamente comprobado como:
     * {@code (InicioNueva < FinExistente + 30) AND (FinNueva > InicioExistente - 30)}
     *
     * @param nombreSala    Nombre de la sala objetivo.
     * @param inicioNueva   Fecha y hora de inicio propuesta.
     * @param duracionMinutos Duración de la película a proyectar.
     * @return {@code true} si la sala está libre (respetando limpieza); {@code false} si hay cruce.
     */
    private boolean validarDisponibilidadSala(String nombreSala, LocalDateTime inicioNueva, int duracionMinutos) {
        LocalDateTime finNueva = inicioNueva.plusMinutes(duracionMinutos);

        for (Funcion f : funciones) {
            // Solo verificar colisiones en la misma sala física
            if (f.getSala().getNombre().equals(nombreSala)) {
                LocalDateTime inicioExistente = f.getHorario();
                LocalDateTime finExistente = inicioExistente.plusMinutes(f.getPelicula().getDuracionMinutos());

                // Definición de márgenes de seguridad para limpieza
                LocalDateTime finExistenteMasLimpieza = finExistente.plusMinutes(30);
                LocalDateTime inicioExistenteMenosLimpieza = inicioExistente.minusMinutes(30);

                // Comprobación de solapamiento de intervalos
                boolean colision = inicioNueva.isBefore(finExistenteMasLimpieza) && finNueva.isAfter(inicioExistenteMenosLimpieza);
                
                if (colision) return false;
            }
        }
        return true;
    }

    /**
     * Utilidad para leer enteros de forma segura, evitando que el programa colapse si el usuario ingresa letras.
     * @param mensaje Prompt a mostrar.
     * @return Un entero válido.
     */
    private int leerEntero(String mensaje) {
        while (true) {
            try {
                System.out.print(mensaje);
                return Integer.parseInt(entrada.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Por favor ingrese un número válido.");
            }
        }
    }

    /**
     * Utilidad para leer y parsear fechas en formato dd/MM/yyyy.
     * @param mensaje Prompt a mostrar.
     * @return Objeto LocalDate o null si el formato es incorrecto.
     */
    private LocalDate leerFecha(String mensaje) {
        try {
            System.out.print(mensaje);
            String fechaStr = entrada.nextLine();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(fechaStr, formatter);
        } catch (DateTimeParseException e) {
            System.out.println("Formato de fecha inválido. Use dd/MM/yyyy");
            return null;
        }
    }

    /**
     * Invoca al GestorArchivos para serializar el estado actual de las listas maestras.
     * Se llama después de cualquier operación de escritura (Alta/Modificación).
     */
    private void guardarDatos() {
        try {
            GestorArchivos.guardarObjeto("peliculas.dat", peliculas);
            GestorArchivos.guardarObjeto("funciones.dat", funciones);
            GestorArchivos.guardarObjeto("usuarios.dat", usuarios);
        } catch (IOException e) {
            System.out.println("Error crítico al guardar datos: " + e.getMessage());
        }
    }
}
