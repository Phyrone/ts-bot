function startBot() {
  echo "Starting Bot..."
  exec java -jar /opt/bot/bot.jar
}

case $1 in
"start")
  startBot
  ;;

esac
