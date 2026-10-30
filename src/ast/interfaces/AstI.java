package ast.interfaces;

/**
 * Nœud de l'AST d'une regex, produit par le Parser et transformé
 * en AFN-ε par {@code automate.AstToAfnVisitor}.
 *
 * <p>L'arbre est binaire : une feuille (lettre, point, mot vide) n'a
 * pas de fils, une étoile n'a qu'un fils gauche, une concaténation
 * et une alternative en ont deux.</p>
 */
public interface AstI {

    /**
     * Renvoie le fils gauche du nœud.
     *
     * @return le fils gauche (le seul fils d'une étoile),
     *         ou {@code null} pour une feuille
     */
    public AstI getFilsGauche();

    /**
     * Renvoie le fils droit du nœud.
     *
     * @return le fils droit, ou {@code null} pour une feuille
     *         ou une étoile
     */
    public AstI getFilsDroit();

    /**
     * Accepte un visiteur (patron visiteur) : appelle la méthode
     * {@code visit} qui correspond au type réel du nœud.
     *
     * @param <T>     type du résultat de la visite
     * @param visitor le visiteur
     * @return le résultat de la visite de ce nœud
     */
    <T> T accept(AstVisitor<T> visitor);

}
