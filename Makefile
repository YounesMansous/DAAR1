# Raccourcis vers les cibles Ant de build.xml, pour ceux qui tapent make.
# make        : jar exécutable egrep-clone.jar (cible par défaut)
# make test   : TestCorrection, puis TestAutomates
# make doc    : Javadoc HTML dans doc/
# make dist   : make clean jar, puis l'archive de rendu (.zip)

ANT ?= ant

.PHONY: all compile jar test doc clean dist

all: jar

compile jar test doc clean dist:
	$(ANT) $@
