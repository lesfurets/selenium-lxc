# Selenium in the sky of LXC

## Problématique
Comment faire tourner plusieurs nodes d'un grid selenium avec le firefox driver sur une même machine ?

Ceux qui se sont déjà frotté à cette problématique savent que ce n'est pas toujours aisé, surtout si l'on dépasse les 10 nodes. L'objectif étant d'en faire tourner au moins 32, le pari n'est pas gagné d'avance.

Sources usable to play with a small set of 200 Selenium tests :   

    https://github.com/lesfurets/selenium-lxc/releases/tag/SELXC_01

## Installation classique du grid
On commence simple, sur une même machine, avec un même utilisateur « grid », on va lancer un hub, et 32 nodes.

Pour le serveur, on exécutera quelquechose du genre :

    java -jar /home/grid/selenium-server-standalone-2.39.0.jar -role hub -timeout 180  -browserTimeout 300 -maxSession 100 -newSessionWaitTimeout -1 & 
    
Et ensuite, une petite boucle pour lancer 32 nodes sur un port différent, le tout dans un xvfb :

    NUMDISPLAY=$((10 + $1)) 
    DISPLAY=$NUMDISPLAY 
    PORT=$((5000 + $1)) 
    FFPORT=$((7000 + $1)) 
    GRID_HUB=http://grid.mondomaine.net:4444/grid/register 
    xvfb-run --auto-servernum --server-num=${NUMDISPLAY} -s "-screen 0 1600x1050x16" java  -jar /home/grid/selenium-server-standalone-2.39.0.jar -role node -hub ${GRID_HUB} -browser browserName=firefox,maxInstances=1,platform=LINUX -port ${PORT}  &

Comme j'ai bien pensé à vérifier que mon /etc/hosts ne cont enait pas plusieurs entrées pour la ligne 127.0.0.1 (cf un bug connu chez selenium), ça démarre.

On va voir un petit coup sur `http://grid.mondomaine.net:4444`, cool, il y a bien nos 32 nœuds de connectés !

Bon, ça a l'air simple finalement, et si je lançais quelques tests, une 100aine pour commencer.

Résultats : effectivement, c'est rapide ! Mais j'ai 25 % d'erreurs avec un message « `Unable to bind to locking port 7054 within 45000 ms` »

En regardant le fonctionnement du driver Firefox, l'explication est simple : chaque nœud se crée un profil firefox, et le teste, avant de pouvoir exécuter les tests. Cela doit être exécuté en 45s ; cherchez pas, c'est codé en dur ; et ne peut être exécuté en parallèle. Pour éviter la paralélisation, chaque nœud tente de faire un bind sur le port 7054 ; c'est aussi en dur ; d'où l'erreur si il n'y arrive pas.

Sur notre machine, cette opération prend 5 secondes environ, les disques étant des disques classiques. Donc 5 secondes pour 10 nœuds, on passe le timeout, et un des nœud ne peut pas fonctionner correctement.

## Ça marche pas, on patch !
On est bloqué par un timeout en dur, pas grave, on a cas l'augmenter ! Le temps d'appeler un vrai développeur, cloner les sources, et le timeout passe à 400s…

Ce coup-ci, on n'a plus l'erreur, mais faut avouer que c'est quand mêne lent… À chaque test, on commence par attendre que les voisins veulent bien nous laisser la main, pourquoi avoir pris une machine avec pleins de cœurs alors ?

## Un peu d'optimisation
Se présentent alors plusieurs solutions :

- virer le lock durant le clone du profil firefox → oubliez, si c'est là, c'est que c'est utile…
- avoir une install de firefox par nœud → et avec 32 installs différentes, tu vas t'amuser à le maintenir et à t'y retrouver !
- faire des machines virtuelles → tout le monde semble faire ça, c'est que ça doit être pas mal, mais c'est pas un peu lourd ?

Une petite recherche sur la virtualisation sous linux, que me propose t'on ?
- Xen
- VMWare
- Virtualbox

→ effectivement, avec ces 3 là, c'est sur que ça va marcher, mais il va en falloir des ressources pour 32 nœuds !
- OpenVZ → on commence à se rapprocher d'un truc léger, mais ça nécessite un kernel spécial
- LXC → ah, ça semble pas mal et facile à installer sur mon Ubuntu Server 12.04 LTS on dirait des zones Solaris, à l'époque où je bossais sur du matos Sun (nostalgie d'une époque révolue…)

## Testons LXC
Mais c'est quoi au juste ? LXC, pour LinuX Container, est un système de containers pour Linux. Il permet d'isoler des systèmes ou des applications dans ce qu'ils verront comme une machine virtuelle.

L'intérêt pour nous est de pouvoir cloner les containers facilement, et ainsi n'avoir qu'une install de Firefox à gérer.

L'installation est plutot aisée : `apt-get install lxc`

Par défaut, au démarrage, on va avoir une interface bridge de créée avec du NAT. Comme ça, nos containers vont pouvoir communiquer entre eux et avec la machine physique, puis iront sur le net via la connexion nattée – donc pas d'adresse MAC qui traine sur le réseau de votre hébergeur.

C'est cool, ça, la conf par défaut qui est celle qu'on veut !

Notez quand même que si vous avez un firewall à vous, il est presque sur que la règle de NAT par défaut va être supprimée. Pensez à en recréer une !

On va pouvoir créer notre premier container. C'est facile, il y a des templates tout prêt, par exemple pour créer un container avec le template ubuntu, s'appelant node-template :

    lxc-create -t ubuntu -n node-template

Quelques minutes après, on a un container dont le filesystem est dans `/var/lib/lxc/node-template`, avec une install de Ubuntu la plus simple possible.

On la démarre :

    lxc-start -n node-template

Une fois dedans, on va se créer un user grid, et y installer ce dont on a besoin :

    adduser grid
    apt-get install firefox xvfb wget

Il reste à télécharger le jar de selenium dans le container, et rendre ça bootable :

    cd /home/grid
    wget http://selenium.googlecode.com/files/selenium-server-standalone-2.39.0.jar

Et, toujours dans le container node-template, on crée un job upstart en créant `/etc/init/grid-node.conf` avec :

    # Script UPStart pour le lancement du grid selenium 
    start on runlevel [2345] 
    stop on runlevel [016] 
    setuid grid 
    setgid grid 
    exec xvfb-run --auto-servernum --server-num=11 -s "-screen 0 1600x1050x16" java -jar /home/grid/selenium-server-standalone-2.39.0.jar -role node -hub http://grid.mondomaine.net:4444/grid/register -browser browserName=firefox,maxInstances=1,platform=LINUX -maxSession 1 -browserSessionReuse -port 5001

On peut éteindre le container, ça devrait démarrer tout seul.

Maintenant, il reste à cloner le template en 32 nodes et démarrer tout ça :

    for id in {1..32}; do 
      echo "Cloning node $id" 
      lxc-clone -o node-template -n node${id} 
      echo "Starting node $id" 
      lxc-start -n node${id} -d 
    done

Et si on veut éteindre tout ça :

    for id in {1..32}; do 
      echo "Stopping node $id" 
      lxc-stop -n node${id} 
      echo "Deletting node $id" 
      lxc-destroy -n node${id} 
    done

C'est plutot simple comme usage. Allez, on relance notre batterie de tests seleniums.

Résultat : ça tourne assez vite, et il n'y a plus d'erreurs de démarrage de Firefox !

Et dans les stats ? C'est dommage, la machine passe encore beaucoup de temps à cloner des Firefox, ou plutot, à attendre les accès disques.

## Optimisation, 2nd round
Comment faire pour ne plus passer son temps en Iowait ?

Soit on se fait une grappe de SSD en RAID0, mais ça va faire cher, et chaque container n'utilise que 600Mo d'espace disque, soit on balance tout en TMPfs – la RAM, ça coute pas cher !

L'avantage certain de tout mettre en RAM : la rapidité !

L'inconvénient : si on éteint la machine, les données sont perdues. En même temps, les données de chaque node, on s'en fout, tant qu'on garde le template… Allez hop, c'est parti !

Histoire de garder notre template sur le disque, on va le compresser et le mettre de coté :

    cd /var/lib/lxc
    tar -czf /home/grid/node-template.tar.gz node-template/

On peut créer alors le tmpfs, en ajoutant dans le /etc/fstab :

    tmpfs		/var/lib/lxc	tmpfs	defaults,size=64g	0	0 

En adaptant bien sur l'espace maximum alloué aux spécifications de la machine. Comptez 1,5 Go par node pour être large.

Il faudra un peu scripter le démarrage du grid, et tant qu'à faire, que tout démarre automatiquement.

Pour le hub, un job upstart dans `/etc/init/grid-server.conf` – sur la machine physique – contenant :

    # Script UPStart pour le lancement du grid selenium 
    start on runlevel [2345] 
    stop on runlevel [016] 
    setuid grid 
    setgid grid 
    exec java -jar /home/grid/selenium-grid-lxc/target/selenium-server-standalone-2.39.0.jar -role hub -timeout 180  -browserTimeout 300 -maxSession 900 -newSessionWaitTimeout 3000000

Et pour démarrer les containers, un nouveau job upstart dans `/etc/init/grid-nodes.conf`

    # Script UPStart pour le lancement du grid selenium 
    start on runlevel [2345] 
    stop on runlevel [016] 
    pre-start script
      if [ ! -d "/var/lib/lxc/node-template" ]; then 
          echo "Template inexistant, decompression" 
          cd /var/lib/lxc 
          tar -xzf /home/grid/node-template.tar.gz 
      fi 
      for id in $(seq 1 32); do 
          echo "Cloning node $id" 
          lxc-clone -o node-template -n node${id} 
          echo "Starting node $id" 
          lxc-start -n node${id} -d 
      done 
    end script
    pre-stop script
      for id in $(seq 1 32); do 
          echo "Stopping node $id" 
          lxc-stop -n node${id} 
          echo "Deletting node $id" 
          lxc-destroy -n node${id} 
      done 
    end script

Et voilà, ça devrait tourner vachement mieux maintenant !

Le clonage des containers passe à 2 secondes, et le CPU Iowait est retombé à moins de 0,2%.

On se permettra même de passer à 48 nœuds, vu que, apparemment, les CPU ne sont pas tous à fond durant l'exécution des tests.

## Choix du matériel

Dans ma démarche, on était parti de l'idée de prendre une machine avec pleins de cœurs, et maintenant, on a une contrainte de mémoire en plus.

Dans notre cas, pour 48 nœuds, on tourne avec une machine à 32 cœurs et 128 Go de RAM. Le CPU est à 100 % durant les tests, la mémoire à 50 %.

Les nouveaux serveurs MG-128 de chez OVH répondent bien à ce besoin, la Dedibox WOPR 256G de chez Online devrait, elle, gérer un peu plus de nœuds pour un peu plus cher, ou la Dedibox mWOPR 128G, en gérer un peu moins pour un peu moins cher. A vous de voir.

