package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

/**
 * Feuille de l'AST : un caractère littéral de la regex.
 * Devient une transition sur ce caractère dans l'AFN-ε.
 */
public class AstChar implements AstI{
    private Character valeur;

    /**
     * Crée une feuille pour un caractère.
     *
     * @param valeur le caractère reconnu
     */
    public AstChar(Character valeur){
        this.valeur = valeur;
    }

    /**
     * Renvoie le caractère reconnu.
     *
     * @return le caractère reconnu
     */
    public Character getValeur(){
        return valeur;
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
