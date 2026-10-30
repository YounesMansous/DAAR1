package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

/**
 * Nœud de l'AST : étoile de Kleene « fils* » (zéro répétition
 * ou plus). Son unique fils est aussi son fils gauche.
 */
public class AstRepeat implements AstI{

    private AstI fils;

    /**
     * Crée l'étoile d'une sous-expression.
     *
     * @param fils l'expression répétée
     */
    public AstRepeat(AstI fils){
        this.fils = fils;
    }

    /**
     * Renvoie l'expression répétée.
     *
     * @return l'expression répétée
     */
    public AstI getFils(){
        return fils;
    }

    @Override 
    public AstI getFilsGauche(){
        return fils;
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
