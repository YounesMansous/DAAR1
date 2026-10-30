package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

/**
 * Nœud de l'AST : concaténation de deux sous-expressions.
 * Le Parser enchaîne les concaténations à gauche :
 * « abc » devient Concat(Concat(a, b), c).
 */
public class AstConcat implements AstI{

    private AstI filsDroit;
    private AstI filsGauche;

    /**
     * Crée la concaténation de deux sous-expressions.
     *
     * @param filsGauche la partie lue en premier
     * @param filsDroit  la partie lue ensuite
     */
    public AstConcat(AstI filsGauche, AstI filsDroit){
        this.filsGauche = filsGauche;
        this.filsDroit = filsDroit;
    }

    @Override 
    public AstI getFilsGauche(){
        return filsGauche;
    }

    @Override 
    public AstI getFilsDroit(){
        return filsDroit;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
