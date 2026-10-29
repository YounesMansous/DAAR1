package ast.interfaces;

import ast.AstChar;
import ast.AstDot;
import ast.AstConcat;
import ast.AstOr;
import ast.AstRepeat;

public interface AstVisitor<T> {
    T visit(AstChar noeud);
    T visit(AstDot noeud);
    T visit(AstConcat noeud);
    T visit(AstOr noeud);
    T visit(AstRepeat noeud);
}
