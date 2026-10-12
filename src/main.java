import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee los archivos generados por {@code GenerateInfoFiles} y crea dos reportes
 * CSV: {@code reporte_vendedores.csv} (nombre;dinero recaudado, de mayor a
 * menor) y {@code reporte_productos.csv} (nombre;precio, de mayor a menor
 * cantidad vendida).
 *
 * <p>Extras: procesa varios archivos por vendedor y detecta datos erroneos o
 * incoherentes (formato incorrecto, productos inexistentes, cantidades o
 * precios negativos, vendedores desconocidos). Esas lineas se ignoran y se
 * muestra una advertencia.</p>
 *
 * <p>No solicita informacion al usuario. Muestra un mensaje de exito o de
 * error al terminar.</p>
 *
 * @version 2.0
 */
public class main {

    /** Archivo de entrada con los productos. */
    private static final String PRODUCTS_FILE = "productos.csv";

    /** Archivo de entrada con los vendedores. */
    private static final String SALESMEN_FILE = "vendedores.csv";

    /** Prefijo de los archivos de ventas. */
    private static final String SALES_FILE_PREFIX = "vendedor_";

    /** Extension de los archivos de ventas. */
    private static final String SALES_FILE_EXTENSION = ".csv";

    /** Separador de campos. */
    private static final String SEPARATOR = ";";

    /** Precio por ID de producto. */
    private static final Map<String, Long> productPrices = new HashMap<>();

    /** Nombre por ID de producto. */
    private static final Map<String, String> productNames = new HashMap<>();

    /** Unidades vendidas por ID de producto. */
    private static final Map<String, Long> productQuantities = new HashMap<>();

    /** Nombre completo por documento de vendedor. */
    private static final Map<Long, String> salesmanNames = new HashMap<>();

    /** Dinero recaudado por documento de vendedor. */
    private static final Map<Long, Long> salesmanTotals = new HashMap<>();

    /** Advertencias encontradas al procesar los archivos. */
    private static final List<String> warnings = new ArrayList<>();

    /**
     * Punto de entrada: carga datos, procesa ventas y genera los reportes.
     *
     * @param args argumentos de linea de comandos (no se usan)
     */
    public static void main(String[] args) {
        try {
            loadProducts(PRODUCTS_FILE);
            loadSalesmen(SALESMEN_FILE);
            processSalesFiles();
            generateSalesmenReport("reporte_vendedores.csv");
            generateProductsReport("reporte_productos.csv");
            for (String warning : warnings) {
                System.out.println("Advertencia: " + warning);
            }
            System.out.println("Procesamiento completado. Reportes creados con exito.");
        } catch (IOException | RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Abre un lector UTF-8; si el archivo no existe lanza un error claro.
     *
     * @param file archivo a leer
     * @return lector del archivo
     * @throws IOException si el archivo no existe o no se puede abrir
     */
    private static BufferedReader openReader(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("No se encontro el archivo " + file.getName()
                    + ". Ejecute primero GenerateInfoFiles.");
        }
        return Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
    }

    /**
     * Carga el catalogo de productos ({@code ID;Nombre;Precio}). Ignora las
     * lineas con formato erroneo, precio negativo o ID repetido.
     *
     * @param filename nombre del archivo de productos
     * @throws IOException si no se puede leer el archivo
     */
    private static void loadProducts(String filename) throws IOException {
        try (BufferedReader br = openReader(new File(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(SEPARATOR);
                try {
                    if (parts.length < 3) {
                        throw new NumberFormatException("faltan campos");
                    }
                    String id = parts[0].trim();
                    long price = Long.parseLong(parts[2].trim());
                    if (price < 0 || productPrices.containsKey(id)) {
                        warnings.add("Producto incoherente (precio negativo o ID repetido): " + line);
                        continue;
                    }
                    productPrices.put(id, price);
                    productNames.put(id, parts[1].trim());
                    productQuantities.put(id, 0L);
                } catch (NumberFormatException e) {
                    warnings.add("Producto con formato erroneo: " + line);
                }
            }
        }
    }

    /**
     * Carga los vendedores ({@code TipoDoc;Documento;Nombres;Apellidos}).
     *
     * @param filename nombre del archivo de vendedores
     * @throws IOException si no se puede leer el archivo
     */
    private static void loadSalesmen(String filename) throws IOException {
        try (BufferedReader br = openReader(new File(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(SEPARATOR);
                try {
                    if (parts.length < 4) {
                        throw new NumberFormatException("faltan campos");
                    }
                    long docNumber = Long.parseLong(parts[1].trim());
                    salesmanNames.put(docNumber, parts[2].trim() + " " + parts[3].trim());
                    salesmanTotals.put(docNumber, 0L);
                } catch (NumberFormatException e) {
                    warnings.add("Vendedor con formato erroneo: " + line);
                }
            }
        }
    }

    /**
     * Procesa todos los archivos {@code vendedor_*.csv} de la carpeta. Un
     * vendedor puede tener varios archivos; todos se suman.
     *
     * @throws IOException si no hay archivos de ventas o no se puede explorar la carpeta
     */
    private static void processSalesFiles() throws IOException {
        File[] files = new File(".").listFiles((dir, name) ->
                name.startsWith(SALES_FILE_PREFIX) && name.endsWith(SALES_FILE_EXTENSION));
        if (files == null || files.length == 0) {
            throw new IOException("No se encontraron archivos de ventas. "
                    + "Ejecute primero GenerateInfoFiles.");
        }
        for (File file : files) {
            processSalesFile(file);
        }
    }

    /**
     * Procesa un archivo de ventas: valida el encabezado y cada linea
     * ({@code IDProducto;Cantidad;}).
     *
     * @param file archivo de ventas
     */
    private static void processSalesFile(File file) {
        try (BufferedReader br = openReader(file)) {
            String header = br.readLine();
            String[] headerParts = header == null ? new String[0] : header.split(SEPARATOR);
            Long docNumber = null;
            if (headerParts.length == 2) {
                try {
                    docNumber = Long.parseLong(headerParts[1].trim());
                } catch (NumberFormatException e) {
                    docNumber = null;
                }
            }
            if (docNumber == null || !salesmanNames.containsKey(docNumber)) {
                warnings.add("Encabezado erroneo o vendedor desconocido en " + file.getName());
                return;
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    processSaleLine(line, docNumber, file.getName());
                }
            }
        } catch (IOException e) {
            warnings.add("No se pudo leer " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Procesa una venta y la suma al vendedor y al producto.
     *
     * @param line linea con formato {@code IDProducto;Cantidad;}
     * @param docNumber documento del vendedor
     * @param fileName nombre del archivo (para las advertencias)
     */
    private static void processSaleLine(String line, long docNumber, String fileName) {
        String[] parts = line.split(SEPARATOR);
        try {
            if (parts.length != 2) {
                throw new NumberFormatException("numero de campos incorrecto");
            }
            String productId = parts[0].trim();
            long quantity = Long.parseLong(parts[1].trim());
            if (!productPrices.containsKey(productId) || quantity < 0) {
                warnings.add("Venta incoherente (producto inexistente o cantidad negativa) en "
                        + fileName + ": " + line);
                return;
            }
            productQuantities.put(productId, productQuantities.get(productId) + quantity);
            salesmanTotals.put(docNumber,
                    salesmanTotals.get(docNumber) + quantity * productPrices.get(productId));
        } catch (NumberFormatException e) {
            warnings.add("Linea con formato erroneo en " + fileName + ": " + line);
        }
    }

    /**
     * Crea el reporte de vendedores (nombre;total) de mayor a menor recaudo.
     *
     * @param filename nombre del reporte
     * @throws IOException si no se puede escribir el archivo
     */
    private static void generateSalesmenReport(String filename) throws IOException {
        List<Map.Entry<Long, Long>> list = new ArrayList<>(salesmanTotals.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(Paths.get(filename), StandardCharsets.UTF_8))) {
            for (Map.Entry<Long, Long> entry : list) {
                writer.println(salesmanNames.get(entry.getKey()) + SEPARATOR + entry.getValue());
            }
        }
    }

    /**
     * Crea el reporte de productos vendidos (nombre;precio) de mayor a menor
     * cantidad vendida. Solo incluye productos con ventas.
     *
     * @param filename nombre del reporte
     * @throws IOException si no se puede escribir el archivo
     */
    private static void generateProductsReport(String filename) throws IOException {
        List<Map.Entry<String, Long>> list = new ArrayList<>();
        for (Map.Entry<String, Long> entry : productQuantities.entrySet()) {
            if (entry.getValue() > 0) {
                list.add(entry);
            }
        }
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(Paths.get(filename), StandardCharsets.UTF_8))) {
            for (Map.Entry<String, Long> entry : list) {
                String id = entry.getKey();
                writer.println(productNames.get(id) + SEPARATOR + productPrices.get(id));
            }
        }
    }
}
