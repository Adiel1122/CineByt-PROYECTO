package mx.unam.fi.cine;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import mx.unam.fi.cine.controlador.*;
import mx.unam.fi.cine.modelo.*;

/**
 * Clase principal (Main Class) y punto de entrada de la aplicación <b>CineByt</b>.
 * <p>
 * Esta clase actúa como la <b>Capa de Vista Principal</b> en la arquitectura MVC de consola.
 * Su responsabilidad es orquestar el ciclo de vida de la aplicación, que incluye:
 * </p>
 * <ol>
 * <li><b>Bootstrapping:</b> Carga inicial de datos desde disco (Deserialización).</li>
 * <li><b>Autenticación:</b> Gestión del Login y validación de credenciales.</li>
 * <li><b>Enrutamiento:</b> Dirección del flujo de usuario hacia los controladores específicos
 * (Admin, Cliente, Vendedor) basándose en el polimorfismo de la clase {@link Usuario}.</li>
 * <li><b>Registro:</b> Captura y validación de datos para nuevos clientes.</li>
 * </ol>
 * <b>Estado Global:</b>
 * Mantiene las listas maestras estáticas de {@code usuarios}, {@code peliculas} y {@code funciones},
 * actuando como un Singleton implícito de la base de datos en memoria.
 *
 * @author Equipo Cine POO
 * @version 4.0
 * @see mx.unam.fi.cine.controlador.ControladorAdministrador
 * @see mx.unam.fi.cine.controlador.ControladorCompra
 * @see mx.unam.fi.cine.modelo.GestorArchivos
 */
public class CineByt {

    // ==========================================
    // BASE DE DATOS EN MEMORIA (Estado Global)
    // ==========================================
    
    /** Lista maestra de todos los usuarios registrados (Clientes y Empleados). */
    private static List<Usuario> usuarios;
    
    /** Lista maestra del catálogo de películas. */
    private static List<Pelicula> peliculas;
    
    /** Lista maestra de la cartelera (Funciones programadas). */
    private static List<Funcion> funciones;
    
    /** Scanner global para la lectura de entrada estándar. */
    private static Scanner entrada = new Scanner(System.in);

    /**
     * Método principal de ejecución.
     * <p>
     * Define el bucle principal del programa. Se encarga de:
     * 1. Inicializar el entorno (cargar archivos).
     * 2. Asegurar que exista al menos un administrador (evita bloqueos del sistema).
     * 3. Mostrar el menú de bienvenida y gestionar el cierre de la aplicación.
     * </p>
     *
     * @param args Argumentos de línea de comando (no utilizados en esta versión).
     */
    public static void main(String[] args) {
        System.out.println("Iniciando App de Cinebyt...");
        
        // 1. Carga de Persistencia
        cargarDatos(); 
        
        // 2. Validación de Seguridad (Bootstrap)
        garantizarAdminPorDefecto(); 

        // 3. Bucle Principal de Interacción (Main Loop)
        boolean salir = false;
        while (!salir) {
            System.out.println("\n=======================================");
            System.out.println("      BIENVENIDO A CINEBYT      ");
            System.out.println("=======================================");
            System.out.println("1. Ingreso al sistema (Login)");
            System.out.println("2. Nuevo registro de cliente");
            System.out.println("3. Salir");
            System.out.print("Seleccione una opción: ");

            String opcion = entrada.nextLine();

            switch (opcion) {
                case "1":
                    login();
                    break;
                case "2":
                    registrarCliente();
                    break;
                case "3":
                    salir = true;
                    System.out.println("Cerrando app... ¡Hasta luego!");
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        }
    }

    // ==========================================
    // GESTIÓN DE DATOS Y LOGIN
    // ==========================================
    
    /**
     * Recupera el estado de la aplicación desde los archivos binarios (.dat).
     * <p>
     * Utiliza {@link GestorArchivos} para deserializar las listas maestras.
     * Implementa un mecanismo de tolerancia a fallos: si los archivos no existen
     * o están corruptos (primera ejecución), inicializa listas vacías para evitar {@code NullPointerException}.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private static void cargarDatos() {
        try {
            usuarios = (List<Usuario>) GestorArchivos.leerObjeto("usuarios.dat");
            peliculas = (List<Pelicula>) GestorArchivos.leerObjeto("peliculas.dat");
            funciones = (List<Funcion>) GestorArchivos.leerObjeto("funciones.dat");
        } catch (Exception e) {
            // Fallback: Inicialización limpia si no hay datos previos
            if (usuarios == null) usuarios = new ArrayList<>();
            if (peliculas == null) peliculas = new ArrayList<>();
            if (funciones == null) funciones = new ArrayList<>();
        }
    }

    /**
     * Rutina de seguridad para garantizar el acceso al sistema (Seeding).
     * <p>
     * Verifica si existe el usuario "Administrador". Si no existe (ej. sistema formateado),
     * crea uno por defecto. Esto es crítico para poder configurar el cine desde cero.
     * <br><b>Credenciales Default:</b> User: Administrador | Pass: 314dm1n
     * </p>
     */
    private static void garantizarAdminPorDefecto() {
        boolean existe = false;
        for (Usuario usuario : usuarios) {
            if (usuario.getNickname().equals("Administrador")) {
                existe = true;
                break;
            }
        }
        if (!existe) {
            Administrador defaultAdmin = new Administrador(
                "elAdmin", " ", " ", 100, 
                "Administrador", "314dm1n", "admin@cinebyt.mx", "5555555555", 
                Empleado.Turno.MATUTINO, false
            );
            usuarios.add(defaultAdmin);
            guardarUsuarios(); // Persistencia inmediata
        }
    }

    /**
     * Gestiona el proceso de autenticación y autorización.
     * <p>
     * Implementa un bucle de validación de credenciales. Una vez autenticado, utiliza
     * <b>Polimorfismo (instanceof)</b> para determinar el rol del usuario y dirigirlo
     * a su menú específico (Administrador, Cliente o Vendedor).
     * </p>
     */
    private static void login() {
            Usuario usuarioLogueado = null;
            boolean credencialesCorrectas = false;
            
            do {
                System.out.println("\n--- INGRESO AL SISTEMA ---");
                System.out.println("(Escriba 'SALIR' en nickname para cancelar)");
                System.out.print("Nickname: ");
                String nick = entrada.nextLine();
                
                // Opción de escape para usabilidad
                if (nick.equalsIgnoreCase("SALIR")) return; 

                System.out.print("Contraseña: ");
                String contra = entrada.nextLine();

                // Búsqueda lineal y validación
                for (Usuario usuario : usuarios) {
                    if (usuario.getNickname().equals(nick) && usuario.getPassword().equals(contra)) {
                        usuarioLogueado = usuario;
                        credencialesCorrectas = true;
                        break;
                    }
                }

                if (!credencialesCorrectas) {
                    System.out.println("AVISO: Los datos no son correctos."); 
                }

            } while (!credencialesCorrectas);

            System.out.println("\n¡Bienvenido " + usuarioLogueado.getNombre() + "!");
            
            // Enrutamiento basado en Tipo (Polimorfismo)
            if (usuarioLogueado instanceof Administrador) {
                menuAdministrador();
            } else if (usuarioLogueado instanceof Cliente) {
                menuCliente((Cliente) usuarioLogueado);
            } else if (usuarioLogueado instanceof VendedorDulceria) {
                menuVendedor((VendedorDulceria) usuarioLogueado);
            }
    }

    /**
     * Flujo de captura de datos para nuevos clientes.
     * <p>
     * Características del proceso:
     * <ul>
     * <li><b>Validación de Entrada:</b> Bucle {@code do-while} que impide avanzar con datos erróneos.</li>
     * <li><b>Integridad:</b> Verifica que el nickname no esté duplicado en el sistema.</li>
     * <li><b>Confirmación de Contraseña:</b> Obliga a escribir la contraseña dos veces para evitar errores tipográficos.</li>
     * <li><b>UX:</b> Implementa pausas visuales ({@link #esperar}) para mejorar la experiencia.</li>
     * </ul>
     */
    private static void registrarCliente() {
        System.out.println("\n--- REGISTRO DE NUEVO CLIENTE ---");

        String nick, contra, confirmContra, nombre, apPaterno, apMaterno, email, cel, tarjeta;
        int edad = 0;
        boolean datosCorrectos = false;
        
        // Bucle de validación de formulario
        do {
            System.out.print("Nombre(s): "); nombre = entrada.nextLine();
            System.out.print("Apellido Paterno: "); apPaterno = entrada.nextLine();
            System.out.print("Apellido Materno: "); apMaterno = entrada.nextLine();
            
            try {
                System.out.print("Edad: ");
                edad = Integer.parseInt(entrada.nextLine());
            } catch (NumberFormatException e) {
                edad = 18; // Valor por defecto seguro ante error de entrada
            }

            System.out.print("Nickname: "); nick = entrada.nextLine();
            System.out.print("Contraseña: "); contra = entrada.nextLine();
            System.out.print("Confirmar Contraseña: "); confirmContra = entrada.nextLine();
            System.out.print("Correo electrónico: "); email = entrada.nextLine();
            System.out.print("Número de celular: "); cel = entrada.nextLine();
            System.out.print("Número de tarjeta bancaria (16 dígitos): "); tarjeta = entrada.nextLine();

            // Resumen de verificación
            System.out.println("\n--- VERIFIQUE SUS DATOS ---");
            System.out.println("Nombre: " + nombre + " " + apPaterno + " " + apMaterno);
            System.out.println("Edad: " + edad);
            System.out.println("Nickname: " + nick);
            System.out.println("Email: " + email);
            System.out.println("Celular: " + cel);
            System.out.println("Tarjeta: " + tarjeta);
            
            // Validación lógica
            if (!contra.equals(confirmContra)) {
                System.out.println("ERROR: Las contraseñas no coinciden. Intente de nuevo.");
                datosCorrectos = false;
                continue; 
            }

            System.out.print("¿Los datos son correctos? (S/N): ");
            String resp = entrada.nextLine();
            
            if (resp.equalsIgnoreCase("S")) {
                datosCorrectos = true;
            } else {
                System.out.println("Por favor ingrese los datos nuevamente.\n");
                datosCorrectos = false;
            }

        } while (!datosCorrectos);

        // Validación de unicidad en la base de datos
        for(Usuario usuario : usuarios) {
            if(usuario.getNickname().equals(nick)) {
                System.out.println("Error: El nickname ya está registrado. No se pudo registrar.");
                esperar(5000);
                return;
            }
        }

        // Creación y persistencia
        Cliente nuevoCliente = new Cliente(nombre, apPaterno, apMaterno, edad, nick, contra, email, cel, tarjeta);
        usuarios.add(nuevoCliente);
        guardarUsuarios();
        
        System.out.println("REGISTRO EXITOSO.");
        System.out.println("Redirigiendo a pantalla inicial en 5 segundos...");
        esperar(5000);
    }

    /**
     * Persiste la lista actualizada de usuarios en disco.
     */
    private static void guardarUsuarios() {
        try {
            GestorArchivos.guardarObjeto("usuarios.dat", usuarios);
        } catch (IOException e) {
            System.out.println("Error al guardar usuarios: " + e.getMessage());
        }
    }

    /**
     * Utilidad para pausar la ejecución del hilo principal.
     * Mejora la legibilidad de los mensajes en consola.
     * @param milisg Tiempo en milisegundos.
     */
    private static void esperar(int milisg) {
        try { 
            Thread.sleep(milisg); 
        } catch (InterruptedException e) {}
    }

    // ==========================================
    // MENÚS ESPECÍFICOS (Controladores)
    // ==========================================

    /**
     * Menú exclusivo para Administradores.
     * <p>
     * Instancia el {@link ControladorAdministrador} inyectando las listas maestras
     * para permitir la gestión del catálogo.
     * </p>
     */
    private static void menuAdministrador() {
        ControladorAdministrador controlAdmin = new ControladorAdministrador(peliculas, funciones, usuarios);
        boolean regresar = false;
        while (!regresar) {
            System.out.println("\n--- MENÚ ADMINISTRADOR ---");
            System.out.println("1. Dar de alta Película");
            System.out.println("2. Dar de alta Función");
            System.out.println("3. Registrar nuevo Empleado");
            System.out.println("4. Ver historial de cliente");
            System.out.println("5. Cerrar Sesión");
            System.out.print("Seleccione una opción: ");
            
            String opc = entrada.nextLine();
            switch (opc) {
                case "1": controlAdmin.darAltaPelicula(); break;
                case "2": controlAdmin.darAltaFuncion(); break;
                case "3": controlAdmin.registrarEmpleado(); break;
                case "4": controlAdmin.verHistorialCliente(); break;
                case "5": regresar = true; break;
                default: System.out.println("Opción inválida.");
            }
        }
    }

    /**
     * Menú Principal del Cliente.
     * <p>
     * Actúa como Hub central para las operaciones de compra. Instancia los controladores
     * de {@link ControladorCompra} y {@link ControladorDulceria} según sea necesario.
     *
     * @param cliente El objeto Cliente autenticado.
     */
    private static void menuCliente(Cliente cliente) {
        ControladorCompra controlCompra = new ControladorCompra(funciones);
        ControladorDulceria controlDulceria = new ControladorDulceria(usuarios);
        
        boolean regresar = false;
        while (!regresar) {
            System.out.println("\n--- MENÚ CLIENTE ---");
            System.out.println("1. Mostrar lista de películas (Cartelera)");
            System.out.println("2. Comprar en Dulcería");
            System.out.println("3. Revisar Notificaciones");
            System.out.println("4. Cerrar Sesión");
            System.out.print("Seleccione una opción: ");

            String opc = entrada.nextLine();
            switch (opc) {
                case "1":
                    mostrarCartelera(cliente, controlCompra);
                    break;
                case "2":
                    controlDulceria.iniciarDulceria(cliente);
                    break;
                case "3":
                    menuNotificaciones(cliente);
                    break;
                case "4":
                    regresar = true;
                    break;
                default: System.out.println("Opción inválida.");
            }
        }
    }

    /**
     * Interfaz de texto para la navegación del catálogo de películas.
     * <p>
     * Permite visualizar detalles y transicionar hacia el flujo de compra
     * gestionado por {@link ControladorCompra}.
     *
     * @param cliente Cliente actual.
     * @param controlCompra Controlador encargado de la transacción.
     */
    private static void mostrarCartelera(Cliente cliente, ControladorCompra controlCompra) {
        boolean enCartelera = true;
        while (enCartelera) {
            System.out.println("\n---- CARTELERA DE PELÍCULAS ----");
            if (peliculas.isEmpty()) {
                System.out.println("No hay películas registradas en el sistema.");
                return;
            }

            // Listado resumido
            for (int i = 0; i < peliculas.size(); i++) {
                Pelicula pelicula = peliculas.get(i);
                System.out.println((i + 1) + ". " + pelicula.getTitulo() + " (" + pelicula.getGenero() + ")");
            }
            System.out.println("\nEscriba el número de la película para ver detalles.");
            System.out.println("O escriba 'Regresar' para volver al menú principal.");
            System.out.print("Elección: ");
            
            String eleccion = entrada.nextLine();
            if (eleccion.equalsIgnoreCase("Regresar")) {
                enCartelera = false;
                continue;
            }

            try {
                int indice = Integer.parseInt(eleccion) - 1;
                if (indice >= 0 && indice < peliculas.size()) {
                    Pelicula peliElegida = peliculas.get(indice);
                    
                    // Detalle completo
                    System.out.println("\n--- DETALLES DE LA PELÍCULA ---");
                    System.out.println(peliElegida.toString());
                    System.out.println("Sinopsis: " + peliElegida.getSinopsis());
                    
                    // Hook de Compra
                    System.out.println("\n¿Desea comprar boletos para esta película? (S/N)");
                    System.out.print("Elección: ");
                    String confirmar = entrada.nextLine();
                    
                    if (confirmar.equalsIgnoreCase("S")) {
                        // Delegación al Controlador de Compra
                        controlCompra.iniciarCompra(cliente, peliElegida);
                        enCartelera = false; // Regreso al menú principal post-compra
                    }
                } else {
                    System.out.println("Número de película inválido.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Por favor escriba un número o 'Regresar'.");
            }
        }
    }

    /**
     * Submenú de Notificaciones.
     * <p>
     * Permite al cliente leer los archivos de texto generados por el sistema (tickets y avisos de cocina).
     */
    private static void menuNotificaciones(Cliente cliente) {
        System.out.println("\n--- NOTIFICACIONES ---");
        System.out.println("A. Revisar órdenes de compra (Boletos)");
        System.out.println("B. Revisar notificaciones de dulcería");
        System.out.print("Seleccione una opción: ");
        String opc = entrada.nextLine().toUpperCase();
        
        if (opc.equals("A")) {
             System.out.println("\n--- TUS BOLETOS ---");
             String archivo = "tickets_" + cliente.getNickname() + ".txt";
             leerArchivoOpcional(archivo);
        } else if (opc.equals("B")) {
             verNotificacionesDulceria(cliente);
        } else {
            System.out.println("Opción inválida.");
        }
    }

    /**
     * Muestra el estado de la orden de dulcería leyendo el archivo de notificaciones específico.
     */
    private static void verNotificacionesDulceria(Cliente cliente) {
        System.out.println("\n--- ESTADO DE ÓRDENES DE DULCERÍA ---");
        String archivo = "notificaciones_" + cliente.getNickname() + ".txt";
        leerArchivoOpcional(archivo);
    }
    
    /**
     * Método auxiliar para leer e imprimir contenido de archivos de texto de forma segura.
     * Evita excepciones si el archivo aún no existe (ej. usuario nuevo).
     */
    private static void leerArchivoOpcional(String nombreArchivo) {
        if (!GestorArchivos.existeArchivo(nombreArchivo)) {
            System.out.println("No se encontró información (Archivo vacío o inexistente).");
            return;
        }
        try {
            List<String> lineas = GestorArchivos.leerArchivoTexto(nombreArchivo);
            for (String linea : lineas) {
                System.out.println(linea); 
            }
        } catch (IOException e) {
            System.out.println("Error al leer el archivo: " + e.getMessage());
        }
    }

    /**
     * Menú simplificado para Vendedores.
     * Permite visualizar su historial de productividad (Logs de preparación).
     */
    private static void menuVendedor(VendedorDulceria vendedor){
    boolean regresar = false;
        while (!regresar) {
            System.out.println("\n--- MENÚ VENDEDOR (" + vendedor.getNickname() + ") ---");
            System.out.println("1. Ver historial de pedidos atendidos");
            System.out.println("2. Cerrar Sesión");
            System.out.print("Seleccione una opción: ");
            
            String opc = entrada.nextLine();
            if (opc.equals("1")) {
                // El archivo se generó dinámicamente en PreparacionDulceria
                String nombreArchivo = "historial_" + vendedor.getNickname() + ".txt";
                System.out.println("\n--- HISTORIAL DE PEDIDOS ---");
                leerArchivoOpcional(nombreArchivo);
                
            } else if (opc.equals("2")) {
                regresar = true;
            } else {
                System.out.println("Opción inválida.");
            }
        }
    }
}
