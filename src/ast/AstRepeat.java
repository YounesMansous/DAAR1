package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

public class AstRepeat implements AstI{

    private AstI fils;
    
    public AstRepeat(AstI fils){
        this.fils = fils;
    }
 
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
