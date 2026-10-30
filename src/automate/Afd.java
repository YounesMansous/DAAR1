package automate;

import java.util.*;

/**
 * Automate fini déterministe, produit par {@link Determinisation}
 * puis réduit par {@link Minimisation}. Chaque (état, symbole) mène
 * à au plus un état ; l'automate n'est pas forcément complet : une
 * transition absente signifie un échec.
 *
 * <p>L'alphabet contient les lettres de la regex, plus {@link #AUTRE}
 * si la regex contient un point. Il est conservé avec l'automate,
 * car c'est lui qui donne les colonnes de la minimisation et de la
 * table {@link AfdCompile}.</p>
 */
public class Afd implements AutomateI<Etat> {

    /**
     * Symbole représentant n'importe quel caractère absent
     * de l'alphabet de la regex (utilisé par le point « . »).
     */
    public static final String AUTRE = "AUTRE";

    private List<Etat> etats;
    private Map<Etat, Map<String, Etat>> transitions;
    private Etat initial;
    private Set<Etat> finaux;

    // Symboles de l'automate : lettres de la regex, plus AUTRE
    // si la regex contient un point
    private Set<String> alphabet;

    /** Crée un AFD vide, sans état initial et d'alphabet vide. */
    public Afd() {
        this.etats = new ArrayList<>();
        this.transitions = new HashMap<>();
        this.finaux = new HashSet<>();
        this.alphabet = new TreeSet<>();
    }

    @Override
    public void ajouterEtat(Etat etat) {
        if (!etats.contains(etat)) {
            etats.add(etat);
        }
    }

    /**
     * Ajoute une transition déterministe.
     *
     * @param transition la transition à ajouter
     * @throws IllegalArgumentException si c'est une ε-transition, ou
     *         si l'état source a déjà une autre destination pour ce
     *         symbole
     */
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

    /**
     * Transition depuis un état.
     *
     * @param source  état de départ
     * @param symbole symbole lu (lettre de l'alphabet ou {@link #AUTRE})
     * @return l'état atteint, ou {@code null} s'il n'y a pas de transition
     */
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

    /**
     * Renvoie l'alphabet de l'automate.
     *
     * @return les symboles littéraux de la regex, plus {@link #AUTRE}
     *         si la regex contient un point
     */
    public Set<String> getAlphabet() {
        return alphabet;
    }

    /**
     * Remplace l'alphabet de l'automate.
     *
     * @param alphabet le nouvel alphabet
     */
    public void setAlphabet(Set<String> alphabet) {
        this.alphabet = alphabet;
    }
}
