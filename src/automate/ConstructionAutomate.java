package automate;

import ast.interfaces.AstI;

/**
 * Enchaîne la construction de l'automate à partir d'un AST :
 * AFN-ε ({@link AstToAfnVisitor}, Thompson / Aho-Ullman), AFD
 * ({@link Determinisation}, sous-ensembles), AFD minimal
 * ({@link Minimisation}, Moore, sans puits), puis table de
 * transitions ({@link AfdCompile}). Garde chaque étape pour --debug
 * et --stats.
 */
public class ConstructionAutomate {

    private final Afn afn;
    private final Afd afd;
    private final Afd afdMinimal;
    private final AfdCompile compile;

    /**
     * Construit les quatre étapes, dans l'ordre.
     *
     * @param ast l'AST dont on construit l'automate (celui de R ou,
     *            pour le matcher une passe, celui de .*R)
     */
    public ConstructionAutomate(AstI ast) {
        this.afn = ast.accept(new AstToAfnVisitor());
        this.afd = new Determinisation(afn).convertir();
        this.afdMinimal = new Minimisation(afd).minimiser();
        this.compile = new AfdCompile(afdMinimal);
    }

    /**
     * Renvoie l'AFN-epsilon.
     *
     * @return l'AFN-epsilon
     */
    public Afn getAfn() {
        return afn;
    }

    /**
     * Renvoie l'AFD obtenu par déterminisation.
     *
     * @return l'AFD obtenu par déterminisation
     */
    public Afd getAfd() {
        return afd;
    }

    /**
     * Renvoie l'AFD minimal, sans état puits.
     *
     * @return l'AFD minimal, sans état puits
     */
    public Afd getAfdMinimal() {
        return afdMinimal;
    }

    /**
     * Renvoie la table de transitions de l'AFD minimal.
     *
     * @return la table de transitions de l'AFD minimal
     */
    public AfdCompile getCompile() {
        return compile;
    }
}
