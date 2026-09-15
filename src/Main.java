import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    // Estructuras para almacenar la información leída
    private static Map<String, Double> productPrices = new HashMap<>();
    private static Map<String, String> productNames = new HashMap<>();
    private static Map<String, Integer> productQuantities = new HashMap<>();
    private static Map<Long, String> salesmanNames = new HashMap<>();
    private static Map<Long, Double> salesmanTotals = new HashMap<>();

    public static void main(String[] args) {
        // 1. Cargar catálogo de productos
        loadProducts("productos.csv");

        // 2. Cargar información de vendedores
        loadSalesmen("vendedores.csv");

        // 3. Procesar todos los archivos de ventas
        processSalesFiles();

        // 4. Generar los reportes finales
        generateSalesmenReport("reporte_vendedores.csv");
        generateProductsReport("reporte_productos.csv");

        System.out.println("Procesamiento completado. Reportes creados con exito.");
    }

    private static void loadProducts(String filename) {
        File file = new File(filename);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 3) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    double price = Double.parseDouble(parts[2].trim());
                    productPrices.put(id, price);
                    productNames.put(id, name);
                    productQuantities.put(id, 0);
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo productos: " + e.getMessage());
        }
    }

    private static void loadSalesmen(String filename) {
        File file = new File(filename);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 4) {
                    long docNumber = Long.parseLong(parts[1].trim());
                    String fullName = parts[2].trim() + " " + parts[3].trim();
                    salesmanNames.put(docNumber, fullName);
                    salesmanTotals.put(docNumber, 0.0);
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo vendedores: " + e.getMessage());
        }
    }

    private static void processSalesFiles() {
        File folder = new File(".");
        File[] files = folder.listFiles((dir, name) -> name.startsWith("vendedor_") && name.endsWith(".csv"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName();
            String docStr = name.replace("vendedor_", "").replace(".csv", "");
            try {
                long docNumber = Long.parseLong(docStr);
                double totalSalesman = 0.0;

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(";");
                        if (parts.length >= 2) {
                            String productId = parts[0].trim();
                            int quantity = Integer.parseInt(parts[1].trim());

                            if (productPrices.containsKey(productId)) {
                                double price = productPrices.get(productId);
                                totalSalesman += price * quantity;
                                productQuantities.put(productId, productQuantities.get(productId) + quantity);
                            }
                        }
                    }
                }
                salesmanTotals.put(docNumber, salesmanTotals.getOrDefault(docNumber, 0.0) + totalSalesman);
            } catch (NumberFormatException | IOException e) {
                System.err.println("Error procesando " + name + ": " + e.getMessage());
            }
        }
    }

    private static void generateSalesmenReport(String filename) {
        List<Map.Entry<Long, Double>> list = new ArrayList<>(salesmanTotals.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (Map.Entry<Long, Double> entry : list) {
                String name = salesmanNames.getOrDefault(entry.getKey(), "Desconocido");
                writer.println(name + ";" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("Error creando " + filename + ": " + e.getMessage());
        }
    }

    private static void generateProductsReport(String filename) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(productQuantities.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (Map.Entry<String, Integer> entry : list) {
                String id = entry.getKey();
                String name = productNames.getOrDefault(id, "Desconocido");
                double price = productPrices.getOrDefault(id, 0.0);
                writer.println(name + ";" + entry.getValue() + ";" + price);
            }
        } catch (IOException e) {
            System.err.println("Error creando " + filename + ": " + e.getMessage());
        }
    }
}
