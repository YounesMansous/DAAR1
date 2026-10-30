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

    @Override
    public AstI getFilsGauche() {
        return null;
    }

    @Override
    public AstI getFilsDroit() {
        return null;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
