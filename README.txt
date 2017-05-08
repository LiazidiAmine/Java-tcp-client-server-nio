JarRet

# Introduciton

Le but du projet est de distribuer de gros calculs sur plusieurs clients, 
et de collecter toutes les réponses au niveau du serveur. 

# Arborescence

 - A la racine du projet : Les fichiers exécutables du Serveur et du Client.
 - /doc/ : La documentation Java.
 - /config/ : La configuration du serveur.
 - /src/ : Les sources du projet.
 - /jobs.txt : Les jobs utilisés par le serveur.
 - /dist/lib/ : Les librairies externes utilisées.
 - /ManuelDev.pdf : Le manuel du développeur.

Avant de lancer quoi que ce soit, la version Java utilisée est 9.

# Le client 

	Sur un terminal, se placer dans le répértoire contenant le jar exécutable.
	Taper la commande : java -jar ClientJarRet.jar Host Port Id

# Le serveur

	Sur un terminal, se placer dans le répértoire contenant le jar exécutable.
	Taper la commande : java -jar ServerJarRet.jar
	Par défaut il est lancé sur le port 3000.

# Remarque 

La modification de la config du serveur se fait manuellement, 
en modifiant le Json dans /config/JarRetConfig.json