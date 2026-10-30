
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

public class AstToAfnVisitor implements AstVisitor<Afn> {

    // ==========================================
    // Copier les etats et transitions d'un AFN
    // ==========================================

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

    // ==========================================
    // CARACTERE
    // ==========================================

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

    // ==========================================
    // POINT (.)
    // ==========================================

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

    // ==========================================
    // MOT VIDE
    // ==========================================

    /**
     * AFN du mot vide : un seul état, à la fois initial et final.
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

    // ==========================================
    // CONCATENATION
    // ==========================================

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

    // ==========================================
    // ALTERNATIVE (|)
    // ==========================================

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

    // ==========================================
    // REPETITION (*)
    // ==========================================

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
