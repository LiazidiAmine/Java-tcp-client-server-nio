Petit aide mémoire pour GIT : https://services.github.com/on-demand/downloads/fr/github-git-cheat-sheet.pdf

Comment ça fonctionne : 

1- chacun de nous clone le projet en local sur sa machine --> git clone https://github.com/LiazidiAmine/oasis.git   
2- GIT utilise un systeme de branche, comme dans un arbre, au depart du projet, on est sur la branche master qui est vide.
chacun de son coté crée une nouvelle branche dés qu'il commence une nouvelle tâche et se positionne dedans pour écrire dessus
--> git checkout -b NOMDELABRANCHE --> le -b permet de créer la branche. git checkout BRANCHE permet de se positionner sur la BRANCHE.   
3- Dés que l'un de nous fais des changements sur sa branche :    
  git status --> affiche en rouge les fichiers modifiés qui n'ont pas encore été commité et en vert ceux qui ont été commité    
  git add NOMFICHIERMODIFIE1 NOMFICHIERMODIFIE2 NOMFICHIERMODIFIE3 --> permet d'ajouter les fichiers modifié a un commit    
  git status --> permet de voir que les precedent fichiers ajoutés sont desormais en vert prêt à être commit    
  git commit -m "message indiquant ce qu'on a fais lors de ces modifs" --> ça commit les fichiers precedents, c'est prêt
  à être envoyer au depot distant GIT du coup.    
  git push origin NOMDELABRANCHE --> permet d'envoyer les modifications enregistrées sur la branche NOMDELABRANCHE      
4- Ensuite, sur github, faut faire une Pull Request, en indiquant la branche master et NOMDELABRANCHE afin que le nouveau 
  contenu de la nouvelle branche soit d'abord testé par l'autre personne avant que ces modifications ne soient ajoutés 
  à la branche master.      
  Ca permet d'éviter d'enregistrer de la merde sur la branche master, éviter d'avoir du code qui ne 
  fonctionne pas sur master, car master est sensé rester toujours propre.     
  
Le principe est qu'à chaque début d'une nouvelle tâche du style:      
Mise en place du code basique permettant la connexion a un serveur, on crée une nouvelle branche pour pusher dessus.     
A chaque fois qu'on fait une avancée dans le projet, du style: le parsing de la reponse GET est fonctionnel,
on add les fichiers modifiés, on commit, on push sur cette nouvelle branche.     
et ensuite si tout est ok sur la nouvelle branche, on la fusionne avec master avec une Pull Request.    
5- Pour vérifier qu'une Pull Request est ok --> 
git fetch origin pull/IDdeLaPullRequest/head:NOMDELABRANCHE    
git checkout NOMDELABRANCHE --> pour se positionner sur cette branche, et la tester ou la modifié
une fois que c'est ok, on n'oublie pas de revenir sur sa vraie branche de travail.    

Faudra pas oublier de faire un git pull origin master afin d'être a jour par rapport à la branche principale master quand
celle ci est modifiée.
