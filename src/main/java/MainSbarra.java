import it.autostrada.sbarra.Sbarra;

public class MainSbarra {
    public static void main(String[] args) throws Exception {
        Sbarra sbarra = new Sbarra();
        sbarra.connect();
        System.out.println("it.autostrada.sbarra.Sbarra pronta e in ascolto su tutti i caselli...");
    }
}