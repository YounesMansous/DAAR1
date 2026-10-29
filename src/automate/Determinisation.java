
package automate;

import java.util.*;

public class Determinisation {

    private final Afn afn;

    public Determinisation(Afn afn) {
        this.afn = afn;
    }

    // ==========================================
    // 1. FERMETURE EPSILON
    // ==========================================

    public Set<Etat> epsilonClosure(Set<Etat> etats) {

        Set<Etat> fermeture = new HashSet<>(etats);
        Deque<Etat> pile = new ArrayDeque<>(etats);

        while (!pile.isEmpty()) {

            Etat courant = pile.pop();

            // Recuperer directement les destinations EPSILON
            for (Etat destination :
                    afn.destinations(courant, "EPSILON")) {

                if (fermeture.add(destination)) {
                    pile.push(destination);
                }
            }
        }

        return fermeture;
    }

    // ==========================================
    // 2. MOVE
    // ==========================================

    public Set<Etat> move(Set<Etat> etats, String symbole) {

        Set<Etat> resultat = new HashSet<>();

        for (Etat etat : etats) {

            resultat.addAll(
                afn.destinations(etat, symbole)
            );
        }

        return resultat;
    }

    // ==========================================
    // 3. CONSTRUCTION DE L'AFD
    // ==========================================

    public Afd convertir() {

        Afd afd = new Afd();

        // Associer chaque ensemble AFN a un etat AFD
        Map<Set<Etat>, Etat> correspondance = new HashMap<>();

        Queue<Set<Etat>> file = new ArrayDeque<>();

        // ======================================
        // CONSTRUCTION DE L'ALPHABET
        // ======================================

        Set<String> alphabet = new HashSet<>();

        for (Map<String, Set<Etat>> sorties :
                afn.getTransitions().values()) {

            for (String symbole : sorties.keySet()) {

                if (!symbole.equals("EPSILON")) {
                    alphabet.add(symbole);
                }
            }
        }

        // ======================================
        // ETAT INITIAL DE L'AFD
        // ======================================

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

        // ======================================
        // EXPLORATION DES ENSEMBLES D'ETATS
        // ======================================

        while (!file.isEmpty()) {

            Set<Etat> courant = file.poll();
            Etat sourceAfd = correspondance.get(courant);

            for (String symbole : alphabet) {

                Set<Etat> destination =
                    epsilonClosure(move(courant, symbole));

                if (destination.isEmpty()) {
                    continue;
                }

                // Creer un etat si l'ensemble est nouveau
                if (!correspondance.containsKey(destination)) {

                    Etat nouvelEtat = new Etat();

                    afd.ajouterEtat(nouvelEtat);

                    correspondance.put(destination, nouvelEtat);
                    file.add(destination);

                    if (contientFinal(destination)) {
                        afd.ajouterEtatFinal(nouvelEtat);
                    }
                }

                // Ajouter la transition deterministe
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

    // ==========================================
    // 4. VERIFIER LES ETATS FINAUX
    // ==========================================

    private boolean contientFinal(Set<Etat> etats) {

        for (Etat etat : etats) {

            if (afn.getFinaux().contains(etat)) {
                return true;
            }
        }

        return false;
    }
}
