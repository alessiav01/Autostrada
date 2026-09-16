package it.autostrada.launcher;

import it.autostrada.casello.Casello;
import it.autostrada.sbarra.Sbarra;
import it.autostrada.server.Server;
import it.autostrada.telecamera.Telecamera;

import java.util.Scanner;

public class MainLauncher {

    private static Server server;
    private static Telecamera telecamera;
    private static Sbarra sbarra;
    private static it.autostrada.rest.RestServer restServer;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            stampaMenu();
            String scelta = scanner.nextLine().trim();

            switch (scelta) {
                case "1" -> avviaServer();
                case "2" -> avviaTelecamera();
                case "3" -> avviaSbarra();
                case "4" -> simulaIngressoManuale(scanner);
                case "5" -> simulaUscitaManuale(scanner);
                case "6" -> simulaIngressoTelepass(scanner);
                case "7" -> simulaUscitaTelepass(scanner);
                case "8" -> avviaRestServer();
                case "0" -> running = false;
                default -> System.out.println("Scelta non valida.");
            }
        }
        System.out.println("Uscita dal simulatore.");
    }

    private static void stampaMenu() {
        System.out.println("\n=== PISSIR - Simulatore Autostrada ===");
        System.out.println("Infrastruttura (avviare una volta sola):");
        System.out.println("  1. Avvia it.autostrada.server.Server Centrale" + (server != null ? " [ATTIVO]" : ""));
        System.out.println("  2. Avvia it.autostrada.telecamera.Telecamera" + (telecamera != null ? " [ATTIVA]" : ""));
        System.out.println("  3. Avvia it.autostrada.sbarra.Sbarra" + (sbarra != null ? " [ATTIVA]" : ""));
        System.out.println("Simulazioni it.autostrada.casello.Casello:");
        System.out.println("  4. Simula Ingresso Manuale");
        System.out.println("  5. Simula Uscita Manuale");
        System.out.println("  6. Simula Ingresso Telepass");
        System.out.println("  7. Simula Uscita Telepass");
        System.out.println("  8. Avvia Backend REST" + (restServer != null ? " [ATTIVO]" : ""));
        System.out.println("  0. Esci");
        System.out.print("Scelta: ");
    }

    private static void avviaServer() throws Exception {
        if (server != null) {
            System.out.println("it.autostrada.server.Server già avviato.");
            return;
        }
        server = new Server();
        server.connect();
    }

    private static void avviaTelecamera() throws Exception {
        if (telecamera != null) {
            System.out.println("it.autostrada.telecamera.Telecamera già avviata.");
            return;
        }
        telecamera = new Telecamera();
        telecamera.connect();
    }

    private static void avviaSbarra() throws Exception {
        if (sbarra != null) {
            System.out.println("it.autostrada.sbarra.Sbarra già avviata.");
            return;
        }
        sbarra = new Sbarra();
        sbarra.connect();
    }

    private static int chiediIdCasello(Scanner scanner) {
        System.out.print("ID casello (1=AL Est, 2=AL Ovest, 5=TO, ...): ");
        return Integer.parseInt(scanner.nextLine().trim());
    }

    private static void simulaIngressoManuale(Scanner scanner) throws Exception {
        int id = chiediIdCasello(scanner);
        Casello c = new Casello(id, "it.autostrada.casello.Casello" + id + "-Ingresso", false, true);
        c.connect();
        c.gestisciIngressoManuale();
        c.attendiCompletamento();
    }

    private static void simulaUscitaManuale(Scanner scanner) throws Exception {
        int id = chiediIdCasello(scanner);
        System.out.print("Matricola/codice del biglietto inserito dal guidatore: ");
        String codiceBiglietto = scanner.nextLine().trim();

        Casello c = new Casello(id, "Casello" + id + "-Uscita", false, false);
        c.connect();
        c.gestisciUscitaManuale(codiceBiglietto);
        c.attendiPedaggio();

        System.out.println("Premi INVIO quando vuoi inviare il pagamento...");
        scanner.nextLine();
        c.eseguiPagamento(c.getTargaCorrente(), c.getPedaggioCorrente());
        c.attendiCompletamento();
    }

    private static void simulaIngressoTelepass(Scanner scanner) throws Exception {
        int id = chiediIdCasello(scanner);
        System.out.print("ID trasmettitore Telepass (INVIO per default TP-0012345): ");
        String idTrasmettitore = scanner.nextLine().trim();
        if (idTrasmettitore.isEmpty()) idTrasmettitore = "TP-0012345";

        Casello c = new Casello(id, "it.autostrada.casello.Casello" + id + "-IngressoTelepass", true, true);
        c.connect();
        c.gestisciIngressoAutomatico(idTrasmettitore);
        c.attendiCompletamento();
    }

    private static void simulaUscitaTelepass(Scanner scanner) throws Exception {
        int id = chiediIdCasello(scanner);
        System.out.print("ID trasmettitore Telepass (INVIO per default TP-0012345): ");
        String idTrasmettitore = scanner.nextLine().trim();
        if (idTrasmettitore.isEmpty()) idTrasmettitore = "TP-0012345";

        Casello c = new Casello(id, "it.autostrada.casello.Casello" + id + "-UscitaTelepass", true, false);
        c.connect();
        c.gestisciUscitaAutomatico(idTrasmettitore);
        c.attendiCompletamento();
    }

    private static void avviaRestServer() {
        if (restServer != null) {
            System.out.println("Backend REST già avviato.");
            return;
        }
        restServer = new it.autostrada.rest.RestServer();
        restServer.start(7000);
    }
}