****** Creacion de logs de entrada
Se puede utilizar el script "genlog.js NUM" donde NUM indica la cantidad de lineas a generar.
El script se encuentra en la carpeta "randexp_github".

****** Configuracion
Deben existir en la carpeta de ejeución los archivos:
config_general
config_loggers
config_rankings
config_statistics
config_statistics_viewer

****** Para correr el monitor (modificar paths segun corresponda):
java -classpath out\production\tp1 YAAM_test < test_log_5000.txt

En el directorio tests hay varias pruebas con sus archivos de configuracion y sus entradas prefijadas