package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

/**
 * Nœud de l'AST : alternative « gauche|droite ».
 */
public class AstOr implements AstI {
    private AstI filsDroit;
    private AstI filsGauche;

    /**
     * Crée l'alternative entre deux branches.
     *
     * @param filsGauche la première branche
     * @param filsDroit  la seconde branche
     */
    public AstOr(AstI filsGauche, AstI filsDroit){
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
