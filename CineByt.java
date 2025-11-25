package mx.unam.fi.cine;

import mx.unam.fi.cine.modelo.*;
import mx.unam.fi.cine.controlador.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Clase principal de la aplicación (Main).
 * Orquesta el inicio de sesión, la carga de datos y la navegación por los menús.
 */
public class CineByt {
    // Listas maestras de datos (Memoria RAM)
    private static List<Usuario> usuarios;
    private static List<Pelicula> peliculas;
    private static List<Funcion> funciones;
    
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Iniciando Sistema de Cine...");
        cargarDatos(); // Carga archivos .dat o inicializa listas vacías
        garantizarAdminPorDefecto(); // [cite: 16-17]

        boolean salir = false;
        while (!salir) {
            System.out.println("\n=======================================");
            System.out.println("      BIENVENIDO AL CINE POO 2026      ");
            System.out.println("=======================================");
            System.out.println("1. Ingreso al sistema (Login)"); //[cite: 35]
            System.out.println("2. Nuevo registro de cliente"); //[cite: 19]
            System.out.println("3. Salir");
            System.out.print("Opcion: ");

            String opcion = scanner.nextLine();

            switch (opcion) {
                case "1":
                    login();
                    break;
                case "2":
                    registrarCliente();
                    break;
                case "3":
                    salir = true;
                    System.out.println("Cerrando sistema... ¡Hasta luego!");
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
     * Carga los objetos serializados desde los archivos .dat.
     * Si no existen, inicializa las listas vacías.
     */
    @SuppressWarnings("unchecked")
    private static void cargarDatos() {
        try {
            usuarios = (List<Usuario>) GestorArchivos.leerObjeto("usuarios.dat");
            peliculas = (List<Pelicula>) GestorArchivos.leerObjeto("peliculas.dat");
            funciones = (List<Funcion>) GestorArchivos.leerObjeto("funciones.dat");
        } catch (Exception e) {
            // Si falla (ej. primera vez), inicializamos listas vacías
            if (usuarios == null) usuarios = new ArrayList<>();
            if (peliculas == null) peliculas = new ArrayList<>();
            if (funciones == null) funciones = new ArrayList<>();
        }
    }

    /**
     * Asegura que exista el administrador por defecto requerido.
     * [cite_start]Nickname: elAdministrador, Pass: 314dm1n [cite: 16-17]
     */
    private static void garantizarAdminPorDefecto() {
        boolean existe = false;
        for (Usuario u : usuarios) {
            if (u.getNickname().equals("elAdministrador")) {
                existe = true;
                break;
            }
        }
        if (!existe) {
            // Creamos el admin por defecto
            Administrador defaultAdmin = new Administrador(
                "Admin", " ", " ", 100, 
                "elAdministrador", "314dm1n", "admin@cine.unam.mx", "5555555555", 
                Empleado.Turno.MATUTINO, false
            );
            usuarios.add(defaultAdmin);
            guardarUsuarios(); // Guardamos inmediatamente para persistir
        }
    }

    /**
     * Lógica de inicio de sesión y validación de credenciales.
     * Redirige al menú correspondiente usando instanceof.
     */
    private static void login() {
        System.out.print("Nickname: ");
        String nick = scanner.nextLine();
        System.out.print("Contraseña: ");
        String pass = scanner.nextLine();

        Usuario usuarioLogueado = null;

        // Validación [cite: 36]
        for (Usuario u : usuarios) {
            if (u.getNickname().equals(nick) && u.getPassword().equals(pass)) {
                usuarioLogueado = u;
                break;
            }
        }

        if (usuarioLogueado != null) {
            System.out.println("¡Bienvenido " + usuarioLogueado.getNombre() + "!");
            
            // Ruteo Polimórfico de Menús
            if (usuarioLogueado instanceof Administrador) {
                menuAdministrador(); // [cite: 40]
            } else if (usuarioLogueado instanceof Cliente) {
                menuCliente((Cliente) usuarioLogueado); // [cite: 91]
            } else if (usuarioLogueado instanceof VendedorDulceria) {
                menuVendedor((VendedorDulceria) usuarioLogueado); // [cite: 173]
            }
        } else {
            System.out.println("Error: Credenciales incorrectas."); // [cite: 37]
        }
    }

    private static void registrarCliente() {
        // Lógica simplificada de registro (puedes expandirla con las validaciones de bucle del PDF)
        System.out.println("--- REGISTRO DE CLIENTE ---");
        // ... (Pedir datos nombre, pass, tarjeta, etc.)
        // Para brevedad del ejemplo Main, instanciamos uno directo:
        System.out.println("(Simulando formulario de registro...)");
        
        System.out.print("Nickname deseado: ");
        String nick = scanner.nextLine();
        
        // Validación simple de duplicados [cite: 32]
        for(Usuario u : usuarios) {
            if(u.getNickname().equals(nick)) {
                System.out.println("El usuario ya existe.");
                return;
            }
        }

        System.out.print("Contraseña: "); String pass = scanner.nextLine();
        System.out.print("Nombre: "); String nombre = scanner.nextLine();
        System.out.print("Tarjeta (16 digitos): "); String tarjeta = scanner.nextLine();

        Cliente nuevo = new Cliente(nombre, "Ap", "Ap", 20, nick, pass, "correo", "555", tarjeta);
        usuarios.add(nuevo);
        guardarUsuarios();
        System.out.println("Registro exitoso. Por favor inicie sesión"); //[cite: 33-34]
    }

    private static void guardarUsuarios() {
        try {
            GestorArchivos.guardarObjeto("usuarios.dat", usuarios);
        } catch (IOException e) {
            System.out.println("Error al guardar usuarios: " + e.getMessage());
        }
    }

    // ==========================================
    // MENÚS ESPECÍFICOS (Controladores)
    // ==========================================

    /**
     * Menú para Administradores.
     * Usa ControladorAdministrador.
     */
    private static void menuAdministrador() {
        ControladorAdministrador ctrlAdmin = new ControladorAdministrador(peliculas, funciones, usuarios);
        boolean regresar = false;
        while (!regresar) {
            System.out.println("\n--- MENÚ ADMINISTRADOR ---");
            System.out.println("1. Dar de alta Película"); //[cite: 43]
            System.out.println("2. Dar de alta Función"); //[cite: 49]
            System.out.println("3. Registrar nuevo Empleado"); //[cite: 71]
            System.out.println("4. Cerrar Sesión"); //[cite: 89]
            System.out.print("Opción: ");
            
            String op = scanner.nextLine();
            switch (op) {
                case "1": ctrlAdmin.darAltaPelicula(); break;
                case "2": ctrlAdmin.darAltaFuncion(); break;
                case "3": ctrlAdmin.registrarEmpleado(); break;
                case "4": regresar = true; break;
                default: System.out.println("Opción inválida.");
            }
        }
    }

    /**
     * Menú para Clientes.
     * Usa ControladorCompra y ControladorDulceria.
     */
    private static void menuCliente(Cliente cliente) {
        ControladorCompra ctrlCompra = new ControladorCompra(funciones);
        ControladorDulceria ctrlDulceria = new ControladorDulceria();
        
        boolean regresar = false;
        while (!regresar) {
            System.out.println("\n--- MENÚ CLIENTE ---");
            System.out.println("1. Ver cartelera / Comprar Boletos"); //[cite: 94, 100]
            System.out.println("2. Comprar en Dulcería"); //[cite: 117]
            System.out.println("3. Revisar Notificaciones (Dulcería/Tickets)"); //[cite: 146]
            System.out.println("4. Cerrar Sesión"); //[cite: 157]
            System.out.print("Opción: ");

            String op = scanner.nextLine();
            switch (op) {
                case "1":
                    // Flujo de compra [cite: 101]
                    if (funciones.isEmpty()) {
                        System.out.println("No hay funciones disponibles.");
                    } else {
                        System.out.println("Funciones disponibles:");
                        for (int i = 0; i < funciones.size(); i++) {
                            System.out.println((i + 1) + ". " + funciones.get(i).toString());
                        }
                        System.out.print("Seleccione número de función (0 cancelar): ");
                        try {
                            int idx = Integer.parseInt(scanner.nextLine()) - 1;
                            if (idx >= 0 && idx < funciones.size()) {
                                ctrlCompra.iniciarCompra(cliente, funciones.get(idx));
                            }
                        } catch (Exception e) {
                            System.out.println("Entrada inválida.");
                        }
                    }
                    break;
                case "2":
                    ctrlDulceria.iniciarDulceria(cliente);
                    break;
                case "3":
                    verNotificaciones(cliente);
                    break;
                case "4":
                    regresar = true;
                    break;
                default: System.out.println("Opción inválida.");
            }
        }
    }

    /**
     * Muestra el contenido de los archivos de texto generados para el cliente.
     */
    private static void verNotificaciones(Cliente cliente) {
        System.out.println("\n--- TUS NOTIFICACIONES ---");
        String archivo = "notificaciones_" + cliente.getNickname() + ".txt";
        try {
            if (GestorArchivos.existeArchivo(archivo)) {
                List<String> lineas = GestorArchivos.leerArchivoTexto(archivo);
                for (String l : lineas) System.out.println(l);
            } else {
                System.out.println("No tienes notificaciones nuevas.");
            }
        } catch (IOException e) {
            System.out.println("Error al leer notificaciones.");
        }
    }

    /**
     * Menú para Vendedores de Dulcería.
     * Solo visualiza historial.
     */
    private static void menuVendedor(VendedorDulceria vendedor) {
        boolean regresar = false;
        while (!regresar) {
            System.out.println("\n--- MENÚ VENDEDOR (" + vendedor.getNickname() + ") ---");
            System.out.println("1. Ver historial de pedidos atendidos"); //[cite: 174]
            System.out.println("2. Cerrar Sesión"); //[cite: 175]
            System.out.print("Opción: ");
            
            String op = scanner.nextLine();
            if (op.equals("1")) {
                System.out.println("Funcionalidad de historial (Lectura de archivo de logs del vendedor)");
                // Aquí se implementaría la lectura del archivo log del vendedor si se hubiera generado en el Hilo.
            } else if (op.equals("2")) {
                regresar = true;
            }
        }
    }
}

