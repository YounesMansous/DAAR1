package automate;

import java.util.*;

/**
 * Troisième étape du pipeline : transforme l'AFN-ε en AFD par la
 * construction des sous-ensembles.
 *
 * <p>Chaque état de l'AFD représente l'ensemble des états de l'AFN
 * où l'on peut se trouver après avoir lu un même mot. L'alphabet de
 * l'AFD est formé des lettres de la regex, plus {@link Afd#AUTRE} si
 * elle contient un point : AUTRE représente d'un coup tous les
 * caractères absents de la regex, qui se comportent tous de la même
 * façon (seul le point les accepte). Il n'y a donc jamais besoin de
 * retour arrière pour gérer le point.</p>
 *
 * <p>Complexité : avec q états dans l'AFN, t transitions et un
 * alphabet de taille s, chaque état de l'AFD coûte
 * O(s · (q + t)) (un {@code move} et une fermeture par symbole).
 * L'AFD peut avoir jusqu'à 2^q états dans le pire cas théorique ; sur
 * les motifs du banc, il en a quelques dizaines au plus.</p>
 */
public class Determinisation {

    private final Afn afn;

    /**
     * Prépare la déterminisation d'un AFN-ε.
     *
     * @param afn l'AFN-ε à déterminiser
     */
    public Determinisation(Afn afn) {
        this.afn = afn;
    }

    /**
     * Fermeture ε : tous les états atteignables depuis l'ensemble
     * donné en ne suivant que des ε-transitions (parcours en
     * profondeur avec une pile). Chaque état et chaque ε-transition
     * sont examinés au plus une fois : O(q + t).
     *
     * @param etats ensemble d'états de départ de l'AFN
     * @return ces états et tous ceux qu'ils atteignent par ε
     */
    public Set<Etat> epsilonClosure(Set<Etat> etats) {

        Set<Etat> fermeture = new HashSet<>(etats);
        Deque<Etat> pile = new ArrayDeque<>(etats);

        while (!pile.isEmpty()) {

            Etat courant = pile.pop();

            for (Etat destination :
                    afn.destinations(courant, "EPSILON")) {

                if (fermeture.add(destination)) {
                    pile.push(destination);
                }
            }
        }

        return fermeture;
    }

    /**
     * Calcule les états atteints depuis un ensemble d'états
     * en lisant un symbole.
     * Le point « . » accepte tous les caractères : ses destinations
     * sont ajoutées pour chaque symbole. Pour {@link Afd#AUTRE},
     * seules les destinations du point sont possibles.
     * Coût O(q + t).
     *
     * @param etats   ensemble d'états de départ de l'AFN
     * @param symbole symbole lu (lettre de l'alphabet ou AUTRE)
     * @return les états atteints, sans fermeture epsilon
     */
    public Set<Etat> move(Set<Etat> etats, String symbole) {

        Set<Etat> resultat = new HashSet<>();

        for (Etat etat : etats) {

            if (!symbole.equals(Afd.AUTRE)) {
                resultat.addAll(
                    afn.destinations(etat, symbole)
                );
            }

            resultat.addAll(
                afn.destinations(etat, ".")
            );
        }

        return resultat;
    }

    /**
     * Construit l'AFD. On part de la fermeture ε de l'état initial ;
     * pour chaque ensemble S encore à traiter (file) et chaque
     * symbole a, l'ensemble fermeture(move(S, a)) devient un état de
     * l'AFD s'il est nouveau. Un ensemble vide ne donne pas d'état
     * (pas de puits) : la transition est simplement absente. Un état
     * de l'AFD est final si son ensemble contient un état final de
     * l'AFN.
     *
     * @return l'AFD, avec son alphabet
     */
    public Afd convertir() {

        Afd afd = new Afd();

        // Associer chaque ensemble AFN a un etat AFD
        Map<Set<Etat>, Etat> correspondance = new HashMap<>();

        Queue<Set<Etat>> file = new ArrayDeque<>();

        // Lettres de la regex, plus AUTRE pour les caracteres
        // qui n'y figurent pas mais que le point accepte
        Set<String> alphabet = new TreeSet<>();

        for (Map<String, Set<Etat>> sorties :
                afn.getTransitions().values()) {

            for (String symbole : sorties.keySet()) {

                if (symbole.equals(".")) {
                    alphabet.add(Afd.AUTRE);
                }
                else if (!symbole.equals("EPSILON")) {
                    alphabet.add(symbole);
                }
            }
        }

        afd.setAlphabet(alphabet);

        Set<Etat> depart = epsilonClosure(
            Set.of(afn.getInitial())
        );

        Etat initialAfd = new Etat();

        afd.ajouterEtat(initialAfd);
        afd.setInitial(initialAfd);

        correspondance.put(depart, initialAfd);
        file.add(depart);

        if (contientFinal(depart)) {
            afd.ajouterEtatFinal(initialAfd);
        }

        while (!file.isEmpty()) {

            Set<Etat> courant = file.poll();
            Etat sourceAfd = correspondance.get(courant);

            for (String symbole : alphabet) {

                Set<Etat> destination =
                    epsilonClosure(move(courant, symbole));

                if (destination.isEmpty()) {
                    continue;
                }

                if (!correspondance.containsKey(destination)) {

                    Etat nouvelEtat = new Etat();

                    afd.ajouterEtat(nouvelEtat);

                    correspondance.put(destination, nouvelEtat);
                    file.add(destination);

                    if (contientFinal(destination)) {
                        afd.ajouterEtatFinal(nouvelEtat);
                    }
                }

                afd.ajouterTransition(
                    new Transition(
                        sourceAfd,
                        correspondance.get(destination),
                        symbole
                    )
                );
            }
        }

        return afd;
    }

    // Vrai si l'ensemble contient un etat final de l'AFN
    private boolean contientFinal(Set<Etat> etats) {

        for (Etat etat : etats) {

            if (afn.getFinaux().contains(etat)) {
                return true;
            }
        }

        return false;
    }
}
