import it.autostrada.casello.Casello;

public class MainCaselloIngressoTelepass {
    public static void main(String[] args) throws Exception {
        // id=2, automatico=true (telepass), entrata=true (ingresso)
        Casello casello = new Casello(2, "Casello2-IngressoTelepass", true, true);
        casello.connect();

        casello.gestisciIngressoAutomatico("TP-0012345"); // ID trasmettitore simulato
    }
}