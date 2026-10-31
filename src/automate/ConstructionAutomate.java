
package automate;

import ast.interfaces.AstI;

//cette classe regroupe les differentes etapes pour construire notre afd minimisé a partir de l'AST
public class ConstructionAutomate {

    private final Afn afn;
    private final Afd afd;
    private final Afd afdMinimal;

    public ConstructionAutomate(AstI ast) {

        // transformation de l'AST en AFN avec notre visitor
        this.afn = ast.accept(new AstToAfnVisitor());

        // on determinise l'AFN pour obtenir notre AFD
        this.afd = new Determinisation(afn).convertir();

        // puis on minimise l'AFD avec Moore
        this.afdMinimal = new Minimisation(afd).minimiser();

    }

    public Afn getAfn() {
        return afn;
    }

    public Afd getAfd() {
        return afd;
    }

    public Afd getAfdMinimal() {
        return afdMinimal;
    }

}
