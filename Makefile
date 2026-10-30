# Raccourcis vers les cibles Ant de build.xml, pour ceux qui tapent make.
# make        : jar exécutable egrep-clone.jar (cible par défaut)
# make test   : TestKmp, puis tests/verifier.sh s'il existe
# make doc    : Javadoc HTML dans doc/
# make bench  : banc de performances et figures (bench/README.md)

ANT ?= ant

.PHONY: all compile jar test doc clean bench

all: jar

compile jar test doc clean bench:
	$(ANT) $@
