import it.autostrada.telecamera.Telecamera;

public class MainTelecamera {
    public static void main(String[] args) throws Exception {
        Telecamera telecamera = new Telecamera();
        telecamera.connect();
        System.out.println("it.autostrada.telecamera.Telecamera pronta e in ascolto su tutti i caselli...");
    }
}