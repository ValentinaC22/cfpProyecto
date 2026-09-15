import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class GenerateInfoFiles {

    public static void main(String[] args) {
        // Generar archivos de prueba
        createProductsFile(10);
        createSalesManInfoFile(5);
        createSalesMenFiles(5, 10);
        System.out.println("Archivos CSV de prueba generados exitosamente.");
    }

    // Crea el archivo de productos (ID;Nombre;Precio)
    public static void createProductsFile(int productsCount) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("productos.csv"))) {
            for (int i = 1; i <= productsCount; i++) {
                String id = "P" + i;
                String name = "Producto_" + i;
                double price = 1000 + (i * 500);
                writer.println(id + ";" + name + ";" + price);
            }
        } catch (IOException e) {
            System.err.println("Error creando productos.csv: " + e.getMessage());
        }
    }

    // Crea el archivo de vendedores (TipoDoc;Documento;Nombres;Apellidos)
    public static void createSalesManInfoFile(int salesmanCount) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("vendedores.csv"))) {
            for (int i = 1; i <= salesmanCount; i++) {
                String docType = "CC";
                long docNumber = 1000000000L + i;
                String name = "Vendedor" + i;
                String lastName = "Apellido" + i;
                writer.println(docType + ";" + docNumber + ";" + name + ";" + lastName);
            }
        } catch (IOException e) {
            System.err.println("Error creando vendedores.csv: " + e.getMessage());
        }
    }

    // Crea los archivos de ventas de cada vendedor (IDProducto;Cantidad)
    public static void createSalesMenFiles(int salesmanCount, int productsCount) {
        Random random = new Random();
        for (int i = 1; i <= salesmanCount; i++) {
            long docNumber = 1000000000L + i;
            String fileName = "vendedor_" + docNumber + ".csv";
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                // Registrar entre 3 y 8 ventas por vendedor
                int salesCount = 3 + random.nextInt(6);
                for (int j = 0; j < salesCount; j++) {
                    String productId = "P" + (1 + random.nextInt(productsCount));
                    int quantity = 1 + random.nextInt(10);
                    writer.println(productId + ";" + quantity);
                }
            } catch (IOException e) {
                System.err.println("Error creando " + fileName + ": " + e.getMessage());
            }
        }
    }
}