package mx.unam.fi.cine.controlador;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import mx.unam.fi.cine.modelo.*;

/**
 * Tarea asíncrona encargada de simular el ciclo de vida de preparación de alimentos.
 * <p>
 * Esta clase implementa la interfaz {@link Runnable}, permitiendo que sea ejecutada en un hilo independiente
 * gestionado por el {@link ControladorDulceria}. Su propósito es doble:
 * </p>
 * <ol>
 * <li><b>Simulación Temporal:</b> Introduce pausas aleatorias (mediante {@code Thread.sleep}) para emular
 * las fases reales de una cocina (Asignación, Inicio, Preparación).</li>
 * <li><b>Comunicación y Auditoría:</b>
 * <ul>
 * <li>Escribe en el archivo de notificaciones del {@code Cliente} para informarle el estado.</li>
 * <li>Escribe en el archivo de historial del {@code VendedorDulceria} para métricas de productividad.</li>
 * </ul>
 * </li>
 * </ol>
 *
 * @author Equipo CineByt
 * @version 3.0
 * @see mx.unam.fi.cine.controlador.ControladorDulceria
 * @see mx.unam.fi.cine.modelo.GestorArchivos
 */
public class PreparacionDulceria implements Runnable {

    /**
     * Cliente propietario de la orden.
     * Necesario para determinar el nombre del archivo de notificación destino ({@code notificaciones_NICKNAME.txt}).
     */
    private Usuario cliente;

    /**
     * Identificador único de la orden (generado previamente en el Controlador).
     * Permite la trazabilidad en los logs.
     */
    private String idOrden;

    /**
     * Resumen del contenido del pedido (ej. "Combo Amix", "Orden Personalizada (3 items)").
     */
    private String detalleOrden; 

    /**
     * Empleado asignado a esta tarea.
     * Su importancia radica en que el log de desempeño se escribirá en su archivo personal ({@code historial_NICKNAME.txt}).
     */
    private VendedorDulceria vendedor; 

    /**
     * Marca de tiempo exacta en que se confirmó el pago y se instanció esta tarea.
     */
    private LocalDateTime fechaGeneracion;

    /**
     * Constructor para inicializar la tarea de preparación.
     * <p>
     * Recibe el contexto completo de la transacción (Quién compra, Qué compra, Quién atiende y Cuándo).
     *
     * @param cliente         Usuario {@link Cliente} que espera el pedido.
     * @param idOrden         Clave única de rastreo.
     * @param detalleOrden    Texto descriptivo de los productos.
     * @param vendedor        El {@link VendedorDulceria} responsable (real o bot).
     * @param fechaGeneracion Timestamp de creación.
     */
    public PreparacionDulceria(Usuario cliente, String idOrden, String detalleOrden, VendedorDulceria vendedor, LocalDateTime fechaGeneracion) {
        this.cliente = cliente;
        this.idOrden = idOrden;
        this.detalleOrden = detalleOrden;
        this.vendedor = vendedor;
        this.fechaGeneracion = fechaGeneracion;
    }

    /**
     * Ejecuta la lógica del hilo de preparación.
     * <p>
     * Este método contiene la secuencia de pasos cronometrados que simulan el trabajo en cocina:
     * <ol>
     * <li><b>Notificación Inicial:</b> Avisa al cliente que la orden está en cola.</li>
     * <li><b>Fase de Asignación (Sleep):</b> Simula el tiempo que tarda el vendedor en tomar la orden (20-40s).</li>
     * <li><b>Fase de Preparación (Sleep):</b> Simula el tiempo de cocción/armado (20-30s + 10-15s).</li>
     * <li><b>Notificación Final:</b> Escribe un mensaje personalizado del vendedor indicando que está listo.</li>
     * <li><b>Registro de Bitácora:</b> Escribe un log detallado en el historial del empleado con todos los tiempos.</li>
     * </ol>
     * Maneja internamente las excepciones de interrupción de hilos y escritura de archivos.
     */
    @Override
    public void run() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        
        // Definición de rutas de salida dinámicas basadas en los usuarios involucrados
        String archivoNotif = "notificaciones_" + cliente.getNickname() + ".txt";
        String archivoHistorial = "historial_" + vendedor.getNickname() + ".txt";

        try {
            // 1. Notificar inicio (Comunicación asíncrona con el Cliente via archivo)
            String msjEspera = "Orden " + idOrden + ": Estamos trabajando arduamente para que tus alimentos sean deliciosos. Por favor, espera un poco más =D";
            // 'append = false' para limpiar notificaciones viejas o iniciar limpio el estado actual
            GestorArchivos.escribirTexto(archivoNotif, msjEspera, false); 

            // =================================================================================
            // SIMULACIÓN DE TIEMPOS DE PROCESO (Lógica de Negocio Temporal)
            // =================================================================================
            
            // PAUSA 1: Latencia de asignación (Simula cola de espera) -> 20 a 40 segundos
            Thread.sleep(ThreadLocalRandom.current().nextInt(20000, 40001));
            LocalDateTime fechaAsignacion = LocalDateTime.now();

            // PAUSA 2: Configuración de insumos -> 20 a 30 segundos
            Thread.sleep(ThreadLocalRandom.current().nextInt(20000, 30001));
            LocalDateTime fechaInicioPrep = LocalDateTime.now();

            // PAUSA 3: Ejecución de preparación física -> 10 a 15 segundos
            Thread.sleep(ThreadLocalRandom.current().nextInt(10000, 15001));
            LocalDateTime fechaFinPrep = LocalDateTime.now();

            // =================================================================================
            // FINALIZACIÓN Y PERSISTENCIA
            // =================================================================================

            // 2. Notificar fin (Actualización de estado para el Cliente)
            String msjListo = "Hola, soy " + vendedor.getNickname() + ". Ya está lista tu orden de dulcería. Puedes pasar a recogerla. " + fechaFinPrep.format(DateTimeFormatter.ofPattern("yyyyMMdd:HHmm"));
            GestorArchivos.escribirTexto(archivoNotif, msjListo, false); // Sobrescribe el mensaje de "espera"

            // 3. Registrar Auditoría (KPIs del Empleado)
            // Se genera un reporte estructurado con los tiempos exactos de cada fase
            String logEmpleado = String.format("Orden: %s | Tipo: %s\nGenerada: %s\nAsignada: %s\nIniciada: %s\nTerminada: %s\n-----------------",
                    idOrden, detalleOrden,
                    fechaGeneracion.format(formato),
                    fechaAsignacion.format(formato),
                    fechaInicioPrep.format(formato),
                    fechaFinPrep.format(formato));
            
            // 'append = true' para conservar el historial acumulado del vendedor
            GestorArchivos.escribirTexto(archivoHistorial, logEmpleado, true); 

        } catch (InterruptedException e) {
            // Manejo de interrupción del hilo (ej. cierre de la aplicación)
            System.err.println("La preparación de la orden " + idOrden + " fue interrumpida.");
            e.printStackTrace();
        } catch (IOException e) {
            // Manejo de errores de disco
            System.err.println("Error de E/S al registrar la orden " + idOrden);
            e.printStackTrace();
        }
    }
}
