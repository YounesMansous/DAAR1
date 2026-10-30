package ast.interfaces;

import ast.AstChar;
import ast.AstDot;
import ast.AstConcat;
import ast.AstOr;
import ast.AstRepeat;
import ast.AstVide;

/**
 * Visiteur de l'AST : une méthode par type de nœud. Le seul
 * visiteur du pipeline est {@code automate.AstToAfnVisitor}, qui
 * construit l'AFN-ε de la regex.
 *
 * @param <T> type du résultat de la visite
 */
public interface AstVisitor<T> {

    /**
     * Visite une lettre.
     *
     * @param noeud une lettre
     * @return le résultat de la visite
     */
    T visit(AstChar noeud);

    /**
     * Visite un point.
     *
     * @param noeud un point (caractère universel)
     * @return le résultat de la visite
     */
    T visit(AstDot noeud);

    /**
     * Visite une concaténation.
     *
     * @param noeud une concaténation
     * @return le résultat de la visite
     */
    T visit(AstConcat noeud);

    /**
     * Visite une alternative.
     *
     * @param noeud une alternative
     * @return le résultat de la visite
     */
    T visit(AstOr noeud);

    /**
     * Visite une étoile.
     *
     * @param noeud une étoile
     * @return le résultat de la visite
     */
    T visit(AstRepeat noeud);

    /**
     * Visite le mot vide.
     *
     * @param noeud le mot vide
     * @return le résultat de la visite
     */
    T visit(AstVide noeud);
}
