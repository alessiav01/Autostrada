import it.autostrada.casello.Casello;

public class MainCasello {
    public static void main(String[] args) throws Exception {
        // id=1, nomeCasello="Casello1", automatico=false (manuale), entrata=true (ingresso)
        Casello casello = new Casello(1, "Casello1", false, true);
        casello.connect();

        // avvia manualmente il flusso, come se un veicolo si fosse presentato
        casello.gestisciIngressoManuale();

    }
}