#!/bin/bash

set -e
host="$1"
port="$2"
shift 2
cmd="$@"
timeout=30 # Время ожидания в секундах
sleep_interval=1 # Интервал проверки в секундах

echo "Ожидание доступности хоста $host на порту $port..."

counter=0
while ! timeout $timeout bash -c "</dev/tcp/$host/$port" >/dev/null 2>&1; do
    counter=$((counter + sleep_interval))
    if [ $counter -gt $timeout ]; then
        echo "Тайм-аут: хост $host на порту $port не доступен после $timeout секунд."
        exit 1
    fi
    sleep $sleep_interval
done

echo "Хост $host на порту $port доступен."
exec $cmd