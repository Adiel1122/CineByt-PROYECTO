package mx.unam.fi.cine.controlador;

import mx.unam.fi.cine.modelo.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Controla el proceso de compra de boletos por parte del Cliente.
 * Maneja la selección de asientos, la simulación concurrente de pago y la generación de tickets.
 */
public class ControladorCompra{
    private Scanner scanner;
    private List<Funcion> funciones; // Referencia a la lista maestra para guardar cambios

    public ControladorCompra(List<Funcion> funciones) {
        this.scanner = new Scanner(System.in);
        this.funciones = funciones;
    }

    /**
     * Inicia el flujo de compra para una función específica.
     * @param cliente El usuario que realiza la compra.
     * @param funcion La función seleccionada.
     */
    public void iniciarCompra(Cliente cliente, Funcion funcion) {
        System.out.println("\n--- COMPRA DE BOLETOS: " + funcion.getPelicula().getTitulo() + " ---");
        
        // 1. Mostrar mapa de asientos [cite: 104]
        mostrarMapaAsientos(funcion.getSala());

        // 2. Selección y Validación de Asientos [cite: 105-106]
        List<Asiento> asientosSeleccionados = solicitarAsientos(funcion.getSala());
        
        if (asientosSeleccionados == null || asientosSeleccionados.isEmpty()) {
            System.out.println("Compra cancelada.");
            return;
        }

        // 3. Simulación de Pago con Hilos (Concurrencia) [cite: 107]
        boolean pagoExitoso = simularPagoConcurrente();

        if (pagoExitoso) {
            // 4. Finalizar Compra: Ocupar asientos y Generar Tickets [cite: 113-116]
            generarTickets(cliente, funcion, asientosSeleccionados);
            
            // Guardar el estado actualizado (asientos ocupados)
            try {
                GestorArchivos.guardarObjeto("funciones.dat", funciones);
            } catch (IOException e) {
                System.out.println("Error al guardar la actualización de la sala.");
            }
        } else {
            System.out.println("La transacción falló. Intente nuevamente.");
        }
    }

    // ==========================================
    // LÓGICA DE SELECCIÓN DE ASIENTOS
    // ==========================================
    private void mostrarMapaAsientos(Sala sala) {
        System.out.println("\nDistribución de la Sala ( [ ]=Libre, [X]=Ocupado ):");
        for (Asiento a : sala.getAsientos()) {
            String estado = a.isOcupado() ? "[X]" : "[" + a.getFila() + a.getNumero() + "]";
            System.out.print(estado + "\t");
            // Salto de línea simple cada cierto número de asientos para visualización básica
            if (a.getNumero() == 15 || (sala.getNombre().contains("VIP") && a.getNumero() == 6)) {
                System.out.println();
            }
        }
        System.out.println();
    }

    private List<Asiento> solicitarAsientos(Sala sala) {
        while (true) {
            System.out.println("\nIngrese los asientos deseados separados por espacio (Ej: A1 B4 C5):");
            System.out.println("O escriba '0' para cancelar.");
            String entrada = scanner.nextLine().toUpperCase();

            if (entrada.equals("0")) return null; // [cite: 103]

            String[] tokens = entrada.split(" ");
            List<Asiento> asientosTemporales = new ArrayList<>();
            boolean errorEncontrado = false;

            for (String token : tokens) {
                // Parsear entrada (Ej. "A10" -> Fila 'A', Numero 10)
                try {
                    char fila = token.charAt(0);
                    int numero = Integer.parseInt(token.substring(1));
                    
                    Asiento asiento = sala.buscarAsiento(fila, numero);

                    if (asiento == null) {
                        System.out.println("Error: El asiento " + token + " no existe en esta sala.");
                        errorEncontrado = true;
                        break;
                    } else if (asiento.isOcupado()) {
                        System.out.println("Error: El asiento " + token + " ya está ocupado. Elija otro."); // [cite: 106]
                        errorEncontrado = true;
                        break;
                    } else {
                        // Verificar duplicados en la misma selección
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
            // Si hubo error, el bucle while repite la solicitud [cite: 106]
        }
    }

    // ==========================================
    // LÓGICA DE HILOS (THREADS)
    // ==========================================
    private boolean simularPagoConcurrente() {
        System.out.println("\nIniciando transacción bancaria...");

        // Hilo 1: Simulación del Banco [cite: 108-110]
        Thread hiloBanco = new Thread(() -> {
            try {
                System.out.println("\n>> Estableciendo conexión con el banco...");
                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 5001)); // Pausa 2-5 seg

                System.out.println("\n>> Haciendo el cargo correspondiente...");
                Thread.sleep(ThreadLocalRandom.current().nextInt(2000, 5001)); // Pausa 2-5 seg

                System.out.println("\n>> Transacción finalizada.");
            } catch (InterruptedException e) {
                System.out.println("Error en la conexión bancaria.");
            }
        });

        // Hilo 2: Barra de Progreso [cite: 111-112]
        Thread hiloBarra = new Thread(() -> {
            char[] barra = {'|', '/', '-', '\\'};
            int i = 0;
            try {
                // Ejecutarse mientras el hilo del banco siga vivo
                while (hiloBanco.isAlive()) {
                    System.out.print("\rProcesando " + barra[i % 4]); // \r regresa al inicio de linea
                    i++;
                    Thread.sleep(500); // Pausa de 0.5 seg
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        hiloBanco.start();
        hiloBarra.start();

        try {
            hiloBanco.join(); // El hilo principal espera a que termine el banco
            hiloBarra.join(); // Esperamos a que termine la barra (que termina cuando banco muere)
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        
        // Pequeña pausa final antes de mostrar resumen [cite: 113]
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        return true;
    }

    // ==========================================
    // GENERACIÓN DE TICKETS
    // ==========================================
    private void generarTickets(Cliente cliente, Funcion funcion, List<Asiento> asientos) {
        System.out.println("\n===============================================");
        System.out.println("             RESUMEN DE COMPRA                 ");
        System.out.println("===============================================");
        System.out.println("Película: " + funcion.getPelicula().getTitulo());
        System.out.println("Horario: " + funcion.getHorario().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        System.out.println("Sala: " + funcion.getSala().getNombre());
        System.out.println("Cliente: " + cliente.getNombre() + " " + cliente.getApPaterno());
        
        double precioBoleto = 60.00; // Precio base ejemplo
        double total = precioBoleto * asientos.size();

        // Formato IDs: Iniciales:AAAAMMDD:hhmm:Sala:Asiento [cite: 115]
        // Reutilizamos la logica del ID de funcion para la primera parte
        String idBase = funcion.getIdFuncion(); 

        for (Asiento asiento : asientos) {
            // Marcar asiento como ocupado
            asiento.setOcupado(true);

            // Generar ID único del boleto
            String idBoleto = String.format("%s:%c%d", idBase, asiento.getFila(), asiento.getNumero());
            
            System.out.println("-----------------------------------------------");
            System.out.println("Asiento: " + asiento.getFila() + asiento.getNumero());
            System.out.println("Ticket ID: " + idBoleto);
            
            // Aquí se podría guardar el ticket en un archivo de texto individual si se desea
            try {
                GestorArchivos.escribirTexto("tickets_" + cliente.getNickname() + ".txt", 
                    "Boleto: " + idBoleto + " | " + funcion.getPelicula().getTitulo(), true);
            } catch (IOException e) {
                // Ignorar error de log no crítico
            }
        }
        
        System.out.println("===============================================");
        System.out.println("TOTAL PAGADO: $" + total);
        System.out.println("Cargo realizado a tarjeta terminación: *" + 
                cliente.getNumeroTarjeta().substring(cliente.getNumeroTarjeta().length() - 4));
        System.out.println("===============================================\n");
    }
}
