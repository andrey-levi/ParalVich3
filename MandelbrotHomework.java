import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MandelbrotHomework {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int MAX_ITERATIONS = 256;

    private static final double X_MIN = -2.5;
    private static final double X_MAX = 1.0;
    private static final double Y_MIN = -1.0;
    private static final double Y_MAX = 1.0;

    public static void main(String[] args) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("Запуск расчета на " + numThreads + " потоках...");

        long startTime = System.currentTimeMillis();

        int[][] iterations = calculateMandelbrot(numThreads);

        long endTime = System.currentTimeMillis();
        System.out.println("Расчет завершен за " + (endTime - startTime) + " мс.");

        showImage(iterations);
    }

    /**
     * Многопоточный расчет множества Мандельброта.
     * Возвращает двумерный массив, где каждое значение — количество итераций до выхода за радиус 2.
     * Если значение равно MAX_ITERATIONS, точка принадлежит множеству.
     */
    private static int[][] calculateMandelbrot(int numThreads) {
        int[][] result = new int[HEIGHT][WIDTH];
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);

        int rowsPerThread = HEIGHT / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int startRow = i * rowsPerThread;
            int endRow = (i == numThreads - 1) ? HEIGHT : startRow + rowsPerThread;

            pool.submit(new MandelbrotWorker(startRow, endRow, result));
        }

        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                throw new RuntimeException("Расчет не завершился вовремя");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Поток был прерван", e);
        }

        return result;
    }

    private static class MandelbrotWorker implements Runnable {
        private final int startRow;
        private final int endRow;
        private final int[][] result;

        public MandelbrotWorker(int startRow, int endRow, int[][] result) {
            this.startRow = startRow;
            this.endRow = endRow;
            this.result = result;
        }

        @Override
        public void run() {
            for (int y = startRow; y < endRow; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    result[y][x] = computeIterations(x, y);
                }
            }
        }

        /**
         * Расчет количества итераций для конкретной точки (пикселя).
         * Формула: z = z^2 + c, где z и c — комплексные числа.
         */
        private int computeIterations(int px, int py) {
            double cx = X_MIN + (px / (double) WIDTH) * (X_MAX - X_MIN);
            double cy = Y_MIN + (py / (double) HEIGHT) * (Y_MAX - Y_MIN);

            double zx = 0.0;
            double zy = 0.0;
            int iterations = 0;

            while (zx * zx + zy * zy <= 4.0 && iterations < MAX_ITERATIONS) {
                double temp = zx * zx - zy * zy + cx;
                zy = 2.0 * zx * zy + cy;
                zx = temp;
                iterations++;
            }

            return iterations;
        }
    }

    private static void showImage(int[][] iterations) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int iter = iterations[y][x];
                int color;

                if (iter == MAX_ITERATIONS) {
                    color = Color.BLACK.getRGB();
                } else {
                    float hue = (iter % 30) / 30.0f;
                    color = Color.HSBtoRGB(hue, 0.8f, 1.0f);
                }
                image.setRGB(x, y, color);
            }
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Множество Мандельброта (Многопоточный расчет)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new JLabel(new ImageIcon(image)));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}