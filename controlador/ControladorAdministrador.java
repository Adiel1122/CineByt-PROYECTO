package mx.unam.fi.cine.controlador;

import mx.unam.fi.cine.modelo.*;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

/**
 * Controla la lógica de negocio específica para el rol de Administrador.
 * Maneja el alta de películas, funciones y empleados.
 */
public class ControladorAdministrador {

    private List<Pelicula> peliculas;
    private List<Funcion> funciones;
    private List<Usuario> usuarios;
    private Scanner scanner;

    /**
     * Constructor. Recibe las listas maestras de datos para poder modificarlas.
     */
    public ControladorAdministrador(List<Pelicula> peliculas, List<Funcion> funciones, List<Usuario> usuarios) {
        this.peliculas = peliculas;
        this.funciones = funciones;
        this.usuarios = usuarios;
        this.scanner = new Scanner(System.in);
    }

    // ==========================================
    // 1. ALTA DE PELÍCULA [cite: 43-48]
    // ==========================================
    public void darAltaPelicula() {
        System.out.println("\n--- ALTA DE PELÍCULA ---");
        try {
            System.out.print("Título: ");
            String titulo = scanner.nextLine();

            System.out.print("Género(s): ");
            String genero = scanner.nextLine();

            System.out.print("Sinopsis: ");
            String sinopsis = scanner.nextLine();

            int duracion = leerEntero("Duración en minutos (ej. 120): ");

            Pelicula nuevaPeli = new Pelicula(titulo, genero, sinopsis, duracion);
            peliculas.add(nuevaPeli);
            
            // Guardar cambios
            guardarDatos();
            System.out.println(">> Película registrada exitosamente.");

        } catch (Exception e) {
            System.out.println("Error al registrar película: " + e.getMessage());
        }
    }

    // ==========================================
    // 2. ALTA DE FUNCIÓN (Lógica Compleja) [cite: 49-60]
    // ==========================================
    public void darAltaFuncion() {
        System.out.println("\n--- ALTA DE FUNCIÓN ---");
        
        if (peliculas.isEmpty()) {
            System.out.println("No hay películas registradas. Registre una primero.");
            return;
        }

        // 1. Listar películas [cite: 50]
        System.out.println("Seleccione una película:");
        for (int i = 0; i < peliculas.size(); i++) {
            System.out.println((i + 1) + ". " + peliculas.get(i).getTitulo());
        }
        
        int indicePeli = leerEntero("Número de película: ") - 1;
        if (indicePeli < 0 || indicePeli >= peliculas.size()) {
            System.out.println("Opción inválida.");
            return;
        }
        Pelicula peliSeleccionada = peliculas.get(indicePeli);

        // 2. Pedir fecha y sala [cite: 51-52]
        LocalDate fecha = leerFecha("Ingrese fecha de la función (dd/MM/yyyy): ");
        if (fecha == null) return; // Error en fecha

        System.out.println("Seleccione Sala: 1. Sala A | 2. Sala B | 3. Sala VIP");
        int opSala = leerEntero("Opción: ");
        String nombreSala = "";
        switch (opSala) {
            case 1: nombreSala = "Sala A"; break;
            case 2: nombreSala = "Sala B"; break;
            case 3: nombreSala = "Sala VIP"; break;
            default: System.out.println("Sala inválida."); return;
        }

        // 3. Mostrar programación actual de esa sala en esa fecha [cite: 53]
        System.out.println("\n--- Funciones actuales en " + nombreSala + " para el " + fecha + " ---");
        boolean hayFunciones = false;
        for (Funcion f : funciones) {
            if (f.getSala().getNombre().equals(nombreSala) && f.getHorario().toLocalDate().equals(fecha)) {
                System.out.println("- " + f.getHorario().toLocalTime() + " | " + f.getPelicula().getTitulo() + 
                                   " (Termina: " + f.getHorario().plusMinutes(f.getPelicula().getDuracionMinutos()).toLocalTime() + ")");
                hayFunciones = true;
            }
        }
        if (!hayFunciones) System.out.println("(Sin funciones programadas)");

        // 4. Confirmar o Cancelar [cite: 54]
        System.out.print("Escriba 'Alta' para continuar o 'Cancelar' para salir: ");
        String confirmacion = scanner.nextLine();
        if (!confirmacion.equalsIgnoreCase("Alta")) return;

        // 5. Validar Horario (Bucle hasta que sea válido) [cite: 56-58]
        boolean horarioValido = false;
        while (!horarioValido) {
            try {
                System.out.println("Ingrese hora de inicio (formato 24h):");
                int hora = leerEntero("Hora (0-23): ");
                int minuto = leerEntero("Minuto (0-59): ");

                LocalTime horaInicio = LocalTime.of(hora, minuto);
                LocalDateTime fechaHoraInicio = LocalDateTime.of(fecha, horaInicio);
                
                // VALIDACIÓN DE CRUCE DE HORARIOS
                if (validarDisponibilidadSala(nombreSala, fechaHoraInicio, peliSeleccionada.getDuracionMinutos())) {
                    // Crear y guardar
                    Funcion nuevaFuncion = new Funcion(peliSeleccionada, nombreSala, fechaHoraInicio);
                    funciones.add(nuevaFuncion);
                    guardarDatos();
                    System.out.println(">> Función registrada con éxito. ID: " + nuevaFuncion.getIdFuncion());
                    horarioValido = true;
                } else {
                    System.out.println("¡ERROR! El horario se cruza con otra función (se requieren 30 min de limpieza).");
                    System.out.print("Escriba 'Cancelar' para salir o presione Enter para intentar otra hora: ");
                    if (scanner.nextLine().equalsIgnoreCase("Cancelar")) return;
                }

            } catch (Exception e) {
                System.out.println("Hora inválida: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 3. REGISTRO DE EMPLEADOS [cite: 71-81]
    // ==========================================
    public void registrarEmpleado() {
        System.out.println("\n--- REGISTRO DE NUEVO EMPLEADO ---");
        try {
            System.out.print("Nombre(s): "); String nombre = scanner.nextLine();
            System.out.print("Apellido Paterno: "); String apPaterno = scanner.nextLine();
            System.out.print("Apellido Materno: "); String apMaterno = scanner.nextLine();
            int edad = leerEntero("Edad: ");
            
            System.out.print("Nickname: "); String nick = scanner.nextLine();
            System.out.print("Contraseña: "); String pass = scanner.nextLine();
            System.out.print("Email: "); String email = scanner.nextLine();
            System.out.print("Celular: "); String cel = scanner.nextLine();

            System.out.println("Turno: 1. Matutino | 2. Vespertino | 3. Nocturno");
            int opTurno = leerEntero("Opción: ");
            Empleado.Turno turno = Empleado.Turno.MATUTINO;
            if (opTurno == 2) turno = Empleado.Turno.VESPERTINO;
            if (opTurno == 3) turno = Empleado.Turno.NOCTURNO;

            System.out.println("Tipo de Empleado: 1. Administrador | 2. Vendedor Dulcería");
            int tipo = leerEntero("Opción: ");

            if (tipo == 1) {
                // Administrador
                System.out.println("¿Es administrador de Fin de Semana? (1. Sí / 2. No)");
                boolean esFinSemana = leerEntero("Opción: ") == 1;
                
                Administrador nuevoAdmin = new Administrador(nombre, apPaterno, apMaterno, edad, nick, pass, email, cel, turno, esFinSemana);
                usuarios.add(nuevoAdmin);

            } else if (tipo == 2) {
                // Vendedor
                System.out.print("Día de descanso (ej. Lunes): ");
                String diaDescanso = scanner.nextLine();
                
                VendedorDulceria nuevoVendedor = new VendedorDulceria(nombre, apPaterno, apMaterno, edad, nick, pass, email, cel, turno, diaDescanso);
                usuarios.add(nuevoVendedor);
            } else {
                System.out.println("Tipo inválido.");
                return;
            }

            guardarDatos();
            System.out.println(">> Empleado registrado exitosamente.");

        } catch (Exception e) {
            System.out.println("Error en registro: " + e.getMessage());
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES Y DE VALIDACIÓN
    // ==========================================

    /**
     * Verifica si hay espacio para una película considerando su duración + 30 min de limpieza.
     */
    private boolean validarDisponibilidadSala(String nombreSala, LocalDateTime inicioNueva, int duracionMinutos) {
        LocalDateTime finNueva = inicioNueva.plusMinutes(duracionMinutos);

        for (Funcion f : funciones) {
            // Solo verificar misma sala
            if (f.getSala().getNombre().equals(nombreSala)) {
                LocalDateTime inicioExistente = f.getHorario();
                LocalDateTime finExistente = inicioExistente.plusMinutes(f.getPelicula().getDuracionMinutos());

                // Regla: 30 minutos de limpieza 
                // El inicio de la nueva debe ser al menos 30 min después del fin de la existente
                // Y el fin de la nueva debe ser al menos 30 min antes del inicio de la existente
                
                LocalDateTime finExistenteMasLimpieza = finExistente.plusMinutes(30);
                LocalDateTime inicioExistenteMenosLimpieza = inicioExistente.minusMinutes(30);

                // Hay colisión si:
                // (Nueva empieza antes de que termine la vieja + 30) Y (Nueva termina después de que empiece la vieja - 30)
                boolean colision = inicioNueva.isBefore(finExistenteMasLimpieza) && finNueva.isAfter(inicioExistenteMenosLimpieza);
                
                if (colision) return false;
            }
        }
        return true;
    }

    private int leerEntero(String mensaje) {
        while (true) {
            try {
                System.out.print(mensaje);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Por favor ingrese un número válido.");
            }
        }
    }

    private LocalDate leerFecha(String mensaje) {
        try {
            System.out.print(mensaje);
            String fechaStr = scanner.nextLine();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(fechaStr, formatter);
        } catch (DateTimeParseException e) {
            System.out.println("Formato de fecha inválido. Use dd/MM/yyyy");
            return null;
        }
    }

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
