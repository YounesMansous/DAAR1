package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

public class AstConcat implements AstI{

    private AstI filsDroit;
    private AstI filsGauche;

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
