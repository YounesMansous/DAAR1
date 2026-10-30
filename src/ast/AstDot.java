package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

/**
 * Feuille de l'AST : le point, qui reconnaît n'importe quel
 * caractère. Devient une transition sur le symbole « . » dans
 * l'AFN-ε, puis le symbole {@code Afd.AUTRE} dans l'AFD.
 */
public class AstDot implements AstI{

    /** Crée un nœud point. */
    public AstDot(){
    }

    @Override 
    public AstI getFilsGauche(){
        return null;
    }

    @Override 
    public AstI getFilsDroit(){
        return null;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
