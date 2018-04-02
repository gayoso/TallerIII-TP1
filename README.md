## Compilacion
Correr alguno de los scripts para Windows o Linux:
build.cmd
build.sh

## Creacion de logs de entrada
Se puede utilizar el script "genlog.js NUM" donde NUM indica la cantidad de lineas a generar.
El script se encuentra en la carpeta "randexp_github".
Los tests ya tienen generadas sus entradas de prueba.

## Configuracion
Deben existir en la carpeta de ejeución los archivos:
config_general
config_loggers
config_rankings
config_statistics
config_statistics_viewer
Los tests ya tienen sus archivos de configuracion seteados.

## Para correr el Monitor (modificar paths segun corresponda):
java -classpath out\production\tp1 YAAM_test < test_log_5000.txt

## Tests
En el directiorio "tests" hay cuatro subcarpetas con casos de prueba con sus entradas y configuracion.
Para correrlos, utilizar los scripts:
run.cmd
run.sh
Para limpiar el directorio, utilizar los scripts:
clean.cmd
clean.sh
