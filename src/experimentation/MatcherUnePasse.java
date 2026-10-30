package experimentation;

import ast.AstFactory;
import ast.interfaces.AstI;

import automate.AfdCompile;

/**
 * Recherche en une seule passe avec l'automate de .*R.
 *
 * Pourquoi c'est équivalent à chercher R n'importe où dans la ligne :
 * après lecture d'un préfixe p de la ligne, l'AFD de .*R est dans un
 * état final si et seulement si p ∈ .*R, c'est-à-dire si p se termine
 * par un mot de R. Or la ligne contient une occurrence de R
 * (ligne[i..j[ ∈ R) si et seulement si le préfixe ligne[0..j[ se
 * termine par un mot de R. Il suffit donc de lire la ligne de gauche
 * à droite et de s'arrêter au premier état final : il est atteint
 * exactement à la fin de la première occurrence de R. L'état initial
 * est final si et seulement si R accepte le mot vide, et alors toute
 * ligne (même vide) convient, comme avec grep.
 *
 * Complexité : O(n) par ligne de longueur n (une transition par
 * caractère). En contrepartie, l'AFD de .*R peut avoir plus d'états
 * que celui de R (exponentiellement plus dans le pire cas théorique).
 */
public class MatcherUnePasse implements Matcher {

    private final AfdCompile automate;

    /**
     * @param automate table de transitions de l'AFD minimal de .*R,
     *                 obtenu à partir de {@link #envelopper(AstI)}
     */
    public MatcherUnePasse(AfdCompile automate) {
        this.automate = automate;
    }

    /**
     * Construit l'AST de .*R : Concat(Repeat(Dot), R).
     *
     * @param r l'AST de la regex R
     * @return l'AST de .*R
     */
    public static AstI envelopper(AstI r) {
        return AstFactory.createConcat(
            AstFactory.createRepeat(AstFactory.createDot()),
            r);
    }

    @Override
    public boolean contientMotif(String ligne) {

        int etat = automate.getInitial();

        if (automate.estFinal(etat)) {
            return true;
        }

        int longueur = ligne.length();

        for (int i = 0; i < longueur; i++) {

            etat = automate.suivant(etat, ligne.charAt(i));

            // Ne se produit pas avec .*R (le point lit tout),
            // mais on reste prudent
            if (etat == AfdCompile.AUCUNE) {
                return false;
            }

            if (automate.estFinal(etat)) {
                return true;
            }
        }

        return false;
    }
}
