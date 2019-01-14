rmdir /q /s ..\out
mkdir ..\out
cd ..\src
javac client/*.java -d ../out
javac server/*.java -d ../out
javac common/*.java -d ../out
cd ..\out
jar cfe server.jar server.Server *
jar cfe client.jar client.Client *