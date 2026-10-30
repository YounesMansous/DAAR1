package automate;

import java.util.*;

/**
 * Quatrième étape du pipeline : minimise l'AFD par l'algorithme de
 * Moore, puis retire l'état puits.
 *
 * <p>Étapes de {@link #minimiser()} :</p>
 * <ol>
 *   <li>suppression des états inaccessibles depuis l'initial ;</li>
 *   <li>complétion par un état puits, pour que chaque état ait une
 *       transition sur chaque symbole de l'alphabet ;</li>
 *   <li>raffinement de Moore jusqu'au point fixe ;</li>
 *   <li>construction de l'AFD quotient (un état par classe) ;</li>
 *   <li>retrait du puits.</li>
 * </ol>
 *
 * <p>Complexité, avec n états et un alphabet de taille s : chaque
 * tour de raffinement coûte O(n · s · k), où k est le nombre de
 * classes (la classe d'un état est cherchée en parcourant la liste
 * des classes). Il y a au plus n tours, d'où O(n³ · s) dans le pire
 * cas ; les AFD du projet ont rarement plus de quelques dizaines
 * d'états.</p>
 */
public class Minimisation {

    private final Afd afd;

    /**
     * Prépare la minimisation d'un AFD.
     *
     * @param afd l'AFD à minimiser ; il n'est pas modifié
     */
    public Minimisation(Afd afd) {
        this.afd = afd;
    }

    // Parcours en largeur depuis l'etat initial, en O(n · s) ;
    // renvoie une copie de l'AFD reduite aux etats atteints
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

    // Copie de l'automate ou chaque transition manquante va vers un
    // etat puits unique, qui boucle sur lui-meme. Moore a besoin d'un
    // automate complet : sinon une transition absente n'aurait pas de
    // classe d'arrivee a comparer.
    private Afd completerAutomate(Afd automate) {

        Set<String> alphabet = automate.getAlphabet();

        Afd resultat = new Afd();
        resultat.setAlphabet(alphabet);

        for (Etat etat : automate.getEtats()) {
            resultat.ajouterEtat(etat);
        }

        for (Etat etat : automate.getFinaux()) {
            resultat.ajouterEtatFinal(etat);
        }

        resultat.setInitial(automate.getInitial());

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

        for (Etat etat : automate.getEtats()) {

            for (String symbole : alphabet) {

                if (automate.transition(etat, symbole) == null) {

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

        if (puits != null) {

            for (String symbole : alphabet) {

                resultat.ajouterTransition(
                    new Transition(puits, puits, symbole)
                );
            }
        }

        return resultat;
    }

    // Partition de depart de Moore : finaux d'un cote, non finaux de
    // l'autre (sans classe vide)
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

    // Indice de la classe qui contient l'etat, en O(k)
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

    // Un tour de Moore : deux etats d'une meme classe restent ensemble
    // si et seulement si, pour chaque symbole, ils vont dans la meme
    // classe. La signature d'un etat est la liste de ces classes
    // d'arrivee (dans l'ordre de l'alphabet) ; chaque classe est
    // decoupee selon les signatures. Les LinkedHashMap gardent un
    // ordre stable, ce qui permet de detecter le point fixe par
    // simple egalite des listes.
    private List<Set<Etat>> raffinerPartitions(
            Afd automate,
            List<Set<Etat>> partitions) {

        Set<String> alphabet = automate.getAlphabet();

        List<Set<Etat>> nouvelles = new ArrayList<>();

        for (Set<Etat> groupe : partitions) {

            Map<List<Integer>, Set<Etat>> sousGroupes =
                new LinkedHashMap<>();

            for (Etat etat : groupe) {

                List<Integer> signature = new ArrayList<>();

                for (String symbole : alphabet) {

                    Etat destination =
                        automate.transition(etat, symbole);

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

    // AFD quotient : un etat par classe. Tous les etats d'une classe
    // ont les memes transitions (au niveau des classes), donc il
    // suffit de lire celles d'un representant.
    private Afd construireAfdMinimal(
            Afd automate,
            List<Set<Etat>> partitions) {

        Afd minimal = new Afd();
        minimal.setAlphabet(automate.getAlphabet());

        Map<Etat, Etat> correspondance = new HashMap<>();

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

        Set<String> alphabet = automate.getAlphabet();

        for (Set<Etat> groupe : partitions) {

            Etat representant = groupe.iterator().next();

            Etat source =
                correspondance.get(representant);

            for (String symbole : alphabet) {

                Etat ancienneDestination =
                    automate.transition(representant, symbole);

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

    /**
     * Minimise l'AFD (algorithme de Moore), puis retire l'état puits.
     * L'automate renvoyé n'est donc pas complet : une transition
     * absente signifie qu'aucun état final n'est plus atteignable.
     *
     * @return l'AFD minimal, sans état puits
     * @throws IllegalStateException si l'AFD n'a pas d'état initial
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

    // Garde seulement les etats depuis lesquels un etat final est
    // atteignable (dans un AFD minimal, seul le puits ne l'est pas),
    // et les transitions entre ces etats. L'etat initial est toujours
    // garde pour que l'automate reste utilisable. Parcours en largeur
    // a l'envers depuis les finaux, en O(n · s). Les matchers
    // s'arretent ainsi des qu'aucune occurrence n'est plus possible.
    private Afd retirerPuits(Afd automate) {

        Map<Etat, List<Etat>> predecesseurs = new HashMap<>();

        for (Map.Entry<Etat, Map<String, Etat>> entree :
                automate.getTransitions().entrySet()) {

            for (Etat destination : entree.getValue().values()) {

                predecesseurs
                    .computeIfAbsent(destination, k -> new ArrayList<>())
                    .add(entree.getKey());
            }
        }

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
