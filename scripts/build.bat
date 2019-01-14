rmdir /q /s ..\out
mkdir ..\out
cd ..\src
javac client/*.java -d ../out
javac server/*.java -d ../out
javac common/*.java -d ../out
cd ..\out
rmdir /q /s ..\artifacts
mkdir ..\artifacts
jar cfe ../artifacts/server.jar server.Server *
jar cfe ../artifacts/client.jar client.Client *
