package mx.unam.fi.cine.modelo;

import java.io.*;

/**
 * Representa un artículo individual vendible en la dulcería del sistema <b>CineByt</b>.
 * <p>
 * Esta clase actúa como la unidad base de inventario. Puede representar ítems simples
 * (ej. "Refresco Grande") o ser utilizada como componente para construir ofertas más complejas
 * mediante el patrón de Composición (ver {@link Combo}).
 * </p>
 * <b>Integración de Datos:</b>
 * <br>
 * Los precios y nombres base suelen cargarse desde el archivo de configuración {@code PreciosProductos.txt}
 * gestionado por el {@link mx.unam.fi.cine.controlador.ControladorDulceria}, aunque las instancias
 * de esta clase son las que viajan a través del sistema durante la transacción.
 *
 * @author Equipo CineByt
 * @version 1.0
 * @see mx.unam.fi.cine.modelo.Combo
 * @see mx.unam.fi.cine.controlador.ControladorDulceria
 */
public class Producto implements Serializable {

    /**
     * Identificador de versión para la serialización.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Nombre descriptivo del producto.
     * Ejemplo: "Palomitas Jumbo Mantequilla" o "Nachos con Queso Extra".
     */
    private String nombre; 

    /**
     * Precio unitario de venta al público.
     * <p>
     * <b>Nota de Diseño:</b> Se utiliza el tipo primitivo {@code double} para facilitar
     * las operaciones aritméticas en este prototipo académico. En un entorno de producción
     * real, se recomendaría el uso de {@code java.math.BigDecimal} para evitar errores
     * de precisión en punto flotante durante cálculos financieros.
     */
    private double precio;

    /**
     * Constructor para inicializar un nuevo producto.
     * * @param nombre Descripción del producto.
     * @param precio Costo unitario (debe ser positivo).
     */
    public Producto(String nombre, double precio) {
        this.nombre = nombre;
        this.precio = precio;
    }

    /**
     * Obtiene el nombre del producto.
     * @return Cadena con la descripción.
     */
    public String getNombre() { return nombre; }

    /**
     * Obtiene el precio del producto.
     * @return Valor numérico del costo.
     */
    public double getPrecio() { return precio; }

    /**
     * Representación textual del producto formateada para menús y recibos.
     * <p>
     * Incluye el símbolo de moneda y formatea el precio a dos decimales.
     * * @return Cadena formato "Nombre ($Precio)".
     */
    @Override
    public String toString() {
        return String.format("%s ($%.2f)", nombre, precio);
    }
}
