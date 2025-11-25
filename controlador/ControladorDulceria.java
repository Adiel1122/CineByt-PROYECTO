package mx.unam.fi.cine.controlador;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import mx.unam.fi.cine.modelo.*;

/**
 * Controlador especializado en la gestión del punto de venta de alimentos y bebidas.
 * <p>
 * Esta clase administra el ciclo de vida completo de la venta de dulcería, desde la configuración
 * dinámica de precios hasta la entrega asíncrona de pedidos.
 * </p>
 * <b>Características Arquitectónicas:</b>
 * <ul>
 * <li><b>Configuración Externa:</b> Los precios no están "hardcodeados"; se cargan al inicio desde {@code PreciosProductos.txt},
 * permitiendo cambios operativos sin recompilación.</li>
 * <li><b>Procesamiento Asíncrono:</b> Utiliza hilos independientes para simular el cobro bancario y para
 * delegar la preparación del pedido a un hilo de "Cocina" ({@link PreparacionDulceria}), liberando el flujo principal.</li>
 * <li><b>Asignación de Recursos:</b> Busca y asigna automáticamente un {@link VendedorDulceria} disponible para registrar la venta.</li>
 * </ul>
 *
 * @author Equipo CineByt
 * @version 4.0
 * @see mx.unam.fi.cine.modelo.PreparacionDulceria
 * @see mx.unam.fi.cine.modelo.VendedorDulceria
 */
public class ControladorDulceria {
    private Scanner entrada;
    private List<Usuario> usuarios;
    
    /** * Estructura de datos en memoria para acceso rápido (O(1)) a los precios.
     * La clave es el identificador del producto (ej. "PALOMITAS_JUMBO") y el valor su costo.
     */
    private Map<String, Double> precios; 

    /**
     * Constructor del Controlador de Dulcería.
     * <p>
     * Inicializa los recursos y dispara inmediatamente la carga de la configuración de precios.
     *
     * @param usuarios Lista maestra de usuarios, necesaria para buscar un vendedor que atienda el pedido.
     */
    public ControladorDulceria(List<Usuario> usuarios) {
        this.entrada = new Scanner(System.in);
        this.usuarios = usuarios;
        this.precios = new HashMap<>();
        cargarPrecios(); // Inicialización de datos maestros
    }

    /**
     * Lee el archivo de configuración externo para poblar el mapa de precios.
     * <p>
     * <b>Formato esperado del archivo:</b> {@code CLAVE_PRODUCTO : PRECIO}
     * <br>Ejemplo: {@code PALOMITAS_MEGA : 85.50}
     * </p>
     * Si el archivo no existe o tiene errores de formato, el sistema maneja la excepción
     * para no interrumpir la ejecución, aunque los precios podrían ser 0.0.
     */
    private void cargarPrecios() {
        try {
            if (GestorArchivos.existeArchivo("PreciosProductos.txt")) {
                List<String> lineas = GestorArchivos.leerArchivoTexto("PreciosProductos.txt");
                for (String linea : lineas) {
                    try {
                        String[] partes = linea.split(":");
                        if (partes.length == 2) {
                            String clave = partes[0].trim().toUpperCase();
                            Double precio = Double.parseDouble(partes[1].trim());
                            precios.put(clave, precio);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Advertencia: Formato inválido en configuración de precios: " + linea);
                    }
                }
            } else {
                System.out.println("AVISO: No se encontró 'PreciosProductos.txt'. Se usarán precios base $0.0");
            }
        } catch (IOException e) {
            System.out.println("Error crítico al cargar precios: " + e.getMessage());
        }
    }

    /**
     * Ejecuta el menú principal de interacción con el cliente en la dulcería.
     * <p>
     * Ofrece un menú híbrido con:
     * <ul>
     * <li><b>Combos (A-D):</b> Paquetes predefinidos con lógica de negocio simplificada.</li>
     * <li><b>Personalizado (E):</b> Flujo dinámico que construye una orden ítem por ítem consultando precios reales.</li>
     * </ul>
     * Tras la selección, desencadena la simulación de pago y la preparación.
     *
     * @param cliente El cliente autenticado que realiza la compra.
     */
    public void iniciarDulceria(Cliente cliente) {
        System.out.println("\n--- BIENVENIDO A LA DULCERÍA ---");
        System.out.println("A. Combo 'amix': Palomitas y dos refrescos tamaño jumbo");
        System.out.println("B. Combo 'nachos': Palomitas, dos refrescos y nachos tamaño jumbo");
        System.out.println("C. Combo 'buen trio': Palomitas, tres refrescos y nachos tamaño mega");
        System.out.println("D. Combo 'qué me ves': Palomitas, refresco y nachos jumbo");
        System.out.println("E. Orden personalizada");
        System.out.println("0. Salir");
        System.out.print("Elige una opción: ");
        
        String opcion = entrada.nextLine().toUpperCase();
        if (opcion.equals("0")) return;

        double totalPagar = 0.0;
        String detallesOrden = "";

        // Lógica de Selección y Cálculo de Costos
        if (opcion.equals("E")) {
            // Flujo de Orden Personalizada (Iterativo)
            List<Producto> listaPersonalizada = armarOrdenPersonalizada();
            if (listaPersonalizada.isEmpty()) {
                System.out.println("Orden vacía. Regresando al menú.");
                return;
            }
            
            for (Producto producto : listaPersonalizada) {
                totalPagar += producto.getPrecio();
            }
            detallesOrden = "Orden Personalizada (" + listaPersonalizada.size() + " items)";
            
        } else if (opcion.matches("[ABCD]")) {
            // Flujo de Combos (Predefinido)
            pedirSabores(opcion); // Captura de preferencias del usuario
            
            switch (opcion) {
                case "A": totalPagar = 180.00; detallesOrden = "Combo Amix"; break;
                case "B": totalPagar = 200.00; detallesOrden = "Combo Nachos"; break;
                case "C": totalPagar = 230.00; detallesOrden = "Combo Buen Trio"; break;
                case "D": totalPagar = 150.00; detallesOrden = "Combo Qué me ves"; break;
            }
        } else {
            System.out.println("Opción inválida.");
            return;
        }

        // Simulación de Transacción Financiera
        boolean pagoExitoso = simularPagoDulceria();

        if (pagoExitoso) {
            // Transición a la fase de Logística (Preparación)
            generarClaveYLanzarPreparacion(cliente, totalPagar, detallesOrden);
        } else {
            System.out.println("El pago no pudo ser procesado. Intente nuevamente.");
        }
    }

    /**
     * Orquesta la simulación visual y temporal del proceso de cobro.
     * <p>
     * Similar al {@code ControladorCompra}, utiliza dos hilos:
     * 1. <b>Backend:</b> Simula latencia de red bancaria.
     * 2. <b>Frontend:</b> Muestra feedback visual (spinner) al usuario.
     * </p>
     * @return {@code true} si la transacción concluye exitosamente.
     */
    private boolean simularPagoDulceria() {
        System.out.println("\nProcesando pago de dulcería...");
        
        // Hilo de Lógica Bancaria
        Thread hiloBanco = new Thread(() -> {
            try {
                System.out.println("\n>> Estableciendo conexión con el banco...");
                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 5001)); 

                System.out.println("\n>> Haciendo el cargo correspondiente...");
                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 5001)); 

                System.out.println("\n>> Transacción finalizada.");
            } catch (InterruptedException e) {
                System.out.println("Error en conexión bancaria.");
            }
        });

        // Hilo de Feedback Visual
        Thread hiloBarra = new Thread(() -> {
            char[] chars = {'|', '/', '-', '\\'};
            int i = 0;
            try {
                while (hiloBanco.isAlive()) {
                    System.out.print("\rValidando " + chars[i++ % 4]);
                    Thread.sleep(500); 
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        hiloBanco.start();
        hiloBarra.start();

        try {
            hiloBanco.join(); 
            hiloBarra.join(); 
            Thread.sleep(1000); 
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * Finaliza la venta y delega la tarea de preparación a un hilo en segundo plano.
     * <p>
     * Acciones clave:
     * <ol>
     * <li>Genera un ID único para la orden.</li>
     * <li>Asigna la orden a un {@link VendedorDulceria} (busca uno real o crea uno "Bot").</li>
     * <li>Instancia y arranca un hilo {@link PreparacionDulceria} que escribirá los archivos de notificación
     * sin bloquear la consola del usuario.</li>
     * </ol>
     *
     * @param cliente       Dueño de la orden.
     * @param total         Monto pagado.
     * @param detalleOrden  Descripción breve de lo comprado.
     */
    private void generarClaveYLanzarPreparacion(Cliente cliente, double total, String detalleOrden) {
        // Generación de Clave Única
        StringBuilder iniciales = new StringBuilder();
        if (cliente.getNombre().length() > 0) iniciales.append(cliente.getNombre().charAt(0));
        if (cliente.getApPaterno().length() > 0) iniciales.append(cliente.getApPaterno().charAt(0));
        
        LocalDateTime ahora = LocalDateTime.now();
        String fechaHora = ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd:HHmm"));
        String claveOrden = iniciales.toString().toUpperCase() + ":" + fechaHora;

        // Feedback inmediato al usuario
        System.out.println("\n--- RESUMEN DE PEDIDO DULCERÍA ---");
        System.out.println("Detalle: " + detalleOrden);
        System.out.println("Total Pagado: $" + total);
        System.out.println("Clave de orden: " + claveOrden);
        System.out.println("Revisa la sección de notificaciones para saber cuando tu orden esté lista.");
        System.out.println("Presione Enter para regresar al menú principal...");
        entrada.nextLine();

        // Lógica de Asignación de Personal (Resource Allocation)
        VendedorDulceria vendedorAsignado = null;
        if (usuarios != null) {
            for (Usuario u : usuarios) {
                if (u instanceof VendedorDulceria) {
                    vendedorAsignado = (VendedorDulceria) u;
                    break; // Estrategia simple: Asignar al primero disponible
                }
            }
        }

        // Fallback: Si no hay personal, el sistema asume el rol (Bot)
        if (vendedorAsignado == null) {
            vendedorAsignado = new VendedorDulceria("Sistema", "Auto", "Bot", 0, "CocinaExpress", "pass", "x", "x", Empleado.Turno.MATUTINO, "Domingo");
        }

        // Ejecución Asíncrona: Hilo de Preparación
        PreparacionDulceria preparacion = new PreparacionDulceria(cliente, claveOrden, detalleOrden, vendedorAsignado, ahora);
        Thread hiloPrep = new Thread(preparacion);
        hiloPrep.start();
    }

    // ==========================================
    // LÓGICA DE ORDEN PERSONALIZADA
    // ==========================================

    /**
     * Construye una lista de productos seleccionados interactivamente por el usuario.
     * <p>
     * Utiliza el mapa {@code precios} para validar existencia y obtener el costo
     * de cada ítem según su tamaño (Clave compuesta: PRODUCTO_TAMAÑO).
     *
     * @return Lista de objetos {@link Producto} validados.
     */
    private List<Producto> armarOrdenPersonalizada() {
        List<Producto> productos = new ArrayList<>();
        boolean continuar = true;
        
        while (continuar) {
            System.out.println("\n--- AGREGAR PRODUCTO ---");
            System.out.println("1. Palomitas");
            System.out.println("2. Refresco");
            System.out.println("3. Nachos");
            System.out.println("4. Terminar orden");
            System.out.print("Seleccione una opción: ");
            String eleccion = entrada.nextLine();

            String clavePrecio = "";
            double precioEncontrado = 0.0;

            switch (eleccion) {
                case "1":
                    System.out.println("Tamaños: Medianas, Grandes, Jumbo, Mega");
                    System.out.print("Escriba tamaño: ");
                    String tamPalom = entrada.nextLine().toUpperCase();
                    
                    clavePrecio = "PALOMITAS_" + tamPalom;
                    precioEncontrado = precios.getOrDefault(clavePrecio, 0.0);
                    
                    if (precioEncontrado > 0) {
                        System.out.print("Sabor (Mantequilla/Queso/Jalapeño): ");
                        String sabPalom = entrada.nextLine();
                        productos.add(new Producto("Palomitas " + tamPalom + " " + sabPalom, precioEncontrado));
                    } else {
                        System.out.println("Tamaño no válido o precio no encontrado.");
                    }
                    break;
                    
                case "2":
                    System.out.println("Tamaños: Mediano, Grande, Jumbo, Mega");
                    System.out.print("Escriba tamaño: ");
                    String tamRefre = entrada.nextLine().toUpperCase();
                    
                    clavePrecio = "REFRESCO_" + tamRefre;
                    precioEncontrado = precios.getOrDefault(clavePrecio, 0.0);

                    if (precioEncontrado > 0) {
                        System.out.print("Sabor (Cola/Naranja/Manzana): ");
                        String sabRefre = entrada.nextLine();
                        productos.add(new Producto("Refresco " + tamRefre + " " + sabRefre, precioEncontrado));
                    } else {
                        System.out.println("Tamaño no válido.");
                    }
                    break;
                    
                case "3":
                    System.out.println("Tamaños: Personal, Jumbo, Mega");
                    System.out.print("Escriba tamaño: ");
                    String tamNacho = entrada.nextLine().toUpperCase();
                    
                    clavePrecio = "NACHOS_" + tamNacho;
                    precioEncontrado = precios.getOrDefault(clavePrecio, 0.0);

                    if (precioEncontrado > 0) {
                        productos.add(new Producto("Nachos " + tamNacho, precioEncontrado));
                    } else {
                        System.out.println("Tamaño no válido.");
                    }
                    break;
                    
                case "4":
                    continuar = false;
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        }
        return productos;
    }

    /**
     * Método auxiliar para capturar preferencias de sabor en combos predefinidos.
     * Solo tiene efecto visual/informativo en este prototipo.
     */
    private void pedirSabores(String comboOpcion) {
        System.out.println("\n--- PERSONALIZAR COMBO ---");
        System.out.println("Elija sabor de Palomitas (Mantequilla, Queso, Jalapeño):");
        String palom = entrada.nextLine();
        
        System.out.println("Elija sabor de Refrescos (Cola, Cola-light, Naranja, Manzana, Toronja):");
        String refre = entrada.nextLine();
        
        System.out.println("Sabores registrados: " + palom + " / " + refre);
    }
}
