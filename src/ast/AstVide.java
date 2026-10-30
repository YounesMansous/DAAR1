package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

/**
 * Feuille de l'AST qui reconnaît le mot vide.
 * Produite pour le motif vide, une branche vide d'alternative
 * (« a| », « |a ») et des parenthèses vides (« () »).
 */
public class AstVide implements AstI {

    /** Crée un nœud mot vide. */
    public AstVide() {
    }

    /** @return toujours {@code null} : une feuille n'a pas de fils */
    @Override
    public AstI getFilsGauche() {
        return null;
    }

    /** @return toujours {@code null} : une feuille n'a pas de fils */
    @Override
    public AstI getFilsDroit() {
        return null;
    }

    /** Accepte un visiteur (patron visiteur). */
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
