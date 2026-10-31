package automate;

public class Transition{
    private Etat source;
    private Etat destination;
    private String symbole;

    public Transition(Etat source, Etat destination, String symbole){
        this.source = source;
        this.destination = destination;
        this.symbole = symbole;
    }

    public String getSymbole() {
        return symbole;
    }

    public Etat getSource(){
        return source;
    }

    public Etat getDestination(){
        return destination;
    }
}
