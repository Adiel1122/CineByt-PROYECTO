package mx.unam.fi.cine.controlador;

import mx.unam.fi.cine.modelo.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class PreparacionDulceria implements Runnable {

    private Usuario cliente;
    private String idOrden;
    private String nombreVendedor; // Simularemos que un vendedor toma la orden

    public PreparacionDulceria(Usuario cliente, String idOrden, String nombreVendedor) {
        this.cliente = cliente;
        this.idOrden = idOrden;
        this.nombreVendedor = nombreVendedor;
    }

    @Override
    public void run() {
        String archivoNotif = "notificaciones_" + cliente.getNickname() + ".txt";

        try {
            // 1. ESTADO: En espera de asignación
            // Escribimos el mensaje inicial de "Estamos trabajando..." [cite: 154]
            String msjEspera = "Orden " + idOrden + ": Estamos trabajando arduamente para que tus alimentos sean deliciosos. Por favor, espera un poco más =D";
            GestorArchivos.escribirTexto(archivoNotif, msjEspera, true); // append = true para historial

            // Pausa de asignación: 20 a 40 segundos [cite: 169]
            // NOTA: Para pruebas rápidas, puedes dividir estos tiempos entre 10, 
            // pero aquí pongo los reales solicitados.
            int tiempoAsignacion = ThreadLocalRandom.current().nextInt(20000, 40001);
            Thread.sleep(tiempoAsignacion);

            // 2. ESTADO: Preparando
            // Pausa de preparación: 20 a 30 segundos [cite: 170]
            int tiempoPreparacion = ThreadLocalRandom.current().nextInt(20000, 30001);
            Thread.sleep(tiempoPreparacion);

            // 3. ESTADO: Terminando
            // Pausa de finalización: 10 a 15 segundos [cite: 171]
            int tiempoFinal = ThreadLocalRandom.current().nextInt(10000, 15001);
            Thread.sleep(tiempoFinal);

            // 4. ESTADO: Listo
            // Sobrescribimos o añadimos el mensaje final [cite: 155]
            String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd:HHmm"));
            String msjListo = "Hola, soy " + nombreVendedor + ". Ya está lista tu orden " + idOrden + 
                              ". Puedes pasar a recogerla. " + fechaHora;
            
            GestorArchivos.escribirTexto(archivoNotif, msjListo, true);
            
            // También deberíamos actualizar el historial del empleado (requerimiento extra), 
            // pero por ahora nos centramos en la notificación al cliente.

        } catch (InterruptedException | IOException e) {
            System.out.println("Error en la preparación de dulcería: " + e.getMessage());
        }
    }
}