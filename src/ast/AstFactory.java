package ast;

import ast.interfaces.AstFactoryI;
import ast.interfaces.AstI;

/**
 * Fabrique des nœuds de l'AST, utilisée par le Parser et par
 * {@code MatcherUnePasse.envelopper} (construction de .*R).
 */
public class AstFactory implements AstFactoryI{

    /** Classe utilitaire : toutes les méthodes sont statiques. */
    private AstFactory() {
    }

    /**
     * Crée une feuille pour une lettre.
     *
     * @param valeur la lettre
     * @return une feuille pour la lettre {@code valeur}
     */
    public static AstI createChar(Character valeur) {
        return new AstChar(valeur);
    }

    /**
     * Renvoie une feuille pour le point (caractère universel).
     *
     * @return une feuille pour le point (caractère universel)
     */
    public static AstI createDot() {
        return new AstDot();
    }

    /**
     * Renvoie une feuille qui reconnaît le mot vide.
     *
     * @return une feuille qui reconnaît le mot vide
     */
    public static AstI createVide() {
        return new AstVide();
    }

    /**
     * Crée une étoile.
     *
     * @param fils l'expression répétée
     * @return le nœud {@code fils*}
     */
    public static AstI createRepeat(AstI fils) {
        return new AstRepeat(fils);
    }

    /**
     * Crée une concaténation.
     *
     * @param gauche la partie gauche
     * @param droite la partie droite
     * @return le nœud {@code gauche droite}
     */
    public static AstI createConcat(AstI gauche, AstI droite) {
        return new AstConcat(gauche, droite);
    }

    /**
     * Crée une alternative.
     *
     * @param gauche la première branche
     * @param droite la seconde branche
     * @return le nœud {@code gauche|droite}
     */
    public static AstI createOr(AstI gauche, AstI droite) {
        return new AstOr(gauche, droite);
    }
}
