package automate;

import java.util.Map;
import java.util.Set;

import ast.AstChar;
import ast.AstConcat;
import ast.AstDot;
import ast.AstOr;
import ast.AstRepeat;
import ast.AstVide;
import ast.interfaces.AstVisitor;

/**
 * Deuxième étape du pipeline : construit l'AFN-ε d'une regex à partir
 * de son AST (construction de Thompson, dans la présentation
 * d'Aho et Ullman).
 *
 * <p>Chaque nœud donne un AFN avec un état initial et des états
 * finaux. Les AFN des fils sont recopiés dans celui du père, puis
 * reliés par des ε-transitions. Un nœud ajoute au plus deux états et
 * quatre ε-transitions, plus une ε-transition par état final d'un
 * fils : l'AFN de R a O(|R|) états et transitions.</p>
 *
 * <p>Temps : chaque nœud recopie les AFN de ses fils, et
 * {@code ajouterEtat} vérifie en temps linéaire que l'état est
 * nouveau. Une concaténation de k lettres (arbre en peigne de
 * profondeur k) coûte donc O(k³) dans le pire cas, ce qui reste
 * négligeable pour des motifs de quelques dizaines de caractères.</p>
 */
public class AstToAfnVisitor implements AstVisitor<Afn> {

    /** Crée le visiteur ; il n'a pas d'état et peut être réutilisé. */
    public AstToAfnVisitor() {
    }

    // Recopie les états et les transitions de source dans destination
    // (sans l'initial ni les finaux, que l'appelant choisit)
    private void copierAfn(Afn source, Afn destination) {

        // Copier les etats
        for (Etat etat : source.getEtats()) {
            destination.ajouterEtat(etat);
        }

        // Copier les transitions de la HashMap
        for (Map.Entry<Etat, Map<String, Set<Etat>>> entreeEtat :
                source.getTransitions().entrySet()) {

            Etat depart = entreeEtat.getKey();

            for (Map.Entry<String, Set<Etat>> entreeSymbole :
                    entreeEtat.getValue().entrySet()) {

                String symbole = entreeSymbole.getKey();

                for (Etat arrivee : entreeSymbole.getValue()) {

                    destination.ajouterTransition(
                        new Transition(depart, arrivee, symbole)
                    );
                }
            }
        }
    }

    /**
     * Lettre c : debut --c--&gt; fin. 2 états, 1 transition.
     *
     * @param noeud la lettre
     * @return l'AFN qui reconnaît exactement cette lettre
     */
    @Override
    public Afn visit(AstChar noeud) {

        Etat debut = new Etat();
        Etat fin = new Etat();

        Afn afn = new Afn();

        afn.ajouterEtat(debut);
        afn.ajouterEtat(fin);

        afn.setInitial(debut);
        afn.ajouterEtatFinal(fin);

        afn.ajouterTransition(
            new Transition(
                debut,
                fin,
                String.valueOf(noeud.getValeur())
            )
        );

        return afn;
    }

    /**
     * Point : debut --.--&gt; fin. Le symbole « . » est traité comme
     * « n'importe quel caractère » par la déterminisation.
     *
     * @param noeud le point
     * @return l'AFN qui reconnaît un caractère quelconque
     */
    @Override
    public Afn visit(AstDot noeud) {

        Etat debut = new Etat();
        Etat fin = new Etat();

        Afn afn = new Afn();

        afn.ajouterEtat(debut);
        afn.ajouterEtat(fin);

        afn.setInitial(debut);
        afn.ajouterEtatFinal(fin);

        afn.ajouterTransition(
            new Transition(debut, fin, ".")
        );

        return afn;
    }

    /**
     * Mot vide : un seul état, à la fois initial et final.
     *
     * @param noeud le mot vide
     * @return l'AFN qui ne reconnaît que le mot vide
     */
    @Override
    public Afn visit(AstVide noeud) {

        Etat etat = new Etat();

        Afn afn = new Afn();

        afn.ajouterEtat(etat);
        afn.setInitial(etat);
        afn.ajouterEtatFinal(etat);

        return afn;
    }

    /**
     * Concaténation G D : chaque état final de G est relié par ε à
     * l'initial de D. Initial de G, finaux de D ; aucun nouvel état.
     *
     * @param noeud la concaténation
     * @return l'AFN de G D
     */
    @Override
    public Afn visit(AstConcat noeud) {

        Afn gauche = noeud.getFilsGauche().accept(this);
        Afn droite = noeud.getFilsDroit().accept(this);

        Afn resultat = new Afn();

        copierAfn(gauche, resultat);
        copierAfn(droite, resultat);

        // Relier les finaux du gauche
        // a l'initial du droite
        for (Etat etat : gauche.getFinaux()) {

            resultat.ajouterTransition(
                new Transition(
                    etat,
                    droite.getInitial(),
                    "EPSILON"
                )
            );
        }

        resultat.setInitial(gauche.getInitial());

        for (Etat etat : droite.getFinaux()) {
            resultat.ajouterEtatFinal(etat);
        }

        return resultat;
    }

    /**
     * Alternative G|D : nouvel initial relié par ε aux initiaux de G
     * et de D, et finaux de G et de D reliés par ε à un nouvel état
     * final unique. 2 nouveaux états.
     *
     * @param noeud l'alternative
     * @return l'AFN de G|D
     */
    @Override
    public Afn visit(AstOr noeud) {

        Afn gauche = noeud.getFilsGauche().accept(this);
        Afn droite = noeud.getFilsDroit().accept(this);

        Etat debut = new Etat();
        Etat fin = new Etat();

        Afn resultat = new Afn();

        copierAfn(gauche, resultat);
        copierAfn(droite, resultat);

        resultat.ajouterEtat(debut);
        resultat.ajouterEtat(fin);

        // Choisir une des deux branches
        resultat.ajouterTransition(
            new Transition(
                debut,
                gauche.getInitial(),
                "EPSILON"
            )
        );

        resultat.ajouterTransition(
            new Transition(
                debut,
                droite.getInitial(),
                "EPSILON"
            )
        );

        // Rejoindre le nouvel etat final
        for (Etat etat : gauche.getFinaux()) {

            resultat.ajouterTransition(
                new Transition(etat, fin, "EPSILON")
            );
        }

        for (Etat etat : droite.getFinaux()) {

            resultat.ajouterTransition(
                new Transition(etat, fin, "EPSILON")
            );
        }

        resultat.setInitial(debut);
        resultat.ajouterEtatFinal(fin);

        return resultat;
    }

    /**
     * Étoile E* : nouvel initial et nouvel état final, avec
     * ε-transitions debut → initial de E (entrer), debut → fin (zéro
     * répétition), finaux de E → initial de E (recommencer) et
     * finaux de E → fin (sortir). 2 nouveaux états.
     *
     * @param noeud l'étoile
     * @return l'AFN de E*
     */
    @Override
    public Afn visit(AstRepeat noeud) {

        Afn interieur = noeud.getFils().accept(this);

        Etat debut = new Etat();
        Etat fin = new Etat();

        Afn resultat = new Afn();

        copierAfn(interieur, resultat);

        resultat.ajouterEtat(debut);
        resultat.ajouterEtat(fin);

        // Entrer dans l'expression repetee
        resultat.ajouterTransition(
            new Transition(
                debut,
                interieur.getInitial(),
                "EPSILON"
            )
        );

        // Autoriser zero repetition
        resultat.ajouterTransition(
            new Transition(debut, fin, "EPSILON")
        );

        for (Etat etat : interieur.getFinaux()) {

            // Recommencer
            resultat.ajouterTransition(
                new Transition(
                    etat,
                    interieur.getInitial(),
                    "EPSILON"
                )
            );

            // Terminer
            resultat.ajouterTransition(
                new Transition(etat, fin, "EPSILON")
            );
        }

        resultat.setInitial(debut);
        resultat.ajouterEtatFinal(fin);

        return resultat;
    }
}
