
package automate;

import java.util.*;

public class Afd implements AutomateI<Etat> {

    private List<Etat> etats;
    private Map<Etat, Map<String, Etat>> transitions;
    private Etat initial;
    private Set<Etat> finaux;

    public Afd() {
        this.etats = new ArrayList<>();
        this.transitions = new HashMap<>();
        this.finaux = new HashSet<>();
    }

    public Afd(Etat initial) {
        this();
        this.initial = initial;
        ajouterEtat(initial);
    }

    @Override
    public void ajouterEtat(Etat etat) {
        if (!etats.contains(etat)) {
            etats.add(etat);
        }
    }

    @Override
    public void ajouterTransition(Transition transition) {

        String symbole = transition.getSymbole();

        if ("EPSILON".equals(symbole)) {
            throw new IllegalArgumentException(
                "Transition epsilon interdite dans un AFD");
        }

        Etat source = transition.getSource();
        Etat destination = transition.getDestination();

        Map<String, Etat> sorties = transitions
            .computeIfAbsent(source, k -> new HashMap<>());

        Etat existante = sorties.get(symbole);

        if (existante != null && existante != destination) {
            throw new IllegalArgumentException(
                "Transition non deterministe sur " + symbole);
        }

        sorties.put(symbole, destination);
    }

    public Etat transition(Etat source, String symbole) {
        return transitions
            .getOrDefault(source, Collections.emptyMap())
            .get(symbole);
    }

    @Override
    public void ajouterEtatFinal(Etat etat) {
        finaux.add(etat);
    }

    @Override
    public Etat getInitial() {
        return initial;
    }

    @Override
    public void setInitial(Etat initial) {
        this.initial = initial;
    }

    @Override
    public List<Etat> getEtats() {
        return etats;
    }

    @Override
    public Map<Etat, Map<String, Etat>> getTransitions() {
        return transitions;
    }

    @Override
    public Set<Etat> getFinaux() {
        return finaux;
    }
}
