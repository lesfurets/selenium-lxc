TODO:
* Projet exemple sur github/lesfurets
* Complèter la boucle


# Selenium on the sky of LXC

## Case study
How to run many selenium grid Firefox nodes on a big cheap hosted server (32 threads, 128Go for 130 euros per month) ?

We had a Selenium grid on a standard server (8 threads, 16Go RAM) and we couldn't host more than 10 nodes, and it was not really stable and fast, so we decided to try on a brand new OVH hosted MP-128 server (32 threads, 128Go RAM) with the hope to run about 232 nodes. Were we dreaming ?

Sources usable to play with a small set of 200 Selenium tests :   

    https://github.com/lesfurets/selenium-lxc/releases/tag/SELXC_01


## Classic Selenium Grid setup on a single host
We started simply on a single host with a single user "grid" launching a selenium "hub" with 32 nodes.

This to start the hub:
    java -jar /home/grid/selenium-server-standalone-2.39.0.jar -role hub -timeout 180  -browserTimeout 300 -maxSession 100 -newSessionWaitTimeout -1 & 
    
And then a loop to launch 32 nodes on a distinct "port", each with a dedicated "xvfb" display :

for node in {1..32}; do 
    NUMDISPLAY=$((10 + $node)) 
    DISPLAY=$NUMDISPLAY 
    PORT=$((5000 + $node)) 
    FFPORT=$((7000 + $node)) 
    GRID_HUB=http://grid.mydomain.com:4444/grid/register 
    xvfb-run --auto-servernum --server-num=${NUMDISPLAY} -s "-screen 0 1600x1050x16" java  -jar /home/grid/selenium-server-standalone-2.39.0.jar -role node -hub ${GRID_HUB} -browser browserName=firefox,maxInstances=1,platform=LINUX -port ${PORT}  &
done


Note: 
Do not forget to check that /etc/hosts doesn't contain multi delcaration fors 127.0.0.1 (known selenium issue).
You should replace "<grid.mydomain.com>" by your server DNS Name

Now you can go to `http://grid.mydomain.com:4444` ... cool, 32 nodes are there !

Great, this is too simple let's start a hand of tests. But it's fast ! But 25 % error messages with « `Unable to bind to locking port 7054 within 45000 ms` »

Having a look to the Firefox selenium driver, there is a simple explanation : each node creates a Firefox profile, and verfies it, before running the tests. The driver performs a bind on port 7054 for each nodes (hardcoded) with a lock to avoid parallel calls, and a 45s timeout (hardcoded).

As each Firefox setup takes 5s on our hardware, it is not really surprising to see some failures when we use 10 nodes at the same time.

## So let's patch Selenium Firefox Driver !
Dirty as we can, we tried to set the timeout to more than 45s ! Let's call a code, fork the selenium repo, create the patch, and get the timeout to 400s…

No more errors ... but so slow to start the tests !! There's a big queue of nodes waiting for firefox to create a profile. No use of 32 threads ... we're sorry #FAIL

## One step beyond
Then we have some alternatives :

- get ride of the lock in Firefox driver ... forget it, it's there on purpose ;-)
- have 32 firefox setup ... not even in your dream any ops will accept that !
- go virtual has everbody says it's the silver bullet, but isn't it a bit to heavy ?

Let google for virtual solution on linux :
- Xen
- VMWare
- Virtualbox

For sure with those it will work, but it's heavy for only 32 independent Firefox instances, with waste on resources managemenr !
- OpenVZ look a lightweigth solution but requires a specific kernel
- LXC that's it ! And seems easy to install on my Ubuntu Server 12.04 LTS, it looks strangely like Solaris Zones when I was working on SUN Harware (good old days ... are gone).

## Let's go with LXC
What is that LXC ? LXC, stands for LinuX Container, it's designed to isolate some application and let then run like they were alone in a system (not far from chroot).

It's easy to clone each container, and then manage the same firefox setup in each.

Seupt step 1 : `apt-get install lxc`

By default, there is bridge interface ceated with NAT. Each containers are able to communicate with each others and the host system, and the internet with this NAT interface, that even work on your hosted server as there is no new MAC adress violating the hosting service restrictions (OVH doesn't allow self made clouds).

With lxc stock installation containers are stored under /var/lib/lxc and contents cache under /var/cache/lxc, but if you have deplaced them, you should check mount point where your containers are stored is not with *nosetuid*. For successfull sudo operations, containers should be hosted on partition without *nosetuid*. 

Great, the default setup is the one we need !!

Beware that if you did setup a firewall on your hosted server, it's quite sure that the NAT rule will be removed. Do not forget to setup one.

Let's create our first container, it's easy with so much ready to use templates. For example take the template ubuntu : node-template :

    lxc-create -t ubuntu -n node-template

Few minutes later you get a container which file-system is there `/var/lib/lxc/node-template`, with the simpliest ubuntu setup.

Start it:

    lxc-start -n node-template

Get into it, install a grid user and "Selenium Node" requirements :

    adduser grid
    apt-get install firefox xvfb wget
    cd /home/grid
    wget http://selenium.googlecode.com/files/selenium-server-standalone-2.39.0.jar

And still in the node-template, we create a job upstart within `/etc/init/grid-node.conf`:

    # Script UPStart pour le lancement du grid selenium 
    start on runlevel [2345] 
    stop on runlevel [016] 
    setuid grid 
    setgid grid 
    exec xvfb-run --auto-servernum --server-num=11 -s "-screen 0 1600x1050x16" java -jar /home/grid/selenium-server-standalone-2.39.0.jar -role node -hub http://grid.mondomaine.net:4444/grid/register -browser browserName=firefox,maxInstances=1,platform=LINUX -maxSession 1 -browserSessionReuse -port 5001

We can now shutdown, the container is ready to use : simples !

Now let's duplicate this template to 32 nodes, and start :

    for id in {1..32}; do 
      echo "Cloning node $id" 
      lxc-clone -o node-template -n node${id} 
      echo "Starting node $id" 
      lxc-start -n node${id} -d 
    done

Shutdown is like this :

    for id in {1..32}; do 
      echo "Stopping node $id" 
      lxc-stop -n node${id} 
      echo "Deletting node $id" 
      lxc-destroy -n node${id} 
    done

Let's run all selenium tests again. 

Résult : 4-5min that's way better, and no more Firefox errors.
What says the monitoring ? Saddly, the server spend it's time to clone firefox profiles, and waits for disks.

## Optimisation, step 2

How to avoid wasted time in Iowait ?

Use SSD with RAID0 ... expensive, and each container is only 600Mo so we only need 30Go of disk.
But wait ... we have 3128Go of RAM, which means that TMPfs should do the trick !!! RAM, is cheap.

Everything in RAM : no way it should be slow.

For sure if the server shutdowns you loose it all, but we don't care about those data, they are dropped each time. Go, go, go ... it should work !

Let's save the template somewhere :

    cd /var/lib/lxc
    tar -czf /home/grid/node-template.tar.gz node-template/

Create the TMPfs, and add it in "/etc/fstab":

    tmpfs		/var/lib/lxc	tmpfs	defaults,size=64g	0	0 

You should reserved something like 1,5 Go per node for ease.

We let you as exercice the grid setup to have everything launched automatically.

For the hub, again an upstart job in `/etc/init/grid-server.conf` – inside the Host OS with :

    # UPStart selenium grid 
    start on runlevel [2345] 
    stop on runlevel [016] 
    setuid grid 
    setgid grid 
    exec java -jar /home/grid/selenium-grid-lxc/target/selenium-server-standalone-2.39.0.jar -role hub -timeout 180  -browserTimeout 300 -maxSession 900 -newSessionWaitTimeout 3000000

For the nodes, an upstart job in `/etc/init/grid-nodes.conf` – inside the Host OS with :

    # UPStart selenium grid nodes
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

This is it, now it should run smoothly !
* containers cloning is 2s
* CPU Iowait has dropped to 0,2%
* all tests dropped from 10min to 45s, thanks who ?

As CPUs are not really fully working, it may even be possible to run 48 nodes ...

## Hardware recommendations

At start CPU was limitating, so we took lot of cores, now RAM is also a requirement, disk is no more.

For 48 selenium nodes : 32 cores, 128Go RAM. CPU is used at 100 % duing tests, RAM is 50%.

Hosting services by OVH allows us to have a MG-128, Online Dedibox WOPR 256G or Dedibox mWOPR 128G should fit, you feedbacks are welcome.
