package ast;

import ast.interfaces.AstI;
import ast.interfaces.AstVisitor;

public class AstDot implements AstI{

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
