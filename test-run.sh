./bin/bash

export JMS_USER=RootManageSharedAccessKey
export JMS_PORT=5671
export JMS_CONNECTTIMEOUT=60000
export JMS_IDLETIMEOUT=150000
export JMS_HOST=[your-namespace].servicebus.windows.net
export JMS_PASSWORD=[shared access key - url encoded]

# -----------------------------------------------------
# to send messages
# -----------------------------------------------------
# arg[0]=queue-name
# arg[1]="message to send"
# arg[2]=loop-count

mvn exec:java -Dexec.mainClass="Publisher" -Dexec.args="testq02 'Hello from Java JMS app' 10"