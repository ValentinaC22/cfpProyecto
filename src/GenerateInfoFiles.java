import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Genera los archivos planos pseudoaleatorios que sirven como entrada para la
 * clase {@code main}: productos, vendedores y un archivo de ventas por vendedor.
 *
 * <p>Formatos generados:</p>
 * <ul>
 *   <li>{@code productos.csv}: {@code ID;Nombre;PrecioPorUnidad}</li>
 *   <li>{@code vendedores.csv}: {@code TipoDoc;Documento;Nombres;Apellidos}</li>
 *   <li>{@code vendedor_<documento>.csv}: primera linea
 *       {@code TipoDoc;Documento}; luego una venta por linea
 *       {@code IDProducto;Cantidad;}</li>
 * </ul>
 *
 * <p>No solicita informacion al usuario. Al terminar muestra un mensaje de
 * exito o un mensaje de error.</p>
 *
 * @version 2.0
 */
public class GenerateInfoFiles {

    /** Nombre del archivo de productos. */
    private static final String PRODUCTS_FILE = "productos.csv";

    /** Nombre del archivo de vendedores. */
    private static final String SALESMEN_FILE = "vendedores.csv";

    /** Prefijo de los archivos de ventas. */
    private static final String SALES_FILE_PREFIX = "vendedor_";

    /** Extension de los archivos de ventas. */
    private static final String SALES_FILE_EXTENSION = ".csv";

    /** Separador de campos. */
    private static final String SEPARATOR = ";";

    /** Tipo de documento de los vendedores generados. */
    private static final String DOCUMENT_TYPE = "CC";

    /** Cantidad maxima de unidades en una venta. */
    private static final int MAX_QUANTITY = 10;

    /** Generador pseudoaleatorio. */
    private static final Random RANDOM = new Random();

    /** Nombres reales de personas. */
    private static final String[] FIRST_NAMES = {
        "Juan", "Maria", "Carlos", "Ana", "Luis", "Laura", "Andres", "Camila",
        "Jorge", "Sofia", "Miguel", "Valentina", "Diego", "Daniela", "Felipe",
        "Paula", "Santiago", "Carolina", "Sebastian", "Natalia"
    };

    /** Apellidos reales. */
    private static final String[] LAST_NAMES = {
        "Garcia", "Rodriguez", "Martinez", "Lopez", "Gonzalez", "Perez",
        "Sanchez", "Ramirez", "Torres", "Diaz", "Vargas", "Castro", "Rojas",
        "Moreno", "Jimenez", "Herrera", "Medina", "Ortiz", "Gomez", "Cardenas"
    };

    /** Nombres base de productos. */
    private static final String[] PRODUCT_NAMES = {
        "Cuaderno", "Lapiz", "Borrador", "Regla", "Tijeras", "Pegante",
        "Marcador", "Carpeta", "Calculadora", "Mochila", "Resaltador",
        "Sacapuntas", "Agenda", "Pintura", "Pincel", "Papel", "Libro",
        "Cartulina", "Compas", "Escuadra"
    };

    /** Cantidad de productos del catalogo generado (define los IDs validos). */
    private static int productsCount = 10;

    /**
     * Punto de entrada: genera productos, vendedores y las ventas de cada uno.
     *
     * @param args argumentos de linea de comandos (no se usan)
     */
    public static void main(String[] args) {
        try {
            createProductsFile(10);
            createSalesManInfoFile(5);
            createSalesFilesForAllSalesmen();
            System.out.println("Archivos CSV de prueba generados exitosamente.");
        } catch (IOException | RuntimeException e) {
            System.err.println("Error generando los archivos: " + e.getMessage());
        }
    }

    /**
     * Crea el archivo de productos con formato {@code ID;Nombre;Precio}.
     * Los IDs son {@code P1, P2, ...}.
     *
     * @param productsCount cantidad de productos a generar (mayor que cero)
     * @throws IOException si no se puede escribir el archivo
     */
    public static void createProductsFile(int productsCount) throws IOException {
        if (productsCount <= 0) {
            throw new IllegalArgumentException("La cantidad de productos debe ser mayor que cero.");
        }
        try (PrintWriter writer = newWriter(PRODUCTS_FILE)) {
            for (int i = 1; i <= productsCount; i++) {
                String baseName = PRODUCT_NAMES[(i - 1) % PRODUCT_NAMES.length];
                int variant = (i - 1) / PRODUCT_NAMES.length;
                String name = variant == 0 ? baseName : baseName + " modelo " + (variant + 1);
                long price = (10 + RANDOM.nextInt(490)) * 100L;
                writer.println("P" + i + SEPARATOR + name + SEPARATOR + price);
            }
        }
        GenerateInfoFiles.productsCount = productsCount;
    }

    /**
     * Crea el archivo de vendedores con formato
     * {@code TipoDoc;Documento;Nombres;Apellidos}. Los documentos no se repiten.
     *
     * @param salesmanCount cantidad de vendedores a generar (mayor que cero)
     * @throws IOException si no se puede escribir el archivo
     */
    public static void createSalesManInfoFile(int salesmanCount) throws IOException {
        if (salesmanCount <= 0) {
            throw new IllegalArgumentException("La cantidad de vendedores debe ser mayor que cero.");
        }
        Set<Long> usedDocuments = new HashSet<>();
        try (PrintWriter writer = newWriter(SALESMEN_FILE)) {
            while (usedDocuments.size() < salesmanCount) {
                long docNumber = 1000000000L + RANDOM.nextInt(900000000);
                if (usedDocuments.add(docNumber)) {
                    String name = FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
                    String lastName = LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)] + " "
                            + LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)];
                    writer.println(DOCUMENT_TYPE + SEPARATOR + docNumber + SEPARATOR
                            + name + SEPARATOR + lastName);
                }
            }
        }
    }

    /**
     * Crea el archivo de ventas pseudoaleatorio de UN vendedor. La primera
     * linea es {@code TipoDoc;Documento} y cada linea siguiente es
     * {@code IDProducto;Cantidad;}.
     *
     * @param randomSalesCount cantidad de ventas a generar
     * @param name nombre del vendedor (se usa para informar el avance)
     * @param id numero de documento del vendedor
     * @throws IOException si no se puede escribir el archivo
     */
    public static void createSalesMenFile(int randomSalesCount, String name, long id)
            throws IOException {
        if (randomSalesCount < 0) {
            throw new IllegalArgumentException("La cantidad de ventas no puede ser negativa.");
        }
        String fileName = SALES_FILE_PREFIX + id + SALES_FILE_EXTENSION;
        try (PrintWriter writer = newWriter(fileName)) {
            writer.println(DOCUMENT_TYPE + SEPARATOR + id);
            for (int i = 0; i < randomSalesCount; i++) {
                String productId = "P" + (1 + RANDOM.nextInt(productsCount));
                int quantity = 1 + RANDOM.nextInt(MAX_QUANTITY);
                writer.println(productId + SEPARATOR + quantity + SEPARATOR);
            }
        }
        System.out.println("Ventas generadas para " + name + " (" + id + ").");
    }

    /**
     * Lee {@code vendedores.csv} y crea un archivo de ventas (entre 3 y 8
     * ventas) para cada vendedor.
     *
     * @throws IOException si no se puede leer o escribir algun archivo
     */
    private static void createSalesFilesForAllSalesmen() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get(SALESMEN_FILE), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(SEPARATOR);
                if (parts.length >= 4) {
                    long docNumber = Long.parseLong(parts[1].trim());
                    String fullName = parts[2].trim() + " " + parts[3].trim();
                    createSalesMenFile(3 + RANDOM.nextInt(6), fullName, docNumber);
                }
            }
        }
    }

    /**
     * Abre un escritor UTF-8 para el archivo indicado.
     *
     * @param fileName nombre del archivo
     * @return escritor listo para usarse
     * @throws IOException si no se puede abrir el archivo
     */
    private static PrintWriter newWriter(String fileName) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8));
    }
}
