package com.project;

// Importaciones necesarias para hilos, pools y semáforos
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        // Creamos un aparcamiento con una capacidad limitada de 3 plazas
        ParkingLot parking = new ParkingLot(3);

        // MOTOR DE EJECUCIÓN (POOL DE HILOS)
        // Usamos un pool de 6 hilos para simular la llegada simultánea de 6 coches
        ExecutorService executor = Executors.newFixedThreadPool(6);

        System.out.println("[SISTEMA] Apertura del aparcamiento. Plazas totales: 3\n");

        // Creamos y lanzamos 6 tareas concurrentes (una para cada coche)
        for (int i = 1; i <= 6; i++) {
            final int cocheId = i;
            
            // Definimos la tarea Runnable que simula el ciclo de vida del coche
            Runnable tascaCotxe = () -> {
                try {
                    // El coche intenta acceder al aparcamiento
                    parking.entrar(cocheId);
                    
                    // Simula el tiempo que el coche pasa aparcado dentro (entre 1 y 3 segundos)
                    long tempsAparcat = (long) (Math.random() * 2000 + 1000);
                    Thread.sleep(tempsAparcat);
                    
                    // El coche abandona el aparcamiento liberando su sitio
                    parking.sortir(cocheId);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            // Enviamos el coche al pool para que actúe de forma concurrente
            executor.execute(tascaCotxe);
        }

        // CIERRE CONTROLADO DEL EXECUTOR
        // Apagamos el administrador de hilos para que no admita nuevas peticiones
        executor.shutdown();
        try {
            // Damos un margen de 15 segundos para que todos los coches terminen de salir
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            System.out.println("\n[SISTEMA] Aparcamiento cerrado y recursos liberados.");
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    // =======================================================================
    // CLASE INTERNA PARKINGLOT
    // Gestiona de manera segura y concurrente el acceso mediante un Semaphore
    // =======================================================================
    static class ParkingLot {
        // El semáforo controlará que no se supere la capacidad máxima establecida
        private final Semaphore semafor;

        public ParkingLot(int capacitat) {
            // Inicializamos el semáforo con los permisos equivalentes a las plazas totales
            // El parámetro 'true' garantiza equidad (Fairness): los coches entran por orden de llegada
            this.semafor = new Semaphore(capacitat, true);
        }

        // Método para gestionar la entrada de un coche
        public void entrar(int cocheId) throws InterruptedException {
            // Comprobamos de manera no bloqueante si hay plazas libres antes de adquirir el permiso
            // Sirve únicamente para lanzar el mensaje informativo de que el coche se queda esperando
            if (semafor.availablePermits() == 0) {
                System.out.println("❌ [Cotxe " + cocheId + "] L'aparcament està plen. S'espera a la cua...");
            }

            // El hilo intenta adquirir un permiso. Si no hay, se congela aquí de forma segura
            semafor.acquire();
            
            // Si pasa de esta línea, significa que ha conseguido plaza con éxito
            System.out.println("🚗 [Cotxe " + cocheId + "] Ha entrat correctament. Places lliures: " + semafor.availablePermits());
        }

        // Método para gestionar la salida de un coche
        public void sortir(int cocheId) {
            // Devolvemos el permiso al semáforo, incrementando el contador de plazas libres
            semafor.release();
            System.out.println("💨 [Cotxe " + cocheId + "] Ha sortit de l'aparcament. Places lliures: " + semafor.availablePermits());
        }
    }
}
