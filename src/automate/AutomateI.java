package automate;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Opérations communes à l'AFN-ε ({@link Afn}) et à l'AFD
 * ({@link Afd}), utilisées pour les construire et pour les afficher
 * avec --debug.
 *
 * @param <T> ce qu'un symbole atteint depuis un état : un ensemble
 *            d'états pour l'AFN, un seul état pour l'AFD
 */
public interface AutomateI<T> {

    /**
     * Ajoute un état, s'il n'y est pas déjà.
     *
     * @param etat l'état à ajouter
     */
    void ajouterEtat(Etat etat);

    /**
     * Ajoute une transition.
     *
     * @param transition la transition à ajouter
     */
    void ajouterTransition(Transition transition);

    /**
     * Marque un état comme final.
     *
     * @param etat l'état final
     */
    void ajouterEtatFinal(Etat etat);

    /**
     * Renvoie l'état initial.
     *
     * @return l'état initial
     */
    Etat getInitial();

    /**
     * Change l'état initial.
     *
     * @param initial le nouvel état initial
     */
    void setInitial(Etat initial);

    /**
     * Renvoie les états, dans l'ordre d'ajout.
     *
     * @return les états, dans l'ordre d'ajout
     */
    List<Etat> getEtats();

    /**
     * Renvoie les transitions, indexées par état source puis par
     * symbole.
     *
     * @return pour chaque état source et chaque symbole, la ou les
     *         destinations
     */
    Map<Etat, Map<String, T>> getTransitions();

    /**
     * Renvoie les états finaux.
     *
     * @return les états finaux
     */
    Set<Etat> getFinaux();
}
