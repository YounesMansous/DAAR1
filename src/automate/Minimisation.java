
package automate;

import java.util.*;

public class Minimisation {

    private final Afd afd;

    public Minimisation(Afd afd) {
        this.afd = afd;
    }

    // ==========================================
    // 1. RECUPERER L'ALPHABET
    // ==========================================

    // L'alphabet est celui calcule par la determinisation
    // (il contient AUTRE si la regex a un point)
    private Set<String> getAlphabet(Afd automate) {
        return automate.getAlphabet();
    }

    // ==========================================
    // 2. RECUPERER UNE TRANSITION
    // ==========================================

    private Etat transition(
            Afd automate,
            Etat source,
            String symbole) {

        return automate.transition(source, symbole);
    }

    // ==========================================
    // 3. SUPPRIMER LES ETATS INACCESSIBLES
    // ==========================================

    private Afd supprimerInaccessibles() {

        if (afd.getInitial() == null) {
            throw new IllegalStateException(
                "L'AFD n'a pas d'etat initial"
            );
        }

        Set<Etat> accessibles = new HashSet<>();
        Queue<Etat> file = new ArrayDeque<>();

        accessibles.add(afd.getInitial());
        file.add(afd.getInitial());

        while (!file.isEmpty()) {

            Etat courant = file.poll();

            Map<String, Etat> sorties =
                afd.getTransitions().getOrDefault(
                    courant,
                    Collections.emptyMap()
                );

            for (Etat destination : sorties.values()) {

                if (accessibles.add(destination)) {
                    file.add(destination);
                }
            }
        }

        Afd resultat = new Afd();
        resultat.setAlphabet(afd.getAlphabet());

        for (Etat etat : afd.getEtats()) {

            if (accessibles.contains(etat)) {

                resultat.ajouterEtat(etat);

                if (afd.getFinaux().contains(etat)) {
                    resultat.ajouterEtatFinal(etat);
                }
            }
        }

        resultat.setInitial(afd.getInitial());

        // Copier uniquement les transitions accessibles
        for (Map.Entry<Etat, Map<String, Etat>> entree :
                afd.getTransitions().entrySet()) {

            Etat source = entree.getKey();

            if (!accessibles.contains(source)) {
                continue;
            }

            for (Map.Entry<String, Etat> sortie :
                    entree.getValue().entrySet()) {

                Etat destination = sortie.getValue();

                if (accessibles.contains(destination)) {

                    resultat.ajouterTransition(
                        new Transition(
                            source,
                            destination,
                            sortie.getKey()
                        )
                    );
                }
            }
        }

        return resultat;
    }

    // ==========================================
    // 4. COMPLETER L'AUTOMATE
    // ==========================================

    private Afd completerAutomate(Afd automate) {

        Set<String> alphabet = getAlphabet(automate);

        Afd resultat = new Afd();
        resultat.setAlphabet(alphabet);

        // Copier les etats
        for (Etat etat : automate.getEtats()) {
            resultat.ajouterEtat(etat);
        }

        // Copier les etats finaux
        for (Etat etat : automate.getFinaux()) {
            resultat.ajouterEtatFinal(etat);
        }

        resultat.setInitial(automate.getInitial());

        // Copier les transitions
        for (Map.Entry<Etat, Map<String, Etat>> entree :
                automate.getTransitions().entrySet()) {

            Etat source = entree.getKey();

            for (Map.Entry<String, Etat> sortie :
                    entree.getValue().entrySet()) {

                resultat.ajouterTransition(
                    new Transition(
                        source,
                        sortie.getValue(),
                        sortie.getKey()
                    )
                );
            }
        }

        Etat puits = null;

        // Completer les transitions manquantes
        for (Etat etat : automate.getEtats()) {

            for (String symbole : alphabet) {

                if (transition(automate, etat, symbole) == null) {

                    if (puits == null) {
                        puits = new Etat();
                        resultat.ajouterEtat(puits);
                    }

                    resultat.ajouterTransition(
                        new Transition(etat, puits, symbole)
                    );
                }
            }
        }

        // Le puits boucle sur tous les symboles
        if (puits != null) {

            for (String symbole : alphabet) {

                resultat.ajouterTransition(
                    new Transition(puits, puits, symbole)
                );
            }
        }

        return resultat;
    }

    // ==========================================
    // 5. PARTITIONS INITIALES
    // ==========================================

    private List<Set<Etat>> partitionsInitiales(Afd automate) {

        Set<Etat> finaux = new LinkedHashSet<>();
        Set<Etat> nonFinaux = new LinkedHashSet<>();

        for (Etat etat : automate.getEtats()) {

            if (automate.getFinaux().contains(etat)) {
                finaux.add(etat);
            } else {
                nonFinaux.add(etat);
            }
        }

        List<Set<Etat>> partitions = new ArrayList<>();

        if (!finaux.isEmpty()) {
            partitions.add(finaux);
        }

        if (!nonFinaux.isEmpty()) {
            partitions.add(nonFinaux);
        }

        return partitions;
    }

    // ==========================================
    // 6. INDICE D'UNE PARTITION
    // ==========================================

    private int indicePartition(
            Etat etat,
            List<Set<Etat>> partitions) {

        for (int i = 0; i < partitions.size(); i++) {

            if (partitions.get(i).contains(etat)) {
                return i;
            }
        }

        throw new IllegalStateException(
            "Etat absent des partitions"
        );
    }

    // ==========================================
    // 7. RAFFINER LES PARTITIONS
    // ==========================================

    private List<Set<Etat>> raffinerPartitions(
            Afd automate,
            List<Set<Etat>> partitions) {

        Set<String> alphabet = getAlphabet(automate);

        List<Set<Etat>> nouvelles = new ArrayList<>();

        for (Set<Etat> groupe : partitions) {

            // Une signature indique les partitions atteintes
            Map<List<Integer>, Set<Etat>> sousGroupes =
                new LinkedHashMap<>();

            for (Etat etat : groupe) {

                List<Integer> signature = new ArrayList<>();

                for (String symbole : alphabet) {

                    Etat destination =
                        transition(automate, etat, symbole);

                    int indice =
                        indicePartition(destination, partitions);

                    signature.add(indice);
                }

                sousGroupes
                    .computeIfAbsent(
                        signature,
                        k -> new LinkedHashSet<>()
                    )
                    .add(etat);
            }

            nouvelles.addAll(sousGroupes.values());
        }

        return nouvelles;
    }

    // ==========================================
    // 8. CONSTRUIRE L'AFD MINIMAL
    // ==========================================

    private Afd construireAfdMinimal(
            Afd automate,
            List<Set<Etat>> partitions) {

        Afd minimal = new Afd();
        minimal.setAlphabet(getAlphabet(automate));

        Map<Etat, Etat> correspondance = new HashMap<>();

        // Un nouvel etat par groupe
        for (Set<Etat> groupe : partitions) {

            Etat nouvelEtat = new Etat();

            minimal.ajouterEtat(nouvelEtat);

            for (Etat ancien : groupe) {

                correspondance.put(ancien, nouvelEtat);

                if (automate.getFinaux().contains(ancien)) {
                    minimal.ajouterEtatFinal(nouvelEtat);
                }
            }

            if (groupe.contains(automate.getInitial())) {
                minimal.setInitial(nouvelEtat);
            }
        }

        Set<String> alphabet = getAlphabet(automate);

        // Construire les nouvelles transitions
        for (Set<Etat> groupe : partitions) {

            Etat representant = groupe.iterator().next();

            Etat source =
                correspondance.get(representant);

            for (String symbole : alphabet) {

                Etat ancienneDestination =
                    transition(automate, representant, symbole);

                Etat nouvelleDestination =
                    correspondance.get(ancienneDestination);

                minimal.ajouterTransition(
                    new Transition(
                        source,
                        nouvelleDestination,
                        symbole
                    )
                );
            }
        }

        return minimal;
    }

    // ==========================================
    // 9. METHODE PRINCIPALE
    // ==========================================

    /**
     * Minimise l'AFD (algorithme de Moore), puis retire l'état puits.
     * L'automate renvoyé n'est donc pas complet : une transition
     * absente signifie qu'aucun état final n'est plus atteignable.
     *
     * @return l'AFD minimal, sans état puits
     */
    public Afd minimiser() {

        Afd accessible = supprimerInaccessibles();

        Afd complet = completerAutomate(accessible);

        List<Set<Etat>> partitions =
            partitionsInitiales(complet);

        while (true) {

            List<Set<Etat>> nouvelles =
                raffinerPartitions(complet, partitions);

            if (nouvelles.equals(partitions)) {
                break;
            }

            partitions = nouvelles;
        }

        return retirerPuits(construireAfdMinimal(complet, partitions));
    }

    // ==========================================
    // 10. RETIRER L'ETAT PUITS
    // ==========================================

    // Garde seulement les etats depuis lesquels un etat final est
    // atteignable (dans un AFD minimal, seul le puits ne l'est pas),
    // et les transitions entre ces etats. L'etat initial est toujours
    // garde pour que l'automate reste utilisable.
    private Afd retirerPuits(Afd automate) {

        // Predecesseurs de chaque etat
        Map<Etat, List<Etat>> predecesseurs = new HashMap<>();

        for (Map.Entry<Etat, Map<String, Etat>> entree :
                automate.getTransitions().entrySet()) {

            for (Etat destination : entree.getValue().values()) {

                predecesseurs
                    .computeIfAbsent(destination, k -> new ArrayList<>())
                    .add(entree.getKey());
            }
        }

        // Parcours a l'envers depuis les etats finaux
        Set<Etat> utiles = new HashSet<>(automate.getFinaux());
        Deque<Etat> file = new ArrayDeque<>(automate.getFinaux());

        while (!file.isEmpty()) {

            Etat courant = file.poll();

            for (Etat precedent : predecesseurs.getOrDefault(
                    courant, Collections.emptyList())) {

                if (utiles.add(precedent)) {
                    file.add(precedent);
                }
            }
        }

        utiles.add(automate.getInitial());

        Afd resultat = new Afd();
        resultat.setAlphabet(automate.getAlphabet());

        for (Etat etat : automate.getEtats()) {

            if (utiles.contains(etat)) {

                resultat.ajouterEtat(etat);

                if (automate.getFinaux().contains(etat)) {
                    resultat.ajouterEtatFinal(etat);
                }
            }
        }

        resultat.setInitial(automate.getInitial());

        for (Map.Entry<Etat, Map<String, Etat>> entree :
                automate.getTransitions().entrySet()) {

            Etat source = entree.getKey();

            if (!utiles.contains(source)) {
                continue;
            }

            for (Map.Entry<String, Etat> sortie :
                    entree.getValue().entrySet()) {

                if (utiles.contains(sortie.getValue())) {

                    resultat.ajouterTransition(
                        new Transition(
                            source,
                            sortie.getValue(),
                            sortie.getKey()
                        )
                    );
                }
            }
        }

        return resultat;
    }
}
