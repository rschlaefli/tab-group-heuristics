#!/bin/sh

mkdir -p ~/.mozilla/native-messaging-hosts ~/.config/google-chrome/NativeMessagingHosts ~/.config/chromium/NativeMessagingHosts
cp manifest-firefox.json ~/.mozilla/native-messaging-hosts/tabs.json
cp manifest-chrome-stage.json ~/.config/google-chrome/NativeMessagingHosts/tabs.json
cp manifest-chrome-stage.json ~/.config/chromium/NativeMessagingHosts/tabs.json

# ln -sf /home/roland/bin/tabs/bin/*.log ./target/universal/stage/bin/
# ln -sf /home/roland/bin/tabs/bin/*.json ./target/universal/stage/bin/
# ln -sf /home/roland/bin/tabs/bin/*.dot ./target/universal/stage/bin/
