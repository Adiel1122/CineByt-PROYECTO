package mx.unam.fi.cine.modelo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de persistencia unificado para el sistema <b>CineByt</b>.
 * <p>
 * Esta clase utilitaria (estática) abstrae la complejidad de las operaciones de Entrada/Salida (I/O)
 * de Java, proporcionando una interfaz limpia para dos mecanismos de almacenamiento:
 * </p>
 * <ol>
 * <li><b>Serialización Binaria (.dat):</b> Para la persistencia de estado de objetos complejos del Modelo
 * (Listas de {@code Usuario}, {@code Funcion}, etc.). Mantiene la integridad de las relaciones entre objetos.</li>
 * <li><b>Texto Plano (.txt):</b> Para la generación de reportes legibles, tickets de venta y
 * lectura de configuraciones externas (ej. precios de dulcería).</li>
 * </ol>
 * <b>Estructura de Directorios:</b>
 * <br>
 * La clase garantiza la existencia de un entorno controlado mediante la carpeta {@code ArchivosAplicacion},
 * centralizando todos los recursos externos del sistema.
 *
 * @author Equipo CineByt
 * @version 1.0
 * @see java.io.Serializable
 * @see java.io.ObjectOutputStream
 */
public class GestorArchivos {

    /**
     * Nombre del directorio raíz donde se centralizará toda la persistencia del sistema.
     * Facilita la portabilidad de la aplicación al no depender de rutas absolutas del sistema operativo.
     */
    public static final String CARPETA_ARCHIVOS = "ArchivosAplicacion";

    /*
     * Bloque de inicialización estática.
     * --------------------------------------------------------------------------------------
     * Este bloque se ejecuta una única vez cuando la clase es cargada por la JVM (Classloader).
     * Su propósito arquitectónico es garantizar la integridad del entorno: verifica si la
     * carpeta de almacenamiento existe; si no, la crea antes de que cualquier método intente
     * escribir en ella, previniendo excepciones de tipo FileNotFoundException por directorios inexistentes.
     */
    static {
        File directorio = new File(CARPETA_ARCHIVOS);
        if (!directorio.exists()) {
            if (directorio.mkdir()) {
                System.out.println("LOG SISTEMA: Carpeta de persistencia '" + CARPETA_ARCHIVOS + "' inicializada correctamente.");
            } else {
                System.err.println("ERROR CRÍTICO: No se pudo crear el directorio de persistencia.");
            }
        }
    }

    // ==========================================
    // MÉTODOS PARA SERIALIZACIÓN (Objetos .dat)
    // ==========================================

    /**
     * Persiste el estado de un objeto (o grafo de objetos) en un archivo binario.
     * <p>
     * Utiliza {@link ObjectOutputStream} para transformar las instancias en un flujo de bytes.
     * Es fundamental para guardar la "base de datos" en memoria del cine (Listas de usuarios, cartelera actual).
     * </p>
     * <b>Manejo de Recursos:</b>
     * Utiliza <i>Try-with-resources</i> para asegurar el cierre automático del flujo (stream),
     * evitando fugas de memoria o bloqueos de archivo.
     *
     * @param nombreArchivo Nombre del archivo destino (ej: "usuarios.dat").
     * @param objeto        La instancia a serializar. Debe implementar la interfaz {@link Serializable}.
     * @throws IOException Si ocurre un fallo en el acceso al disco o durante la escritura de bytes.
     */
    public static void guardarObjeto(String nombreArchivo, Object objeto) throws IOException {
        String rutaCompleta = CARPETA_ARCHIVOS + File.separator + nombreArchivo;
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(rutaCompleta))) {
            oos.writeObject(objeto);
        }
    }

    /**
     * Recupera (deserializa) un objeto desde un archivo binario.
     * <p>
     * Reconstruye el estado de los objetos guardados previamente. Es utilizado durante
     * el arranque del sistema (en {@code CineByt.main}) para cargar la información histórica.
     * </p>
     *
     * @param nombreArchivo Nombre del archivo fuente.
     * @return El objeto reconstruido (tipo {@code Object}). El invocador es responsable de hacer el <i>Casting</i> correcto.
     * @throws IOException            Si el archivo no existe o es ilegible.
     * @throws ClassNotFoundException Si el archivo contiene una clase que no coincide con el código actual del proyecto (versiones incompatibles).
     */
    public static Object leerObjeto(String nombreArchivo) throws IOException, ClassNotFoundException {
        String rutaCompleta = CARPETA_ARCHIVOS + File.separator + nombreArchivo;
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(rutaCompleta))) {
            return ois.readObject();
        }
    }

    // ==========================================
    // MÉTODOS PARA TEXTO PLANO (.txt)
    // ==========================================

    /**
     * Escribe cadenas de caracteres en un archivo de texto plano.
     * <p>
     * Diseñado para operaciones de reporte y notificación donde el formato humano es prioritario.
     * </p>
     * <b>Usos Comunes:</b>
     * <ul>
     * <li>Generación de Tickets de compra (ControladorCompra).</li>
     * <li>Notificaciones de pedidos listos (ControladorDulceria).</li>
     * <li>Logs de auditoría del sistema.</li>
     * </ul>
     *
     * @param nombreArchivo Nombre del archivo destino (ej: "Ticket_123.txt").
     * @param contenido     La cadena de texto a escribir.
     * @param append        Bandera de modo de escritura:
     * <ul>
     * <li>{@code true}: (Append) Agrega el texto al final del archivo sin borrar lo anterior (útil para logs).</li>
     * <li>{@code false}: (Overwrite) Sobrescribe el archivo completo (útil para tickets nuevos).</li>
     * <li>Se utiliza {@link BufferedWriter} para optimizar la escritura en búferes.</li>
     * </ul>
     * @throws IOException Si falla la operación de escritura.
     */
    public static void escribirTexto(String nombreArchivo, String contenido, boolean append) throws IOException {
        String rutaCompleta = CARPETA_ARCHIVOS + File.separator + nombreArchivo;
        
        // FileWriter recibe el flag 'append' en su constructor
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(rutaCompleta, append))) {
            bw.write(contenido);
            bw.newLine(); // Garantiza la separación de registros mediante salto de línea del sistema
        }
    }

    /**
     * Lee secuencialmente el contenido de un archivo de texto.
     * <p>
     * Carga información línea por línea en memoria. Es utilizado críticamente por el
     * {@code ControladorDulceria} para leer el catálogo de precios desde {@code PreciosProductos.txt}.
     * </p>
     *
     * @param nombreArchivo Nombre del archivo a leer.
     * @return Una lista de {@code String}, donde cada elemento representa una línea del archivo original.
     * @throws IOException Si el archivo no se encuentra o no se puede leer.
     */
    public static List<String> leerArchivoTexto(String nombreArchivo) throws IOException {
        String rutaCompleta = CARPETA_ARCHIVOS + File.separator + nombreArchivo;
        List<String> lineas = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(rutaCompleta))) {
            String linea;
            // Lectura en bucle hasta el fin del archivo (EOF)
            while ((linea = br.readLine()) != null) {
                lineas.add(linea);
            }
        }
        return lineas;
    }

    /**
     * Verifica la existencia física de un archivo en el directorio de la aplicación.
     * <p>
     * Método auxiliar preventivo utilizado antes de intentar operaciones de lectura,
     * permitiendo al sistema tomar decisiones (como crear un archivo por defecto) si
     * los datos no existen.
     * </p>
     *
     * @param nombreArchivo Nombre del archivo a verificar.
     * @return {@code true} si el archivo existe y es legible; {@code false} en caso contrario.
     */
    public static boolean existeArchivo(String nombreArchivo) {
        File archivo = new File(CARPETA_ARCHIVOS + File.separator + nombreArchivo);
        return archivo.exists();
    }
}
