package ast.interfaces;

/**
 * Signatures de la fabrique de nœuds de l'AST.
 *
 * <p>Les méthodes {@code static} d'une interface ne sont pas
 * héritées : cette interface ne contraint donc pas
 * {@code ast.AstFactory}, elle documente seulement les signatures
 * que la fabrique reprend à l'identique. Ses propres méthodes ne
 * doivent pas être appelées.</p>
 */
public interface AstFactoryI {

    /**
     * Feuille pour une lettre.
     *
     * @param valeur la lettre
     * @return une feuille pour cette lettre
     * @throws UnsupportedOperationException toujours
     */
    public static AstI createChar(Character valeur) {
        throw new UnsupportedOperationException();
    }

    /**
     * Feuille pour le point.
     *
     * @return une feuille pour le point
     * @throws UnsupportedOperationException toujours
     */
    public static AstI createDot(){
        throw new UnsupportedOperationException();
    }

    /**
     * Feuille pour le mot vide.
     *
     * @return une feuille pour le mot vide
     * @throws UnsupportedOperationException toujours
     */
    public static AstI createVide(){
        throw new UnsupportedOperationException();
    }

    /**
     * Nœud étoile.
     *
     * @param fils l'expression répétée
     * @return un nœud étoile
     * @throws UnsupportedOperationException toujours
     */
    public static AstI createRepeat(AstI fils){
        throw new UnsupportedOperationException();
    }

    /**
     * Nœud de concaténation.
     *
     * @param gauche la partie gauche
     * @param droite la partie droite
     * @return un nœud de concaténation
     * @throws UnsupportedOperationException toujours
     */
    public static AstI createConcat(AstI gauche, AstI droite){
        throw new UnsupportedOperationException();
    }

    /**
     * Nœud d'alternative.
     *
     * @param gauche la première branche
     * @param droite la seconde branche
     * @return un nœud d'alternative
     * @throws UnsupportedOperationException toujours
     */
    public static AstI createOr(AstI gauche, AstI droite){
        throw new UnsupportedOperationException();
    }
}
