package automate;

import ast.interfaces.AstI;

/**
 * Enchaîne la construction de l'automate à partir d'un AST :
 * AFN-epsilon (Aho-Ullman), AFD (sous-ensembles), AFD minimal
 * (Moore, sans puits), puis table de transitions.
 * Garde chaque étape pour --debug et --stats.
 */
public class ConstructionAutomate {

    private final Afn afn;
    private final Afd afd;
    private final Afd afdMinimal;
    private final AfdCompile compile;

    /**
     * @param ast l'AST dont on construit l'automate
     */
    public ConstructionAutomate(AstI ast) {
        this.afn = ast.accept(new AstToAfnVisitor());
        this.afd = new Determinisation(afn).convertir();
        this.afdMinimal = new Minimisation(afd).minimiser();
        this.compile = new AfdCompile(afdMinimal);
    }

    /** @return l'AFN-epsilon */
    public Afn getAfn() {
        return afn;
    }

    /** @return l'AFD obtenu par déterminisation */
    public Afd getAfd() {
        return afd;
    }

    /** @return l'AFD minimal, sans état puits */
    public Afd getAfdMinimal() {
        return afdMinimal;
    }

    /** @return la table de transitions de l'AFD minimal */
    public AfdCompile getCompile() {
        return compile;
    }
}
