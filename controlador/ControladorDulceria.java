package mx.unam.fi.cine.controlador;

import mx.unam.fi.cine.modelo.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ControladorDulceria {
    private Scanner scanner;

    public ControladorDulceria() {
        this.scanner = new Scanner(System.in);
    }

    public void iniciarDulceria(Cliente cliente) {
        System.out.println("\n--- BIENVENIDO A LA DULCERÍA ---");
        System.out.println("A. Combo 'Amix' (Palomitas + 2 Refrescos Jumbo)");
        System.out.println("B. Combo 'Nachos' (Palomitas + 2 Refrescos + Nachos Jumbo)");
        System.out.println("C. Combo 'Buen Trio' (Palomitas + 3 Refrescos + Nachos Mega)");
        System.out.println("D. Combo 'Qué me ves' (Palomitas + Refresco + Nachos Jumbo)");
        System.out.println("E. Orden Personalizada");
        System.out.println("0. Salir");

        System.out.print("Elige una opción: ");
        String opcion = scanner.nextLine().toUpperCase();

        if (opcion.equals("0")) return;

        double totalPagar = 0;
        String detallesOrden = "";

        if (opcion.equals("E")) {
            // Lógica Opción E: Suma individual [cite: 137]
            List<Producto> listaPersonalizada = armarOrdenPersonalizada();
            if (listaPersonalizada.isEmpty()) return;

            for (Producto p : listaPersonalizada) {
                totalPagar += p.getPrecio();
            }
            detallesOrden = "Orden Personalizada (" + listaPersonalizada.size() + " productos)";

        } else {
            // Lógica Combos: Composición y Descuento [cite: 138]
            Combo comboSeleccionado = crearComboPredefinido(opcion);
            if (comboSeleccionado == null) {
                System.out.println("Opción inválida.");
                return;
            }
            // Personalizar sabores del combo
            personalizarSaboresCombo(comboSeleccionado);
            
            totalPagar = comboSeleccionado.calcularPrecioTotal();
            detallesOrden = comboSeleccionado.getNombre();
        }

        // Finalizar Compra y Lanzar Hilo
        procesarCompraDulceria(cliente, totalPagar, detallesOrden);
    }

    // --- Métodos Auxiliares ---

    private List<Producto> armarOrdenPersonalizada() {
        List<Producto> cuenta = new ArrayList<>();
        boolean continuar = true;
        
        // Precios base simulados (deberían venir de archivo según PDF, hardcodeados por simplicidad ahora)
        double pPalomita = 80.0, pRefresco = 60.0, pNachos = 70.0;

        while (continuar) {
            System.out.println("\nAgregando productos:");
            System.out.println("1. Palomitas ($80) | 2. Refresco ($60) | 3. Nachos ($70) | 4. Terminar");
            int sel = Integer.parseInt(scanner.nextLine()); // Try-catch omitido por brevedad

            if (sel == 4) break;

            switch (sel) {
                case 1:
                    System.out.print("Sabor (Mantequilla/Queso/Caramelo): ");
                    String sabP = scanner.nextLine();
                    cuenta.add(new Producto("Palomitas " + sabP, pPalomita));
                    break;
                case 2:
                    System.out.print("Sabor (Cola/Naranja/Limon): ");
                    String sabR = scanner.nextLine();
                    cuenta.add(new Producto("Refresco " + sabR, pRefresco));
                    break;
                case 3:
                    System.out.print("Tamaño (Chico/Grande): ");
                    String tamN = scanner.nextLine();
                    cuenta.add(new Producto("Nachos " + tamN, pNachos));
                    break;
            }
        }
        return cuenta;
    }

    private Combo crearComboPredefinido(String opcion) {
        // Precios base para cálculo
        double pPalomita = 80.0, pRefresco = 60.0, pNachos = 70.0;
        Combo c = null;

        switch (opcion) {
            case "A": // Amix
                c = new Combo("Combo Amix");
                c.agregarProducto(new Producto("Palomitas", pPalomita));
                c.agregarProducto(new Producto("Refresco Jumbo", pRefresco));
                c.agregarProducto(new Producto("Refresco Jumbo", pRefresco));
                break;
            case "B": // Nachos
                c = new Combo("Combo Nachos");
                c.agregarProducto(new Producto("Palomitas", pPalomita));
                c.agregarProducto(new Producto("Refresco", pRefresco));
                c.agregarProducto(new Producto("Refresco", pRefresco));
                c.agregarProducto(new Producto("Nachos Jumbo", pNachos));
                break;
            // ... Implementar C y D similarmente
            default: return null;
        }
        return c;
    }

    private void personalizarSaboresCombo(Combo c) {
        System.out.println("Personalizando " + c.getNombre() + "...");
        // Simulación simple: preguntamos un sabor general para no iterar demasiado
        System.out.print("¿Sabor de palomitas?: ");
        String sabP = scanner.nextLine();
        System.out.print("¿Sabor de refrescos?: ");
        String sabR = scanner.nextLine();
        System.out.println("Sabores registrados: " + sabP + ", " + sabR);
    }

    private void procesarCompraDulceria(Cliente cliente, double total, String detalle) {
        System.out.println("\nTotal a pagar: $" + total);
        System.out.println("Simulando pago...");
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        System.out.println("¡Pago exitoso!");

        // Generar Clave [cite: 140]
        // Iniciales:AAAAMMDD:hhmm
        StringBuilder iniciales = new StringBuilder();
        iniciales.append(cliente.getNombre().charAt(0));
        iniciales.append(cliente.getApPaterno().charAt(0));
        
        LocalDateTime ahora = LocalDateTime.now();
        String fechaHora = ahora.format(DateTimeFormatter.ofPattern("yyyyMMdd:HHmm"));
        
        String claveCompra = iniciales.toString().toUpperCase() + ":" + fechaHora;

        System.out.println("Tu clave de orden es: " + claveCompra);
        System.out.println("Revisa la sección de notificaciones para ver el estado.");

        // Lanzar Hilo de Preparación [cite: 144, 159]
        // Simulamos que lo atiende "Juan Perez"
        PreparacionDulceria hilo = new PreparacionDulceria(cliente, claveCompra, "Juan Perez");
        Thread t = new Thread(hilo);
        t.start();
    }
}
