package mx.unam.fi.cine.controlador;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import mx.unam.fi.cine.modelo.*;

/**
 * Controlador transaccional encargado de gestionar el flujo completo de adquisición de boletos.
 * <p>
 * Esta clase actúa como el orquestador principal de las operaciones del cliente final. Sus responsabilidades
 * abarcan desde la consulta de disponibilidad hasta la confirmación financiera y emisión de tickets.
 * </p>
 * <b>Aspectos Técnicos Destacados:</b>
 * <ul>
 * <li><b>Concurrencia:</b> Implementa simulación de procesos bancarios asíncronos mediante {@link Thread}.</li>
 * <li><b>Persistencia:</b> Actualiza el estado de los objetos {@link Funcion} (ocupación de asientos) y genera bitácoras de texto.</li>
 * <li><b>Validación:</b> Asegura la integridad de los datos de entrada (fechas, coordenadas de asientos).</li>
 * </ul>
 *
 * @author Equipo CineByt
 * @version 2.0
 * @see mx.unam.fi.cine.modelo.Cliente
 * @see mx.unam.fi.cine.modelo.Funcion
 */
public class ControladorCompra {
    
    /** Manejador de entrada de datos por consola. */
    private Scanner entrada;
    
    /** * Referencia directa a la lista maestra de funciones en memoria.
     * Cualquier modificación en los objetos de esta lista (ej. ocupar asiento) se reflejará globalmente.
     */
    private List<Funcion> funciones; 

    /**
     * Constructor del controlador de compras.
     * * @param funciones Referencia a la lista maestra de funciones (Cartelera) cargada en {@code CineByt}.
     */
    public ControladorCompra(List<Funcion> funciones) {
        this.entrada = new Scanner(System.in);
        this.funciones = funciones;
    }

    /**
     * Inicia el flujo de venta para una película específica.
     * <p>
     * Pasos del proceso:
     * <ol>
     * <li>Solicita y valida la fecha de asistencia.</li>
     * <li>Filtra la lista de funciones buscando coincidencias de Título + Fecha.</li>
     * <li>Presenta las opciones disponibles al usuario.</li>
     * <li>Delega la gestión de asientos al método {@link #realizarCompraAsientos}.</li>
     * </ol>
     * * @param cliente  El usuario autenticado que realiza la compra.
     * @param pelicula La película seleccionada previamente desde el menú principal.
     */
    public void iniciarCompra(Cliente cliente, Pelicula pelicula) {
        System.out.print("Ingrese fecha para ver funciones (dd/MM/yyyy): ");
        String fecha = entrada.nextLine();
        LocalDate fechaSeleccionada;
        try {
            fechaSeleccionada = LocalDate.parse(fecha, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            System.out.println("Formato de fecha inválido.");
            return;
        }

        // 2. Filtrado manual de funciones coincidentes
        List<Funcion> funcionesDisponibles = new ArrayList<>();
        for (Funcion funcion : funciones) {
            if (funcion.getPelicula().getTitulo().equals(pelicula.getTitulo()) && 
                funcion.getHorario().toLocalDate().equals(fechaSeleccionada)) {
                funcionesDisponibles.add(funcion);
            }
        }

        if (funcionesDisponibles.isEmpty()) {
            System.out.println("No hay funciones programadas para esa fecha.");
            return;
        }

        // 3. Despliegue de opciones
        System.out.println("\nFunciones disponibles para " + pelicula.getTitulo() + " el " + fecha + ":");
        for (int i = 0; i < funcionesDisponibles.size(); i++) {
            Funcion f = funcionesDisponibles.get(i);
            System.out.println((i + 1) + ". Hora: " + f.getHorario().toLocalTime() + " | Sala: " + f.getSala().getNombre() + " | Género: " + f.getPelicula().getGenero());
        }

        // 4. Selección del usuario
        System.out.print("Seleccione número de función (0 para cancelar): ");
        try {
            int eleccion = Integer.parseInt(entrada.nextLine());
            if (eleccion == 0) return;
            
            if (eleccion > 0 && eleccion <= funcionesDisponibles.size()) {
                Funcion funcionElegida = funcionesDisponibles.get(eleccion - 1);
                // Transición a la siguiente fase del flujo
                realizarCompraAsientos(cliente, funcionElegida);
            } else {
                System.out.println("Opción inválida.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Entrada inválida.");
        }
    }

    /**
     * Gestiona la selección de lugares, pago y finalización de la transacción.
     * <p>
     * Este método encapsula la lógica crítica de negocio:
     * <ul>
     * <li>Visualización del estado actual de la sala.</li>
     * <li>Bloqueo lógico de asientos (selección).</li>
     * <li>Simulación de pago bancario (Proceso Bloqueante Simulada).</li>
     * <li>Confirmación de compra y persistencia.</li>
     * </ul>
     * * @param cliente El comprador.
     * @param funcion La función específica seleccionada.
     */
    private void realizarCompraAsientos(Cliente cliente, Funcion funcion) {
        // Mostrar mapa gráfico de la sala
        mostrarMapaAsientos(funcion.getSala());

        // Selección y Validación de entrada
        List<Asiento> asientosSeleccionados = solicitarAsientos(funcion.getSala());
        if (asientosSeleccionados == null || asientosSeleccionados.isEmpty()) return;

        // Invocación del subsistema de pago concurrente
        boolean pagoExitoso = simularPagoConcurrente();

        if (pagoExitoso) {
            generarTickets(cliente, funcion, asientosSeleccionados);
            try { 
                // Persistencia inmediata del estado actualizado (Asientos ocupados)
                GestorArchivos.guardarObjeto("funciones.dat", funciones); 
            } catch (IOException e) {
                System.err.println("Advertencia: No se pudo actualizar el archivo maestro de funciones.");
            }
        }
    }

    // ==========================================
    // LÓGICA DE SELECCIÓN DE ASIENTOS
    // ==========================================
    
    /**
     * Renderiza en consola una representación visual de la matriz de asientos.
     * <p>
     * Recorre la lista de asientos y formatea la salida basándose en el estado de ocupación.
     * Maneja saltos de línea dinámicos para dibujar la cuadrícula correctamente según el tipo de sala.
     * </p>
     * @param sala La sala a visualizar.
     */
    private void mostrarMapaAsientos(Sala sala) {
        System.out.println("\nDistribución de la Sala ( [ ]=Libre, [X]=Ocupado ):");
        for (Asiento a : sala.getAsientos()) {
            String estado = a.isOcupado() ? "[X]" : "[" + a.getFila() + a.getNumero() + "]";
            System.out.print(estado + "\t");
            
            // Lógica de visualización: Salto de línea al final de cada fila física
            // Sala VIP tiene filas cortas (6), Sala estándar filas largas (15)
            if (a.getNumero() == 15 || (sala.getNombre().contains("VIP") && a.getNumero() == 6)) {
                System.out.println();
            }
        }
        System.out.println();
    }

    /**
     * Procesa la entrada del usuario para seleccionar múltiples asientos.
     * <p>
     * Realiza validaciones robustas:
     * <ul>
     * <li><b>Formato:</b> Valida patrón LetraNúmero (ej. A1).</li>
     * <li><b>Existencia:</b> Verifica que el asiento exista en la sala.</li>
     * <li><b>Disponibilidad:</b> Verifica que no esté ocupado previamente.</li>
     * <li><b>Unicidad:</b> Evita seleccionar el mismo asiento dos veces en la misma operación.</li>
     * </ul>
     * * @param sala La sala sobre la que se opera.
     * @return Lista de objetos {@link Asiento} validados listos para compra, o {@code null} si cancela.
     */
    private List<Asiento> solicitarAsientos(Sala sala) {
        while (true) {
            System.out.println("\nIngrese los asientos deseados separados por espacio (Ej: A1 B4 C5):");
            System.out.println("O escriba '0' para cancelar.");
            String entradaAsientos = entrada.nextLine().toUpperCase();

            if (entradaAsientos.equals("0")) return null;

            String[] tokens = entradaAsientos.split(" ");
            List<Asiento> asientosTemporales = new ArrayList<>();
            boolean errorEncontrado = false;

            for (String token : tokens) {
                try {
                    // Parsing: "A10" -> Fila 'A', Numero 10
                    char fila = token.charAt(0);
                    int numero = Integer.parseInt(token.substring(1));
                    
                    Asiento asiento = sala.buscarAsiento(fila, numero);

                    if (asiento == null) {
                        System.out.println("Error: El asiento " + token + " no existe en esta sala.");
                        errorEncontrado = true;
                        break;
                    } else if (asiento.isOcupado()) {
                        System.out.println("Error: El asiento " + token + " ya está ocupado. Elija otro.");
                        errorEncontrado = true;
                        break;
                    } else {
                        // Verificar duplicados en la entrada actual del usuario
                        if(asientosTemporales.contains(asiento)){
                            System.out.println("Error: Ha ingresado el asiento " + token + " dos veces.");
                            errorEncontrado = true;
                            break;
                        }
                        asientosTemporales.add(asiento);
                    }
                } catch (Exception e) {
                    System.out.println("Formato inválido para: " + token + ". Use formato FilaNumero (Ej: A5).");
                    errorEncontrado = true;
                    break;
                }
            }

            if (!errorEncontrado && !asientosTemporales.isEmpty()) {
                return asientosTemporales;
            }
            // Si hubo error, el bucle while repite la solicitud desde el inicio
        }
    }

    // ==========================================
    // LÓGICA DE HILOS (THREADS)
    // ==========================================
    
    /**
     * Simula una transacción bancaria utilizando programación concurrente.
     * <p>
     * Este método orquesta dos hilos paralelos para mejorar la experiencia de usuario (UX):
     * <ol>
     * <li><b>Hilo Banco (Backend):</b> Simula la latencia de red y procesamiento bancario mediante {@code Thread.sleep} y tiempos aleatorios.</li>
     * <li><b>Hilo Barra (Frontend):</b> Muestra una animación de carga en consola para indicar actividad mientras el hilo del banco trabaja.</li>
     * </ol>
     * <b>Sincronización:</b>
     * Se utiliza {@code join()} para pausar el hilo principal (Main) hasta que la transacción bancaria finalice,
     * asegurando que no se generen tickets antes de confirmar el "pago".
     * * @return {@code true} si la transacción simulada concluye correctamente.
     */
    private boolean simularPagoConcurrente() {
        System.out.println("\nIniciando transacción bancaria...");

        // Hilo 1: Simulación del Proceso Bancario (Lógica de Negocio Simulada)
        Thread hiloBanco = new Thread(() -> {
            try {
                System.out.println("\n>> Estableciendo conexión con el banco...");
                // Simulación de latencia variable (2 a 5 segundos)
                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 5001)); 

                System.out.println("\n>> Haciendo el cargo correspondiente...");
                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 5001)); 

                System.out.println("\n>> Transacción finalizada.");
            } catch (InterruptedException e) {
                System.out.println("Error en la conexión bancaria.");
            }
        });

        // Hilo 2: Feedback visual al usuario (Barra de progreso)
        Thread hiloBarra = new Thread(() -> {
            char[] barra = {'|', '/', '-', '\\'};
            int i = 0;
            try {
                // El hilo visual vive solo mientras el hilo funcional (banco) esté vivo
                while (hiloBanco.isAlive()) {
                    System.out.print("\rProcesando " + barra[i % 4]); // \r retorno de carro para sobreescribir línea
                    i++;
                    Thread.sleep(500); // Actualización cada 0.5 seg
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Inicio de ejecución concurrente
        hiloBanco.start();
        hiloBarra.start();

        try {
            // Sincronización: El hilo principal espera (bloquea) hasta que el banco termine
            hiloBanco.join(); 
            hiloBarra.join(); // Aseguramos limpieza del hilo visual
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        
        // Pequeña pausa de cortesía antes del resumen
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        return true;
    }

    // ==========================================
    // GENERACIÓN DE TICKETS
    // ==========================================
    
    /**
     * Finaliza la compra generando los comprobantes y actualizando el modelo.
     * <p>
     * Acciones realizadas:
     * <ul>
     * <li>Cambia el estado de los objetos {@link Asiento} a "Ocupado".</li>
     * <li>Genera un ID único por boleto (Composición: FunciónID + Asiento).</li>
     * <li>Calcula el total monetario.</li>
     * <li>Escribe un registro persistente en el historial del usuario (archivo de texto).</li>
     * </ul>
     * * @param cliente  Usuario que compró.
     * @param funcion  Función comprada.
     * @param asientos Lista de asientos adquiridos.
     */
    private void generarTickets(Cliente cliente, Funcion funcion, List<Asiento> asientos) {
        System.out.println("\n===============================================");
        System.out.println("             RESUMEN DE COMPRA                 ");
        System.out.println("===============================================");
        System.out.println("Película: " + funcion.getPelicula().getTitulo());
        System.out.println("Horario: " + funcion.getHorario().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        System.out.println("Sala: " + funcion.getSala().getNombre());
        System.out.println("Cliente: " + cliente.getNombre() + " " + cliente.getApPaterno());
        
        double precioBoleto = 60.00; // Precio base fijo para este prototipo
        double total = precioBoleto * asientos.size();

        // Base del ID: Iniciales:AAAAMMDD:hhmm:Sala
        String idBase = funcion.getIdFuncion(); 

        for (Asiento asiento : asientos) {
            // 1. Mutación de Estado: Marcar asiento como ocupado en memoria
            asiento.setOcupado(true);

            // 2. Generación de ID único del boleto
            String idBoleto = String.format("%s:%c%d", idBase, asiento.getFila(), asiento.getNumero());
            
            System.out.println("-----------------------------------------------");
            System.out.println("Asiento: " + asiento.getFila() + asiento.getNumero());
            System.out.println("Ticket ID: " + idBoleto);
            
            // 3. Persistencia de Historial (Log de tickets)
            try {
                GestorArchivos.escribirTexto("tickets_" + cliente.getNickname() + ".txt", 
                    "Boleto: " + idBoleto + " | " + funcion.getPelicula().getTitulo(), true);
            } catch (IOException e) {
                // Error no crítico: Si falla el log, la compra sigue siendo válida en memoria
            }
        }
        
        System.out.println("===============================================");
        System.out.println("TOTAL PAGADO: $" + total);
        System.out.println("Cargo realizado a tarjeta terminación: *" + 
                cliente.getNumeroTarjeta().substring(cliente.getNumeroTarjeta().length() - 4));
        System.out.println("===============================================\n");
    }
}
