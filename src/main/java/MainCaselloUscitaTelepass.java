import it.autostrada.casello.Casello;

public class MainCaselloUscitaTelepass {
    public static void main(String[] args) throws Exception {
        // id=2, automatico=true (telepass), entrata=false (uscita)
        Casello casello = new Casello(2, "Casello2-UscitaTelepass", true, false);
        casello.connect();

        casello.gestisciUscitaAutomatico("TP-0012345"); // stesso ID di ingresso, per coerenza
    }
}