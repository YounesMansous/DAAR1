package experimentation;

import automate.AfdCompile;

/**
 * Recherche naïve avec l'automate de R : on relance l'AFD depuis
 * chaque position de départ de la ligne, jusqu'au premier état final.
 *
 * <p>Complexité : O(n²) par ligne de longueur n dans le pire cas
 * (n départs, et jusqu'à n caractères lus pour chacun). En pratique,
 * le retrait de l'état puits arrête chaque essai dès qu'aucun état
 * final n'est plus atteignable, mais un motif comme « a.*b » lit
 * quand même toute la fin de la ligne depuis chaque « a ».</p>
 */
public class MatcherNaif implements Matcher {

    private final AfdCompile automate;

    /**
     * Crée le matcher à partir de l'automate de R.
     *
     * @param automate table de transitions de l'AFD minimal de R
     */
    public MatcherNaif(AfdCompile automate) {
        this.automate = automate;
    }

    /**
     * Pour chaque position de départ, lit la ligne avec l'AFD de R
     * jusqu'à un état final (trouvé) ou une transition absente
     * (essai suivant). O(n²) dans le pire cas.
     *
     * @param ligne une ligne du texte
     * @return vrai si la ligne contient un mot de R
     */
    @Override
    public boolean contientMotif(String ligne) {

        int initial = automate.getInitial();

        // R accepte le mot vide : toute ligne le contient
        if (automate.estFinal(initial)) {
            return true;
        }

        int longueur = ligne.length();

        for (int debut = 0; debut < longueur; debut++) {

            int etat = initial;

            for (int i = debut; i < longueur; i++) {

                etat = automate.suivant(etat, ligne.charAt(i));

                if (etat == AfdCompile.AUCUNE) {
                    break;
                }

                if (automate.estFinal(etat)) {
                    return true;
                }
            }
        }

        return false;
    }
}
