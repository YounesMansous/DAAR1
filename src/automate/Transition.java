package automate;

/**
 * Transition d'un automate : source, destination et symbole lu.
 * Le symbole est une lettre, « . » (point, dans l'AFN), "EPSILON"
 * (ε-transition, dans l'AFN) ou {@link Afd#AUTRE} (dans l'AFD).
 */
public class Transition{
    private Etat source;
    private Etat destination;
    private String symbole;

    /**
     * Crée une transition.
     *
     * @param source      état de départ
     * @param destination état d'arrivée
     * @param symbole     symbole lu
     */
    public Transition(Etat source, Etat destination, String symbole){
        this.source = source;
        this.destination = destination;
        this.symbole = symbole;
    }

    /**
     * Renvoie le symbole lu.
     *
     * @return le symbole lu
     */
    public String getSymbole() {
        return symbole;
    }

    /**
     * Renvoie l'état de départ.
     *
     * @return l'état de départ
     */
    public Etat getSource(){
        return source;
    }

    /**
     * Renvoie l'état d'arrivée.
     *
     * @return l'état d'arrivée
     */
    public Etat getDestination(){
        return destination;
    }
}
