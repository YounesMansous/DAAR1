package automate;

import java.util.*;

/**
 * Automate fini non déterministe avec ε-transitions (AFN-ε),
 * construit à partir de l'AST par {@link AstToAfnVisitor}.
 * Un (état, symbole) peut mener à plusieurs états. Les symboles sont
 * les lettres de la regex, « . » pour le point et "EPSILON" pour
 * les ε-transitions.
 */
public class Afn implements AutomateI<Set<Etat>> {

    private List<Etat> etats;
    private Map<Etat, Map<String, Set<Etat>>> transitions;
    private Etat initial;
    private Set<Etat> finaux;

    /** Crée un AFN vide, sans état initial. */
    public Afn() {
        this.etats = new ArrayList<>();
        this.transitions = new HashMap<>();
        this.finaux = new HashSet<>();
    }

    @Override
    public void ajouterEtat(Etat etat) {
        if (!etats.contains(etat)) {
            etats.add(etat);
        }
    }

    @Override
    public void ajouterTransition(Transition transition) {

        transitions
            .computeIfAbsent(
                transition.getSource(),
                k -> new HashMap<>()
            )
            .computeIfAbsent(
                transition.getSymbole(),
                k -> new HashSet<>()
            )
            .add(transition.getDestination());
    }

    /**
     * Destinations depuis un état en lisant un symbole.
     *
     * @param source  état de départ
     * @param symbole lettre, « . » ou "EPSILON"
     * @return les états atteints (ensemble vide s'il n'y en a pas)
     */
    public Set<Etat> destinations(Etat source, String symbole) {
        return transitions
            .getOrDefault(source, Collections.emptyMap())
            .getOrDefault(symbole, Collections.emptySet());
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
    public Map<Etat, Map<String, Set<Etat>>> getTransitions() {
        return transitions;
    }

    @Override
    public Set<Etat> getFinaux() {
        return finaux;
    }
}
