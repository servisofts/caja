#!/bin/bash


# ESTO ES SOLO PARA UBUNTU

PORT="10045"
# Buscar el PID del proceso que escucha en el puerto 10031
PID=$(sudo ss -lptn "sport = :$PORT" | grep -oP 'pid=\K\d+')

# Verificar si se encontró un PID
if [ -z "$PID" ]; then
  echo "No se encontró ningún proceso escuchando en el puerto $PORT."
else
  echo "Terminando el proceso con PID $PID que escucha en el puerto $PORT."
  # Terminar el proceso
  sudo kill -9 $PID
fi



