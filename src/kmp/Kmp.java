package kmp;

import ast.AstChar;
import ast.AstConcat;
import ast.interfaces.AstI;

import experimentation.Matcher;

public class Kmp implements Matcher {

    private final String motif;
    private final char[] p;
    private final int[] echec;

    public Kmp(String motif) {
        this.motif = motif;
        this.p = motif.toCharArray();
        this.echec = calculerEchec(p);
    }

    private static int[] calculerEchec(char[] p) {

        int[] pi = new int[p.length];
        int k = 0;

        for (int i = 1; i < p.length; i++) {

            // Le bord ne se prolonge pas : on essaie le bord du bord
            while (k > 0 && p[i] != p[k]) {
                k = pi[k - 1];
            }

            if (p[i] == p[k]) {
                k++;
            }

            pi[i] = k;
        }

        return pi;
    }

    @Override
    public boolean contientMotif(String ligne) {

        int m = p.length;

        if (m == 0) {
            return true;
        }

        // q = nombre de caracteres du motif deja reconnus
        int q = 0;

        for (int i = 0; i < ligne.length(); i++) {

            char c = ligne.charAt(i);

            while (q > 0 && c != p[q]) {
                q = echec[q - 1];
            }

            if (c == p[q]) {
                q++;
            }

            if (q == m) {
                return true;
            }
        }

        return false;
    }

    public String getMotif() {
        return motif;
    }

    public int[] getEchec() {
        return echec.clone();
    }

    public static String motifLitteral(AstI ast) {

        StringBuilder mot = new StringBuilder();

        return collecter(ast, mot) ? mot.toString() : null;
    }

    private static boolean collecter(AstI noeud, StringBuilder mot) {

        if (noeud instanceof AstChar) {
            mot.append(((AstChar) noeud).getValeur());
            return true;
        }

        if (noeud instanceof AstConcat) {
            return collecter(noeud.getFilsGauche(), mot)
                && collecter(noeud.getFilsDroit(), mot);
        }

        return false;
    }
}
